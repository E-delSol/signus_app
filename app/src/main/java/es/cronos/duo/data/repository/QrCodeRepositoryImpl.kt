package es.cronos.duo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import es.cronos.duo.domain.repository.QrCodeRepository
import kotlinx.coroutines.tasks.await
import java.util.UUID

class QrCodeRepositoryImpl : QrCodeRepository {
    
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override suspend fun generateUniqueCode(): String {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        
        // 1. Limpieza preventiva
        try {
            val oldSessions = firestore.collection("sessions")
                .whereEqualTo("user1Id", userId)
                .get()
                .await()
                
            for (document in oldSessions) {
                document.reference.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Crear nueva sesión
        val uniqueCode = UUID.randomUUID().toString().substring(0, 8).uppercase()

        val sessionData = hashMapOf(
            "user1Id" to userId,
            "createdAt" to System.currentTimeMillis(),
            "status" to "waiting"
        )

        firestore.collection("sessions").document(uniqueCode).set(sessionData).await()
        
        return uniqueCode
    }

    override suspend fun linkPartner(code: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        val docRef = firestore.collection("sessions").document(code)
        
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val user1Id = snapshot.getString("user1Id")
                
                if (user1Id != null && user1Id != userId) {
                    // 1. Actualizar la sesión
                    docRef.update(
                        mapOf(
                            "user2Id" to userId,
                            "status" to "paired",
                            "linkedAt" to System.currentTimeMillis()
                        )
                    ).await()

                    // 2. Actualizar el perfil del Usuario 1 (Creador) con el ID del Usuario 2 (Invitado)
                    val updateMap1 = mapOf("partnerId" to userId)
                    firestore.collection("users").document(user1Id)
                        .set(updateMap1, SetOptions.merge())
                        .await()

                    // 3. Actualizar el perfil del Usuario 2 (Invitado) con el ID del Usuario 1 (Creador)
                    val updateMap2 = mapOf("partnerId" to user1Id)
                    firestore.collection("users").document(userId)
                        .set(updateMap2, SetOptions.merge())
                        .await()

                    return true
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteSession() {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            // 1. Obtener el usuario actual para ver quién es su pareja
            val userDoc = firestore.collection("users").document(userId).get().await()
            val partnerId = userDoc.getString("partnerId")

            // 2. Eliminar partnerId de ambos usuarios (si existe partner)
            val updates = mapOf<String, Any>("partnerId" to FieldValue.delete())
            
            // Actualizar usuario actual
            firestore.collection("users").document(userId).update(updates).await()

            // Actualizar pareja (si existe)
            if (partnerId != null && partnerId.isNotBlank()) {
                firestore.collection("users").document(partnerId).update(updates).await()
            }

            // 3. Limpiar colección de sesiones (comportamiento original)
            // Borrar donde soy el creador (user1)
            val sessionsAsUser1 = firestore.collection("sessions")
                .whereEqualTo("user1Id", userId)
                .get()
                .await()
                
            for (doc in sessionsAsUser1) {
                doc.reference.delete().await()
            }
            
            // Borrar donde soy el invitado (user2)
            val sessionsAsUser2 = firestore.collection("sessions")
                .whereEqualTo("user2Id", userId)
                .get()
                .await()
                
            for (doc in sessionsAsUser2) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
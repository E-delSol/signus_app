package es.cronos.duo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.cronos.duo.domain.repository.QrCodeRepository
import kotlinx.coroutines.tasks.await
import java.util.UUID

class QrCodeRepositoryImpl : QrCodeRepository {
    
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override suspend fun generateUniqueCode(): String {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        
        // 1. Limpieza preventiva: Borrar cualquier sesión previa creada por este usuario
        // para asegurar que solo tenga una sesión activa.
        try {
            val oldSessions = firestore.collection("sessions")
                .whereEqualTo("user1Id", userId)
                .get()
                .await()
                
            for (document in oldSessions) {
                document.reference.delete().await()
            }
        } catch (e: Exception) {
            // Ignorar errores de limpieza si es la primera vez o hay problemas de red
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
                    docRef.update(
                        mapOf(
                            "user2Id" to userId,
                            "status" to "paired",
                            "linkedAt" to System.currentTimeMillis()
                        )
                    ).await()
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
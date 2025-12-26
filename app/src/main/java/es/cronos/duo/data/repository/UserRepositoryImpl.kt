package es.cronos.duo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl : UserRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Corregido: Usar un getter para obtener siempre el usuario actual, 
    // en lugar de 'by lazy' que podría capturar null si se inicializa muy pronto.
    private val currentUserUid: String?
        get() = auth.currentUser?.uid

    override suspend fun getUser(): User? {
        return currentUserUid?.let { uid ->
            val document = firestore.collection("users").document(uid).get().await()
            document.toObject(User::class.java)
        }
    }

    override fun observeUser(): Flow<User?> = callbackFlow {
        val uid = currentUserUid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun updateUserStatus(status: SemaphoreStatus) {
        currentUserUid?.let {
            val userData = mapOf("status" to status.name)
            firestore.collection("users").document(it).set(userData, SetOptions.merge()).await()
        }
    }

    override fun getPartnerStatus(partnerId: String): Flow<SemaphoreStatus> = callbackFlow {
        val docRef = firestore.collection("users").document(partnerId)

        val subscription = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val partnerStatus = snapshot.getString("status")?.let {
                    try {
                        SemaphoreStatus.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                } ?: SemaphoreStatus.BUSY // Default to BUSY if status is missing or invalid
                trySend(partnerStatus)
            } else {
                trySend(SemaphoreStatus.BUSY) // Partner not found, assume busy
            }
        }

        awaitClose { subscription.remove() }
    }
}
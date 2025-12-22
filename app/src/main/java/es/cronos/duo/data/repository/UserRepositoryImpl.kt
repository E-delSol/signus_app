package es.cronos.duo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl : UserRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val currentUserUid by lazy { FirebaseAuth.getInstance().currentUser?.uid }

    override suspend fun getUser(): User? {
        return currentUserUid?.let { uid ->
            val document = firestore.collection("users").document(uid).get().await()
            document.toObject(User::class.java)
        }
    }

    override suspend fun updateUserStatus(status: SemaphoreStatus) {
        currentUserUid?.let {
            // Save as String to ensure consistency with getString() in getPartnerStatus
            firestore.collection("users").document(it).update("status", status.name).await()
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
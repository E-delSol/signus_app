package es.cronos.duo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val fcm: FirebaseMessaging = FirebaseMessaging.getInstance()
) : UserRepository {

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

    override suspend fun updateUserStatus(status: SemaphoreStatus, expirationTimestamp: Long?, statusDuration: Long?) {
        currentUserUid?.let {
            val updates = mutableMapOf<String, Any>("status" to status.name)
            if (expirationTimestamp != null) {
                updates["statusExpiration"] = expirationTimestamp
            } else {
                updates["statusExpiration"] = com.google.firebase.firestore.FieldValue.delete()
            }
            
            if (statusDuration != null) {
                updates["statusDuration"] = statusDuration
            } else {
                updates["statusDuration"] = com.google.firebase.firestore.FieldValue.delete()
            }

            firestore.collection("users").document(it).set(updates, SetOptions.merge()).await()
        }
    }

    override fun getPartnerStatus(partnerId: String): Flow<User?> = callbackFlow {
        val docRef = firestore.collection("users").document(partnerId)

        val subscription = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                trySend(user)
            } else {
                trySend(null) // Partner not found
            }
        }

        awaitClose { subscription.remove() }
    }

    override suspend fun saveFcmToken(token: String) {
        currentUserUid?.let { uid ->
            val data = mapOf("fcmToken" to token)
            firestore.collection("users").document(uid).set(data, SetOptions.merge()).await()
        }
    }

    override suspend fun syncFcmToken() {
        try {
            val token = fcm.token.await()
            saveFcmToken(token)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
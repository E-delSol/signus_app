package es.cronos.duo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import es.cronos.duo.data.remote.MeApi
import es.cronos.duo.data.remote.PartnerApi
import es.cronos.duo.data.remote.StatusApi
import es.cronos.duo.data.remote.socket.SemaphoreSocket
import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.UserRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val fcm: FirebaseMessaging = FirebaseMessaging.getInstance(),
    private val meApi: MeApi,
    private val partnerApi: PartnerApi,
    private val semaphoreSocket: SemaphoreSocket,
    private val statusApi: StatusApi
) : UserRepository {

    private val currentUserUid: String?
        get() = auth.currentUser?.uid

    override suspend fun getUser(): User? {
        return fetchCurrentUser()
    }

    override fun observeUser(): Flow<User?> = flow {
        // Initial load only. Polling /me is no longer the primary update mechanism.
        emit(fetchCurrentUser())
    }

    override suspend fun updateUserStatus(status: SemaphoreStatus, expirationTimestamp: Long?, statusDuration: Long?) {
        // Backend PATCH /status solo soporta el estado simple por ahora.
        // expirationTimestamp y statusDuration se mantienen en la firma para no romper el flujo actual.
        statusApi.updateStatus(status.name)
    }

    override fun getPartnerStatus(partnerId: String): Flow<User?> = flow {
        emit(fetchPartnerUser())
        try {
            semaphoreSocket.observePartnerStatusChangedEvents().collect { event ->
                val partnerUser = User(
                    id = event.partnerId ?: partnerId,
                    partnerId = null,
                    status = event.status?.let(::toSemaphoreStatus),
                    statusExpiration = event.statusExpiration,
                    statusDuration = event.statusDuration
                )
                Log.d(TAG, "Partner status updated via websocket")
                emit(partnerUser)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Partner websocket unavailable, using polling fallback", e)
            while (currentCoroutineContext().isActive) {
                delay(3000L)
                emit(fetchPartnerUser())
            }
        }
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

    private suspend fun fetchCurrentUser(): User? {
        return meApi.getMe().let { response ->
            User(
                id = response.id,
                email = response.email,
                displayName = response.displayName,
                partnerId = response.partnerId,
                status = response.status?.let(::toSemaphoreStatus),
                statusExpiration = response.statusExpiration,
                statusDuration = response.statusDuration
            )
        }
    }

    private suspend fun fetchPartnerUser(): User? {
        return partnerApi.getPartner().let { response ->
            User(
                id = response.id,
                email = response.email,
                displayName = response.displayName,
                partnerId = response.partnerId,
                status = response.status?.let(::toSemaphoreStatus),
                statusExpiration = response.statusExpiration,
                statusDuration = response.statusDuration
            )
        }
    }

    private fun toSemaphoreStatus(status: String): SemaphoreStatus? {
        return runCatching { SemaphoreStatus.valueOf(status) }.getOrNull()
    }

    companion object {
        private const val TAG = "UserRepositoryImpl"
    }
}

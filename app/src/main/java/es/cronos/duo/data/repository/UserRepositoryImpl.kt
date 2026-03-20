package es.cronos.duo.data.repository

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import es.cronos.duo.BuildConfig
import es.cronos.duo.data.local.TokenStore
import es.cronos.duo.data.remote.DeviceApi
import es.cronos.duo.data.remote.MeApi
import es.cronos.duo.data.remote.PartnerApi
import es.cronos.duo.data.remote.StatusApi
import es.cronos.duo.data.remote.dto.UpsertDeviceTokenRequest
import es.cronos.duo.data.remote.socket.PartnerUnlinkedSocketEvent
import es.cronos.duo.data.remote.socket.SemaphoreSocket
import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.UserRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import java.util.concurrent.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val tokenStore: TokenStore,
    private val fcm: FirebaseMessaging,
    private val deviceApi: DeviceApi,
    private val meApi: MeApi,
    private val partnerApi: PartnerApi,
    private val semaphoreSocket: SemaphoreSocket,
    private val statusApi: StatusApi
) : UserRepository {
    private val currentUser = MutableStateFlow<User?>(null)

    override suspend fun getUser(): User? {
        return fetchCurrentUser().also { user ->
            currentUser.value = user
        }
    }

    override fun observeUser(): Flow<User?> = currentUser.onStart {
        if (currentUser.value == null) {
            getUser()
        }
    }

    override suspend fun unlinkPartner() {
        partnerApi.deletePartner()
        currentUser.update { user ->
            user?.copy(partnerId = null)
        }
    }

    override suspend fun updateUserStatus(status: SemaphoreStatus, expirationTimestamp: Long?, statusDuration: Long?) {
        // Backend PATCH /status solo soporta el estado simple por ahora.
        // expirationTimestamp y statusDuration se mantienen en la firma para no romper el flujo actual.
        statusApi.updateStatus(status.name)
    }

    override fun getPartnerStatus(partnerId: String): Flow<User?> = flow {
        emit(fetchPartnerUser())
        try {
            semaphoreSocket.observePartnerEvents().collect { event ->
                when (event) {
                    is PartnerUnlinkedSocketEvent -> {
                        Log.d(TAG, "Partner unlinked via websocket")
                        currentUser.update { user -> user?.copy(partnerId = null) }
                        emit(null)
                    }
                    is es.cronos.duo.data.remote.socket.SemaphoreStatusChangedSocketEvent -> {
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
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Partner websocket unavailable, using polling fallback", e)
            while (currentCoroutineContext().isActive) {
                delay(3000L)
                emit(fetchPartnerUser())
            }
        }
    }

    override suspend fun registerOrUpdateDeviceToken(fcmToken: String) {
        if (fcmToken.isBlank()) return
        if (!isAuthenticated()) return

        val deviceId = tokenStore.getOrCreateDeviceId()
        val appVersion = BuildConfig.VERSION_NAME
        val request = UpsertDeviceTokenRequest(
            deviceId = deviceId,
            fcmToken = fcmToken,
            platform = DEVICE_PLATFORM_ANDROID,
            appVersion = appVersion
        )

        runCatching {
            deviceApi.registerOrUpdateDeviceToken(request)
        }.onFailure { error ->
            Log.w(
                TAG,
                "Failed to register/update FCM token. deviceIdPresent=${deviceId.isNotBlank()}, tokenLength=${fcmToken.length}, platform=${request.platform}, appVersion=$appVersion",
                error
            )
        }
    }

    override suspend fun syncFcmToken() {
        if (!isAuthenticated()) return
        runCatching {
            fcm.token.await()
        }.onSuccess { token ->
            if (!token.isNullOrBlank()) {
                registerOrUpdateDeviceToken(token)
            }
        }.onFailure { error ->
            Log.w(TAG, "Failed to fetch FCM token", error)
        }
    }

    override suspend fun deactivateDeviceToken(deviceId: String) {
        if (!isAuthenticated()) return

        runCatching {
            deviceApi.deactivateDeviceToken(deviceId)
        }.onFailure { error ->
            Log.w(TAG, "Failed to deactivate FCM token", error)
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
        return try {
            partnerApi.getPartner().let { response ->
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
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                null
            } else {
                throw e
            }
        }
    }

    private fun toSemaphoreStatus(status: String): SemaphoreStatus? {
        return runCatching { SemaphoreStatus.valueOf(status) }.getOrNull()
    }

    companion object {
        private const val TAG = "UserRepositoryImpl"
        private const val DEVICE_PLATFORM_ANDROID = "android"
    }

    private fun isAuthenticated(): Boolean {
        return !tokenStore.getToken().isNullOrBlank()
    }
}

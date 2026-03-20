package es.cronos.duo.data.repository

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import es.cronos.duo.data.local.TokenStore
import es.cronos.duo.data.remote.DeviceApi
import es.cronos.duo.data.remote.MeApi
import es.cronos.duo.data.remote.PartnerApi
import es.cronos.duo.data.remote.StatusApi
import es.cronos.duo.data.remote.dto.MeResponseDto
import es.cronos.duo.data.remote.dto.PartnerResponseDto
import es.cronos.duo.data.remote.socket.PartnerUnlinkedSocketEvent
import es.cronos.duo.data.remote.socket.SemaphoreSocket
import es.cronos.duo.domain.model.SemaphoreStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class UserRepositoryImplTest {

    private val tokenStore: TokenStore = mockk(relaxed = true)
    private val fcm: FirebaseMessaging = mockk()
    private val deviceApi: DeviceApi = mockk(relaxed = true)
    private val meApi: MeApi = mockk()
    private val partnerApi: PartnerApi = mockk()
    private val semaphoreSocket: SemaphoreSocket = mockk()
    private val statusApi: StatusApi = mockk()
    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0
        userRepository = UserRepositoryImpl(tokenStore, fcm, deviceApi, meApi, partnerApi, semaphoreSocket, statusApi)
    }

    @Test
    fun `given backend me response when getUser is called then maps to domain user`() = runTest {
        coEvery { meApi.getMe() } returns MeResponseDto(
            id = "user123",
            email = "test@example.com",
            displayName = "Test User",
            partnerId = "partner123",
            status = "AVAILABLE",
            statusExpiration = 12345L,
            statusDuration = 60000L
        )

        val result = userRepository.getUser()

        result?.id shouldBeEqualTo "user123"
        result?.email shouldBeEqualTo "test@example.com"
        result?.displayName shouldBeEqualTo "Test User"
        result?.partnerId shouldBeEqualTo "partner123"
        result?.status shouldBeEqualTo SemaphoreStatus.AVAILABLE
        result?.statusExpiration shouldBeEqualTo 12345L
        result?.statusDuration shouldBeEqualTo 60000L
    }

    @Test
    fun `when backend me fails then getUser propagates the error`() = runTest {
        coEvery { meApi.getMe() } throws Exception("Backend error")

        try {
            userRepository.getUser()
        } catch (e: Exception) {
            e.message shouldBeEqualTo "Backend error"
        }
    }

    @Test
    fun `given backend partner response when getPartnerStatus is collected then maps to domain user`() = runTest {
        coEvery { partnerApi.getPartner() } returns PartnerResponseDto(
            id = "partner123",
            status = "BUSY",
            statusExpiration = 98765L
        )

        val result = userRepository.getPartnerStatus("ignored-partner-id").first()

        result?.id shouldBeEqualTo "partner123"
        result?.status shouldBeEqualTo SemaphoreStatus.BUSY
        result?.statusExpiration shouldBeEqualTo 98765L
    }

    @Test
    fun `given cached user when unlinkPartner is called then backend is invoked and partnerId is cleared`() = runTest {
        coEvery { meApi.getMe() } returns MeResponseDto(
            id = "user123",
            partnerId = "partner123"
        )
        coEvery { partnerApi.deletePartner() } returns Unit

        userRepository.getUser()
        userRepository.unlinkPartner()

        coVerify(exactly = 1) { partnerApi.deletePartner() }
        userRepository.observeUser().first()?.partnerId shouldBeEqualTo null
    }

    @Test
    fun `given websocket partner unlinked event when getPartnerStatus is collected then clear cached partner`() = runTest {
        coEvery { meApi.getMe() } returns MeResponseDto(
            id = "user123",
            partnerId = "partner123"
        )
        coEvery { partnerApi.getPartner() } returns PartnerResponseDto(
            id = "partner123",
            status = "AVAILABLE"
        )
        every { semaphoreSocket.observePartnerEvents() } returns flowOf(PartnerUnlinkedSocketEvent("partner123"))

        userRepository.getUser()
        val result = userRepository.getPartnerStatus("partner123").toList()

        result[0]?.id shouldBeEqualTo "partner123"
        result[1] shouldBeEqualTo null
        userRepository.observeUser().first()?.partnerId shouldBeEqualTo null
    }

    @Test
    fun `when registerOrUpdateDeviceToken is called then request includes persistent deviceId platform and appVersion`() = runTest {
        every { tokenStore.getToken() } returns "access-token"
        every { tokenStore.getOrCreateDeviceId() } returns "device-123"
        coEvery { deviceApi.registerOrUpdateDeviceToken(any()) } returns Unit

        userRepository.registerOrUpdateDeviceToken("fcm-token-123")

        coVerify(exactly = 1) {
            deviceApi.registerOrUpdateDeviceToken(
                match { request ->
                    request.deviceId == "device-123" &&
                        request.fcmToken == "fcm-token-123" &&
                        request.platform == "android" &&
                        request.appVersion.isNotBlank()
                }
            )
        }
    }
}

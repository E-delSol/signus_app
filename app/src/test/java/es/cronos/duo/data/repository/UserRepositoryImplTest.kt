package es.cronos.duo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import es.cronos.duo.data.remote.MeApi
import es.cronos.duo.data.remote.PartnerApi
import es.cronos.duo.data.remote.StatusApi
import es.cronos.duo.data.remote.dto.MeResponseDto
import es.cronos.duo.data.remote.dto.PartnerResponseDto
import es.cronos.duo.domain.model.SemaphoreStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class UserRepositoryImplTest {

    private val firestore: FirebaseFirestore = mockk()
    private val auth: FirebaseAuth = mockk()
    private val fcm: FirebaseMessaging = mockk()
    private val meApi: MeApi = mockk()
    private val partnerApi: PartnerApi = mockk()
    private val statusApi: StatusApi = mockk()
    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setup() {
        userRepository = UserRepositoryImpl(firestore, auth, fcm, meApi, partnerApi, statusApi)
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
}

package es.cronos.duo.data.repository

import android.util.Log
import es.cronos.duo.data.local.TokenStore
import es.cronos.duo.data.network.NetworkHttpClientProvider
import es.cronos.duo.data.remote.AuthApi
import es.cronos.duo.data.remote.dto.AuthResponseDto
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.util.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private val authApi: AuthApi = mockk()
    private val tokenStore: TokenStore = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val networkHttpClientProvider: NetworkHttpClientProvider = mockk(relaxed = true)

    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any(), any(), any()) } returns 0
        authRepository = AuthRepositoryImpl(
            authApi = authApi,
            tokenStore = tokenStore,
            userRepository = userRepository,
            networkHttpClientProvider = networkHttpClientProvider
        )
    }

    @Test
    fun `when backend login succeeds then save token sync fcm and return success`() = runTest {
        val email = "test@example.com"
        val password = "password"
        val accessToken = "access-token"
        val refreshToken = "refresh-token"
        coEvery { authApi.login(email, password) } returns AuthResponseDto(
            accessToken = accessToken,
            refreshToken = refreshToken
        )

        val result = authRepository.login(email, password)

        result shouldBeInstanceOf Resource.Success::class
        result.data?.id shouldBeEqualTo "backend_user"
        result.data?.email shouldBeEqualTo email
        verify(exactly = 1) { tokenStore.saveToken(accessToken) }
        verify(exactly = 1) { tokenStore.saveRefreshToken(refreshToken) }
        verify(exactly = 1) { networkHttpClientProvider.clearBearerTokenCache() }
        coVerify(exactly = 1) { userRepository.syncFcmToken() }
    }

    @Test
    fun `when backend register succeeds then save token sync fcm and return success`() = runTest {
        val email = "new@example.com"
        val password = "password"
        val displayName = "New User"
        val accessToken = "new-access-token"
        val refreshToken = "new-refresh-token"
        coEvery { authApi.register(email, password, displayName) } returns AuthResponseDto(
            accessToken = accessToken,
            refreshToken = refreshToken
        )

        val result = authRepository.register(email, password, displayName)

        result shouldBeInstanceOf Resource.Success::class
        result.data?.id shouldBeEqualTo "backend_user"
        result.data?.email shouldBeEqualTo email
        result.data?.displayName shouldBeEqualTo displayName
        verify(exactly = 1) { tokenStore.saveToken(accessToken) }
        verify(exactly = 1) { tokenStore.saveRefreshToken(refreshToken) }
        verify(exactly = 1) { networkHttpClientProvider.clearBearerTokenCache() }
        coVerify(exactly = 1) { userRepository.syncFcmToken() }
    }

    @Test
    fun `when backend login has network error then return network error`() = runTest {
        val email = "test@example.com"
        val password = "password"
        coEvery { authApi.login(email, password) } throws IOException("offline")

        val result = authRepository.login(email, password)

        result shouldBeInstanceOf Resource.Error::class
        result.message shouldBeEqualTo "Error de red"
        verify(exactly = 0) { tokenStore.saveToken(any()) }
        coVerify(exactly = 0) { userRepository.syncFcmToken() }
    }

    @Test
    fun `when backend login throws unexpected exception then return friendly error`() = runTest {
        val email = "test@example.com"
        val password = "password"
        coEvery { authApi.login(email, password) } throws IllegalStateException("Illegal input: Field 'access token'")

        val result = authRepository.login(email, password)

        result shouldBeInstanceOf Resource.Error::class
        result.message shouldBeEqualTo "No se pudo iniciar sesión"
        verify(exactly = 0) { tokenStore.saveToken(any()) }
        coVerify(exactly = 0) { userRepository.syncFcmToken() }
    }

    @Test
    fun `when logout with token then clear token deactivate device and sign out firebase`() = runTest {
        every { tokenStore.getToken() } returns "existing-token"
        every { tokenStore.getOrCreateDeviceId() } returns "device-123"

        authRepository.logout()

        coVerify(exactly = 1) { userRepository.deactivateDeviceToken("device-123") }
        verify(exactly = 1) { tokenStore.clearToken() }
        verify(exactly = 1) { tokenStore.clearRefreshToken() }
        verify(exactly = 1) { networkHttpClientProvider.clearBearerTokenCache() }
    }
}

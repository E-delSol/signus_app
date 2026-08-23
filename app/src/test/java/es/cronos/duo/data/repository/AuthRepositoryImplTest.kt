package es.cronos.duo.data.repository

import android.util.Log
import es.cronos.duo.data.local.TokenStore
import es.cronos.duo.data.network.NetworkHttpClientProvider
import es.cronos.duo.data.remote.AuthApi
import es.cronos.duo.data.remote.dto.AuthResponseDto
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.util.Resource
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
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
        mockkStatic("io.ktor.client.statement.HttpResponseKt")
        every { Log.d(any(), any()) } returns 0
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

    @Test
    fun `when refresh session succeeds then replace access and refresh tokens and clear bearer cache`() = runTest {
        every { tokenStore.getRefreshToken() } returns "stored-refresh-token"
        every { tokenStore.getToken() } returns "stored-access-token"
        coEvery { authApi.refreshSession("stored-refresh-token") } returns AuthResponseDto(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token"
        )

        val result = authRepository.refreshSession()

        result shouldBeEqualTo true
        verify(exactly = 1) { tokenStore.saveToken("new-access-token") }
        verify(exactly = 1) { tokenStore.saveRefreshToken("new-refresh-token") }
        verify(exactly = 1) { networkHttpClientProvider.clearBearerTokenCache() }
    }

    @Test
    fun `when refresh session has client request exception then return false and keep stored tokens unchanged`() = runTest {
        every { tokenStore.getRefreshToken() } returns "stored-refresh-token"
        coEvery { authApi.refreshSession("stored-refresh-token") } throws clientRequestException(
            status = HttpStatusCode.Unauthorized
        )

        val result = authRepository.refreshSession()

        result shouldBeEqualTo false
        verify(exactly = 0) { tokenStore.saveToken(any()) }
        verify(exactly = 0) { tokenStore.saveRefreshToken(any()) }
        verify(exactly = 0) { networkHttpClientProvider.clearBearerTokenCache() }
    }

    @Test
    fun `when refresh session has io exception then return false and keep stored tokens unchanged`() = runTest {
        every { tokenStore.getRefreshToken() } returns "stored-refresh-token"
        coEvery { authApi.refreshSession("stored-refresh-token") } throws IOException("offline")

        val result = authRepository.refreshSession()

        result shouldBeEqualTo false
        verify(exactly = 0) { tokenStore.saveToken(any()) }
        verify(exactly = 0) { tokenStore.saveRefreshToken(any()) }
        verify(exactly = 0) { networkHttpClientProvider.clearBearerTokenCache() }
    }

    @Test
    fun `when login returns unauthorized with backend error then propagate backend message`() = runTest {
        val email = "test@example.com"
        val password = "password"
        coEvery { authApi.login(email, password) } throws clientRequestException(
            status = HttpStatusCode.Unauthorized,
            body = """{"error":"Credenciales del backend"}"""
        )

        val result = authRepository.login(email, password)

        result shouldBeInstanceOf Resource.Error::class
        result.message shouldBeEqualTo "Credenciales del backend"
    }

    @Test
    fun `when login returns unauthorized without backend error then use default invalid credentials message`() = runTest {
        val email = "test@example.com"
        val password = "password"
        coEvery { authApi.login(email, password) } throws clientRequestException(
            status = HttpStatusCode.Unauthorized
        )

        val result = authRepository.login(email, password)

        result shouldBeInstanceOf Resource.Error::class
        result.message shouldBeEqualTo "Credenciales inválidas"
    }

    @Test
    fun `when login returns bad request without backend error then use default invalid data message`() = runTest {
        val email = "test@example.com"
        val password = "password"
        coEvery { authApi.login(email, password) } throws clientRequestException(
            status = HttpStatusCode.BadRequest
        )

        val result = authRepository.login(email, password)

        result shouldBeInstanceOf Resource.Error::class
        result.message shouldBeEqualTo "Datos inválidos"
    }

    @Test
    fun `when register returns conflict with backend error then propagate backend message`() = runTest {
        val email = "new@example.com"
        val password = "password"
        val displayName = "New User"
        coEvery { authApi.register(email, password, displayName) } throws clientRequestException(
            status = HttpStatusCode.Conflict,
            body = """{"error":"Email ya registrado"}"""
        )

        val result = authRepository.register(email, password, displayName)

        result shouldBeInstanceOf Resource.Error::class
        result.message shouldBeEqualTo "Email ya registrado"
    }

    @Test
    fun `when register returns conflict without backend error then use default conflict message`() = runTest {
        val email = "new@example.com"
        val password = "password"
        val displayName = "New User"
        coEvery { authApi.register(email, password, displayName) } throws clientRequestException(
            status = HttpStatusCode.Conflict
        )

        val result = authRepository.register(email, password, displayName)

        result shouldBeInstanceOf Resource.Error::class
        result.message shouldBeEqualTo "Conflicto al registrarse"
    }

    @Test
    fun `startupCheck when logged in and refresh succeeds returns Success`() = runTest {
        every { tokenStore.getToken() } returns "valid-token"
        every { tokenStore.getRefreshToken() } returns "valid-refresh"
        coEvery { authApi.refreshSession("valid-refresh") } returns AuthResponseDto(
            accessToken = "new-token",
            refreshToken = "new-refresh"
        )

        val result = authRepository.startupCheck()

        result shouldBeInstanceOf Resource.Success::class
        coVerify(exactly = 1) { authApi.refreshSession("valid-refresh") }
        coVerify(exactly = 0) { authApi.bootstrap() }
    }

    @Test
    fun `startupCheck when logged in but refresh fails throws exception falls through to bootstrap`() = runTest {
        every { tokenStore.getToken() } returns "valid-token"
        every { tokenStore.getRefreshToken() } returns "valid-refresh"
        coEvery { authApi.refreshSession("valid-refresh") } throws clientRequestException(
            status = HttpStatusCode.Unauthorized
        )
        coEvery { authApi.bootstrap() } returns "ok"

        val result = authRepository.startupCheck()

        result shouldBeInstanceOf Resource.Success::class
        coVerify(exactly = 1) { authApi.refreshSession("valid-refresh") }
        coVerify(exactly = 1) { authApi.bootstrap() }
    }

    @Test
    fun `startupCheck when not logged in calls bootstrap and returns Success`() = runTest {
        every { tokenStore.getToken() } returns null
        coEvery { authApi.bootstrap() } returns "ok"

        val result = authRepository.startupCheck()

        result shouldBeInstanceOf Resource.Success::class
        coVerify(exactly = 1) { authApi.bootstrap() }
        coVerify(exactly = 0) { authApi.refreshSession(any()) }
    }

    @Test
    fun `startupCheck when not logged in and bootstrap throws client exception returns Error`() = runTest {
        every { tokenStore.getToken() } returns null
        coEvery { authApi.bootstrap() } throws clientRequestException(
            status = HttpStatusCode.UpgradeRequired
        )

        val result = authRepository.startupCheck()

        result shouldBeInstanceOf Resource.Error::class
        result.message shouldBeEqualTo "Startup check failed"
        coVerify(exactly = 1) { authApi.bootstrap() }
    }

    @Test
    fun `startupCheck when bootstrap throws IOException returns network error`() = runTest {
        every { tokenStore.getToken() } returns null
        coEvery { authApi.bootstrap() } throws IOException("offline")

        val result = authRepository.startupCheck()

        result shouldBeInstanceOf Resource.Error::class
        result.message shouldBeEqualTo "Error de red"
    }

    @Test
    fun `startupCheck when bootstrap throws unexpected exception returns friendly error`() = runTest {
        every { tokenStore.getToken() } returns null
        coEvery { authApi.bootstrap() } throws IllegalStateException("unexpected")

        val result = authRepository.startupCheck()

        result shouldBeInstanceOf Resource.Error::class
        result.message shouldBeEqualTo "Error inesperado en startup"
    }

    @Test
    fun `startupCheck when logged in but no refresh token falls through to bootstrap`() = runTest {
        every { tokenStore.getToken() } returns "valid-token"
        every { tokenStore.getRefreshToken() } returns null
        coEvery { authApi.bootstrap() } returns "ok"

        val result = authRepository.startupCheck()

        result shouldBeInstanceOf Resource.Success::class
        coVerify(exactly = 0) { authApi.refreshSession(any()) }
        coVerify(exactly = 1) { authApi.bootstrap() }
    }

    private fun clientRequestException(
        status: HttpStatusCode,
        body: String = ""
    ): ClientRequestException {
        val response = mockk<HttpResponse>(relaxed = true)
        every { response.status } returns status
        every { response.call } returns mockk<HttpClientCall>(relaxed = true)
        coEvery { response.bodyAsText() } returns body
        return ClientRequestException(response, body)
    }
}

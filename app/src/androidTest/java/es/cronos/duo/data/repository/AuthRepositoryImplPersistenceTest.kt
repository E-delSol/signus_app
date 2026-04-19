package es.cronos.duo.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.cronos.duo.data.local.TokenStore
import es.cronos.duo.data.network.NetworkHttpClientProvider
import es.cronos.duo.data.remote.AuthApi
import es.cronos.duo.data.remote.dto.AuthResponseDto
import es.cronos.duo.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthRepositoryImplPersistenceTest {

    private lateinit var context: Context
    private lateinit var tokenStore: TokenStore

    private val authApi: AuthApi = mockk()
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val networkHttpClientProvider: NetworkHttpClientProvider = mockk(relaxed = true)

    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearStore()
        tokenStore = TokenStore(context)
        authRepository = AuthRepositoryImpl(
            authApi = authApi,
            tokenStore = tokenStore,
            userRepository = userRepository,
            networkHttpClientProvider = networkHttpClientProvider
        )
    }

    @After
    fun tearDown() {
        clearStore()
    }

    @Test
    fun `when login succeeds then access and refresh tokens stay persisted for a new store instance`() = runBlocking {
        coEvery { authApi.login("test@example.com", "password") } returns AuthResponseDto(
            accessToken = "login-access-token",
            refreshToken = "login-refresh-token"
        )

        authRepository.login("test@example.com", "password")

        val reloadedStore = TokenStore(context)
        assertEquals("login-access-token", reloadedStore.getToken())
        assertEquals("login-refresh-token", reloadedStore.getRefreshToken())
    }

    @Test
    fun `when refresh session succeeds then new tokens replace the previous persisted values`() = runBlocking {
        tokenStore.saveToken("old-access-token")
        tokenStore.saveRefreshToken("old-refresh-token")
        coEvery { authApi.refreshSession("old-refresh-token") } returns AuthResponseDto(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token"
        )

        authRepository.refreshSession()

        val reloadedStore = TokenStore(context)
        assertEquals("new-access-token", reloadedStore.getToken())
        assertEquals("new-refresh-token", reloadedStore.getRefreshToken())
    }

    @Test
    fun `when logout is executed then persisted access and refresh tokens are cleared`() = runBlocking {
        tokenStore.saveToken("access-token")
        tokenStore.saveRefreshToken("refresh-token")

        authRepository.logout()

        val reloadedStore = TokenStore(context)
        assertNull(reloadedStore.getToken())
        assertNull(reloadedStore.getRefreshToken())
    }

    private fun clearStore() {
        context.deleteSharedPreferences(SECURE_PREFS_FILE)
    }

    private companion object {
        const val SECURE_PREFS_FILE = "auth_secure_prefs"
    }
}

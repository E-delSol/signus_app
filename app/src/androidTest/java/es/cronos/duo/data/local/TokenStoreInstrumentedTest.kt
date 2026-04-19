package es.cronos.duo.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenStoreInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearStore()
    }

    @After
    fun tearDown() {
        clearStore()
    }

    @Test
    fun `when access token is saved then it is available from a new store instance`() {
        val firstStore = TokenStore(context)

        firstStore.saveToken("access-token-123")

        val reloadedStore = TokenStore(context)

        assertEquals("access-token-123", reloadedStore.getToken())
    }

    @Test
    fun `when refresh token is saved then it is available from a new store instance`() {
        val firstStore = TokenStore(context)

        firstStore.saveRefreshToken("refresh-token-123")

        val reloadedStore = TokenStore(context)

        assertEquals("refresh-token-123", reloadedStore.getRefreshToken())
    }

    @Test
    fun `when device id is created then it remains stable across store instances`() {
        val firstStore = TokenStore(context)

        val firstDeviceId = firstStore.getOrCreateDeviceId()

        val reloadedStore = TokenStore(context)
        val reloadedDeviceId = reloadedStore.getOrCreateDeviceId()

        assertNotNull(firstDeviceId)
        assertEquals(firstDeviceId, reloadedDeviceId)
    }

    private fun clearStore() {
        context.deleteSharedPreferences(SECURE_PREFS_FILE)
    }

    private companion object {
        const val SECURE_PREFS_FILE = "auth_secure_prefs"
    }
}

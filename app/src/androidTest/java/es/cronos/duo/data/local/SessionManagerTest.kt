package es.cronos.duo.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionManagerTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Usamos un nombre de archivo de prueba para no interferir con la app
        sharedPreferences = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
    }

    @After
    fun teardown() {
        // Limpiamos después de cada test
        sharedPreferences.edit().clear().commit()
    }

    @Test
    fun givenUserToken_whenSaveToken_thenTokenIsPersisted() {
        // Given
        val token = "sample_fcm_token_123"
        
        // When
        sharedPreferences.edit().putString("fcm_token", token).commit()

        // Then
        val savedToken = sharedPreferences.getString("fcm_token", null)
        assertThat(savedToken, `is`(token))
    }

    @Test
    fun givenSavedData_whenClear_thenDataIsRemoved() {
        // Given
        sharedPreferences.edit().putString("user_id", "123").commit()

        // When
        sharedPreferences.edit().remove("user_id").commit()

        // Then
        val userId = sharedPreferences.getString("user_id", null)
        assertThat(userId, nullValue())
    }
}

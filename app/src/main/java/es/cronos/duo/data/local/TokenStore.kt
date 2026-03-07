package es.cronos.duo.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(
    context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCES_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return preferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun clearToken() {
        preferences.edit().remove(KEY_ACCESS_TOKEN).apply()
    }

    companion object {
        private const val PREFERENCES_FILE = "auth_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "key_access_token"
    }
}

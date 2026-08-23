package es.cronos.duo.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * ⚠️ IMPORTANT: Deprecated API usage (intentional)
 *
 * This class uses EncryptedSharedPreferences and MasterKey, which are deprecated
 * in androidx.security:security-crypto 1.1.0.
 *
 * Official documentation:
 * - EncryptedSharedPreferences is deprecated and suggests using SharedPreferences instead.
 * - However, there is currently NO drop-in replacement that provides encrypted
 *   key-value storage with the same guarantees.
 *
 * Why we keep this implementation:
 * - Stores sensitive data: accessToken, refreshToken, deviceId
 * - This storage is part of the authentication core flow (login, refresh, logout)
 * - Replacing it requires a controlled migration to avoid:
 *     - session loss
 *     - broken refresh token flow
 *     - deviceId inconsistency
 *
 * ⚠️ DO NOT replace or refactor this lightly.
 *
 * Any migration must:
 * - Be staged (dual read / gradual write)
 * - Preserve existing persisted data
 * - Be validated against auth flows and app updates
 *
 * Security note:
 * - This storage is excluded from Auto Backup and device transfer,
 *   as required by Android documentation, to avoid restoring encrypted
 *   data without the corresponding keystore key.
 *
 * See:
 * - ARCHITECTURE.md (auth & persistence)
 * - rules.md (no architectural violations)
 *
 * Refactor strategy must be defined BEFORE any implementation change.
 */
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

    fun saveRefreshToken(token: String) {
        preferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? {
        return preferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun clearRefreshToken() {
        preferences.edit().remove(KEY_REFRESH_TOKEN).apply()
    }

    fun getOrCreateDeviceId(): String {
        val existing = preferences.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val generated = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    companion object {
        private const val PREFERENCES_FILE = "auth_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "key_access_token"
        private const val KEY_REFRESH_TOKEN = "key_refresh_token"
        private const val KEY_DEVICE_ID = "key_device_id"
    }
}

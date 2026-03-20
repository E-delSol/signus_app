package es.cronos.duo.presentation.login

import android.util.Log
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import es.cronos.duo.R

class GoogleAuthUiClient(
    private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): SignInResult {
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            handleSignIn(result)
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Google credential request failed", e)
            SignInResult(idToken = null, errorMessage = "No se pudo iniciar sesión con Google")
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected Google sign-in error", e)
            SignInResult(idToken = null, errorMessage = "No se pudo iniciar sesión con Google")
        }
    }

    private fun handleSignIn(result: GetCredentialResponse): SignInResult {
        val credential = result.credential
        
        return if (credential is GoogleIdTokenCredential) {
            SignInResult(idToken = credential.idToken, errorMessage = null)
        } else {
            Log.w(TAG, "Unexpected credential type: ${credential::class.java.simpleName}")
            SignInResult(idToken = null, errorMessage = "No se pudo iniciar sesión con Google")
        }
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Google credential state cleanup failed", e)
        }
    }

    companion object {
        private const val TAG = "GoogleAuthUiClient"
    }

    data class SignInResult(
        val idToken: String?,
        val errorMessage: String?
    )
}

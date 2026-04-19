package es.cronos.duo.data.repository

import android.util.Log
import es.cronos.duo.data.local.TokenStore
import es.cronos.duo.data.network.NetworkHttpClientProvider
import es.cronos.duo.data.remote.AuthApi
import es.cronos.duo.data.remote.dto.ErrorResponseDto
import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.util.Resource
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import java.io.IOException

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
    private val userRepository: UserRepository,
    private val networkHttpClientProvider: NetworkHttpClientProvider
) : AuthRepository {

    override val currentUser: User?
        get() = if (isLoggedIn()) {
            User(id = "backend_user")
        } else {
            null
        }

    override suspend fun login(email: String, password: String): Resource<User> {
        try {
            val response = authApi.login(email = email, password = password)
            storeSessionTokens(response.accessToken, response.refreshToken)
            userRepository.syncFcmToken()
            return Resource.Success(
                User(
                    id = "backend_user",
                    email = email
                )
            )
        } catch (e: ClientRequestException) {
            return Resource.Error(mapLoginError(e.response.status, parseErrorMessage(e)))
        } catch (e: ServerResponseException) {
            return Resource.Error("Error inesperado del servidor (${e.response.status.value})")
        } catch (_: IOException) {
            return Resource.Error("Error de red")
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected login error", e)
            return Resource.Error("No se pudo iniciar sesión")
        }
    }

    override suspend fun register(email: String, password: String, displayName: String): Resource<User> {
        try {
            val response = authApi.register(
                email = email,
                password = password,
                displayName = displayName
            )
            storeSessionTokens(response.accessToken, response.refreshToken)
            userRepository.syncFcmToken()
            return Resource.Success(
                User(
                    id = "backend_user",
                    email = email,
                    displayName = displayName
                )
            )
        } catch (e: ClientRequestException) {
            return Resource.Error(mapRegisterError(e.response.status, parseErrorMessage(e)))
        } catch (e: ServerResponseException) {
            return Resource.Error("Error inesperado del servidor (${e.response.status.value})")
        } catch (_: IOException) {
            return Resource.Error("Error de red")
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected register error", e)
            return Resource.Error("No se pudo completar el registro")
        }
    }

    override fun isLoggedIn(): Boolean {
        return !tokenStore.getToken().isNullOrBlank()
    }

    override suspend fun logout() {
        runCatching {
            if (isLoggedIn()) {
                val deviceId = tokenStore.getOrCreateDeviceId()
                userRepository.deactivateDeviceToken(deviceId)
            }
        }
        clearSessionTokens()
    }

    override suspend fun refreshSession(): Boolean {
        val refreshToken = tokenStore.getRefreshToken() ?: return false
        return try {
            val response = authApi.refreshSession(refreshToken)

            Log.d("AuthRefresh", "Old access token: ${tokenStore.getToken()}")
            Log.d("AuthRefresh", "New access token: ${response.accessToken}")
            Log.d("AuthRefresh", "Old refresh token: ${tokenStore.getRefreshToken()}")
            Log.d("AuthRefresh", "New refresh token: ${response.refreshToken}")

            storeSessionTokens(response.accessToken, response.refreshToken)
            true
        } catch (_: ClientRequestException) {
            false
        } catch (_: ServerResponseException) {
            false
        } catch (_: IOException) {
            false
        }
    }

    override suspend fun testProtectedEndpoint(): Resource<String> {
        return try {
            Resource.Success(authApi.testProtected())
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                Resource.Error("Token inválido o expirado")
            } else {
                Resource.Error("Error inesperado en endpoint protegido")
            }
        } catch (_: IOException) {
            Resource.Error("Error de red")
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected protected endpoint error", e)
            Resource.Error("No se pudo completar la operación")
        }
    }

    private suspend fun parseErrorMessage(e: ClientRequestException): String? {
        val body = e.response.bodyAsText().trim()
        if (body.isEmpty()) return null
        return runCatching {
            json.decodeFromString(ErrorResponseDto.serializer(), body).error
        }.getOrNull()
    }

    private fun mapLoginError(status: HttpStatusCode, backendMessage: String?): String {
        return when (status) {
            HttpStatusCode.Unauthorized -> backendMessage ?: "Credenciales inválidas"
            HttpStatusCode.BadRequest -> backendMessage ?: "Datos inválidos"
            HttpStatusCode.Conflict -> backendMessage ?: "Conflicto al iniciar sesión"
            else -> "Error inesperado al iniciar sesión (${status.value})"
        }
    }

    private fun mapRegisterError(status: HttpStatusCode, backendMessage: String?): String {
        return when (status) {
            HttpStatusCode.BadRequest -> backendMessage ?: "Datos inválidos"
            HttpStatusCode.Conflict -> backendMessage ?: "Conflicto al registrarse"
            HttpStatusCode.Unauthorized -> backendMessage ?: "No autorizado"
            else -> "Error inesperado al registrarse (${status.value})"
        }
    }

    private fun storeSessionTokens(accessToken: String, refreshToken: String) {
        tokenStore.saveToken(accessToken)
        tokenStore.saveRefreshToken(refreshToken)
        networkHttpClientProvider.clearBearerTokenCache()
    }

    private fun clearSessionTokens() {
        tokenStore.clearToken()
        tokenStore.clearRefreshToken()
        networkHttpClientProvider.clearBearerTokenCache()
    }

    companion object {
        private const val TAG = "AuthRepositoryImpl"
        private val json = Json { ignoreUnknownKeys = true }
    }
}

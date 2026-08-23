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
            tokenStore.getToken()?.let {
                User(id = "backend_user")
            }
        } else null

    override suspend fun login(email: String, password: String): Resource<User> {
        return safeAuthCall(
            errorMapper = { status, msg -> mapLoginError(status, msg) }
        ) {
            val response = authApi.login(email, password)

            val access = response.accessToken
            val refresh = response.refreshToken

            if (access.isBlank()) {
                throw IllegalStateException("Missing accessToken from login response")
            }

            persistSession(access, refresh)

            userRepository.syncFcmToken()

            Resource.Success(
                User(
                    id = "backend_user",
                    email = email
                )
            )
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Resource<User> {
        return safeAuthCall(
            errorMapper = { status, msg -> mapRegisterError(status, msg) }
        ) {
            val response = authApi.register(email, password, displayName)

            val access = response.accessToken
            val refresh = response.refreshToken

            if (access.isBlank()) {
                throw IllegalStateException("Missing accessToken from register response")
            }

            persistSession(access, refresh)

            userRepository.syncFcmToken()

            Resource.Success(
                User(
                    id = "backend_user",
                    email = email,
                    displayName = displayName
                )
            )
        }
    }

    override fun isLoggedIn(): Boolean {
        return !tokenStore.getToken().isNullOrBlank()
    }

    override suspend fun logout() {
        runCatching {
            val deviceId = tokenStore.getOrCreateDeviceId()
            userRepository.deactivateDeviceToken(deviceId)
        }

        clearSession()
    }

    override suspend fun refreshSession(): Boolean {
        val refreshToken = tokenStore.getRefreshToken() ?: return false

        return try {
            val response = authApi.refreshSession(refreshToken)

            val newAccess = response.accessToken
            val newRefresh = response.refreshToken

            if (newAccess.isBlank()) {
                clearSession()
                return false
            }

            persistSession(newAccess, newRefresh) // puede ser null válido
            true

        } catch (e: Exception) {
            clearSession()
            false
        }
    }

    override suspend fun startupCheck(): Resource<Unit> {
        return try {

            val refreshed = if (isLoggedIn()) {
                refreshSession()
            } else {
                false
            }

            val bootstrap = runCatching {
                authApi.bootstrap()
            }.isSuccess

            return if (isLoggedIn() && (refreshed || bootstrap)) {
                Resource.Success(Unit)
            } else {
                clearSession()
                Resource.Error("Sesión inválida")
            }

        } catch (e: Exception) {
            clearSession()
            Resource.Error("Error en startup")
        }
    }

    override suspend fun testProtectedEndpoint(): Resource<String> {
        return try {
            Resource.Success(authApi.testProtected())
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                clearSession()
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

    private inline suspend fun <T> safeAuthCall(
        crossinline errorMapper: (HttpStatusCode, String?) -> String,
        crossinline block: suspend () -> Resource<T>
    ): Resource<T> {
        return try {
            block()
        } catch (e: ClientRequestException) {
            Resource.Error(errorMapper(e.response.status, parseErrorMessage(e)))
        } catch (e: ServerResponseException) {
            Resource.Error("Error del servidor (${e.response.status.value})")
        } catch (_: IOException) {
            Resource.Error("Error de red")
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected auth error", e)
            Resource.Error("Error inesperado")
        }
    }

    private fun persistSession(accessToken: String, refreshToken: String?) {
        tokenStore.saveToken(accessToken)

        if (!refreshToken.isNullOrBlank()) {
            tokenStore.saveRefreshToken(refreshToken)
        }

        networkHttpClientProvider.clearBearerTokenCache()
    }

    private fun clearSession() {
        tokenStore.clearToken()
        tokenStore.clearRefreshToken()
        networkHttpClientProvider.clearBearerTokenCache()
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
            else -> "Error inesperado (${status.value})"
        }
    }

    private fun mapRegisterError(status: HttpStatusCode, backendMessage: String?): String {
        return when (status) {
            HttpStatusCode.BadRequest -> backendMessage ?: "Datos inválidos"
            HttpStatusCode.Conflict -> backendMessage ?: "Conflicto al registrarse"
            HttpStatusCode.Unauthorized -> backendMessage ?: "No autorizado"
            else -> "Error inesperado (${status.value})"
        }
    }

    companion object {
        private const val TAG = "AuthRepositoryImpl"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
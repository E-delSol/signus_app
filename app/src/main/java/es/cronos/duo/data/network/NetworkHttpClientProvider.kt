package es.cronos.duo.data.network

import es.cronos.duo.data.local.TokenStore
import es.cronos.duo.data.remote.dto.RefreshTokenResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody

class NetworkHttpClientProvider(
    private val tokenStore: TokenStore,
    private val endpointConfig: NetworkEndpointConfig,
    private val clientInstanceIdProvider: ClientInstanceIdProvider,
    private val appVersionProvider: AppVersionProvider,
    private val versionEnforcementInterceptor: VersionEnforcementInterceptor
) {
    val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                }
            )
        }
        install(WebSockets)
        install(Auth) {
            bearer {
                loadTokens { buildStoredTokens() }
                sendWithoutRequest { request ->
                    request.url.encodedPath != REFRESH_ENDPOINT_PATH
                }
                refreshTokens {
                    val currentRefreshToken = tokenStore.getRefreshToken() ?: oldTokens?.refreshToken
                    if (currentRefreshToken.isNullOrBlank()) return@refreshTokens null

                    val refreshedTokens = runCatching {
                        client.post(REFRESH_ENDPOINT_PATH) {
                            markAsRefreshTokenRequest()
                            setBody(mapOf("refreshToken" to currentRefreshToken))
                        }.body<RefreshTokenResponseDto>()
                    }.getOrNull() ?: return@refreshTokens null

                    BearerTokens(
                        accessToken = refreshedTokens.accessToken,
                        refreshToken = currentRefreshToken
                    ).also(::persistTokens)
                }
            }
        }

        defaultRequest {
            url(endpointConfig.httpBaseUrl)
            contentType(ContentType.Application.Json)
            headers.append(VersionEnforcementContract.VERSION_HEADER, appVersionProvider.versionName.substringBefore("-"))
            if (headers[CLIENT_INSTANCE_HEADER] == null) {
                headers.append(CLIENT_INSTANCE_HEADER, clientInstanceIdProvider.getId())
            }
        }

        engine {
            addInterceptor(Interceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                val code = response.code
                if (code == HttpStatusCode.UpgradeRequired.value || code == HttpStatusCode.BadRequest.value) {
                    val body = response.peekBody(Long.MAX_VALUE).string()
                    versionEnforcementInterceptor.intercept(HttpStatusCode.fromValue(code), body)
                }
                response
            })
        }
    }

    fun clearBearerTokenCache() {
        client.plugin(Auth).providers
            .filterIsInstance<BearerAuthProvider>()
            .forEach { it.clearToken() }
    }

    private fun buildStoredTokens(): BearerTokens? {
        val accessToken = tokenStore.getToken()
        val refreshToken = tokenStore.getRefreshToken()

        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            return null
        }

        return BearerTokens(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private fun persistTokens(tokens: BearerTokens) {
        tokenStore.saveToken(tokens.accessToken)
        tokenStore.saveRefreshToken(tokens.refreshToken)
    }

    companion object {
        const val CLIENT_INSTANCE_HEADER = "X-Client-Instance-Id"
        private const val REFRESH_ENDPOINT_PATH = "/auth/refresh"
    }
}

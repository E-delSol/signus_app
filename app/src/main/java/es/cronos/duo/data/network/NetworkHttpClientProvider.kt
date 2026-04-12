package es.cronos.duo.data.network

import es.cronos.duo.data.local.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class NetworkHttpClientProvider(
    private val tokenStore: TokenStore,
    private val endpointConfig: NetworkEndpointConfig,
    private val clientInstanceIdProvider: ClientInstanceIdProvider
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

        defaultRequest {
            url(endpointConfig.httpBaseUrl)
            contentType(ContentType.Application.Json)
            val token = tokenStore.getToken()
            if (!token.isNullOrBlank() && headers[HttpHeaders.Authorization] == null) {
                headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
            if (headers[CLIENT_INSTANCE_HEADER] == null) {
                headers.append(CLIENT_INSTANCE_HEADER, clientInstanceIdProvider.getId())
            }
        }
    }

    companion object {
        const val CLIENT_INSTANCE_HEADER = "X-Client-Instance-Id"
    }
}

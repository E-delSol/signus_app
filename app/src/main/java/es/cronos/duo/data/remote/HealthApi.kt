package es.cronos.duo.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class HealthApi(
    private val httpClient: HttpClient
) {
    suspend fun getHealth(): String {
        return httpClient.get("/health").body()
    }
}

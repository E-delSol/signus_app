package es.cronos.duo.data.remote

import es.cronos.duo.data.remote.dto.MeResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class MeApi(
    private val httpClient: HttpClient
) {
    suspend fun getMe(): MeResponseDto {
        return httpClient.get("/me").body()
    }
}

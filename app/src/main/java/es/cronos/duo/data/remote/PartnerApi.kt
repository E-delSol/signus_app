package es.cronos.duo.data.remote

import es.cronos.duo.data.remote.dto.PartnerResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.get

class PartnerApi(
    private val httpClient: HttpClient
) {
    suspend fun getPartner(): PartnerResponseDto {
        return httpClient.get("/partner") {
            expectSuccess = true
        }.body()
    }

    suspend fun deletePartner() {
        httpClient.delete("/partner") {
            expectSuccess = true
        }
    }
}

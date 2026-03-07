package es.cronos.duo.data.remote

import es.cronos.duo.data.remote.dto.ConfirmPairingSessionRequestDto
import es.cronos.duo.data.remote.dto.CreatePairingSessionResponseDto
import es.cronos.duo.data.remote.dto.GetLinkSessionStatusResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class PairingApi(
    private val httpClient: HttpClient
) {
    suspend fun createPairingSession(): CreatePairingSessionResponseDto {
        return httpClient.post("/linking/sessions").body()
    }

    suspend fun confirmPairingSession(linkCode: String) {
        httpClient.post("/linking/sessions/confirm") {
            expectSuccess = true
            setBody(ConfirmPairingSessionRequestDto(linkCode = linkCode))
        }
    }

    suspend fun getLinkSessionStatus(sessionId: String): GetLinkSessionStatusResponseDto {
        return httpClient.get("/linking/sessions/$sessionId").body()
    }
}

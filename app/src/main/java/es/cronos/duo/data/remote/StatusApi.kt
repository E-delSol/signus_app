package es.cronos.duo.data.remote

import es.cronos.duo.data.remote.dto.UpdateStatusRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.patch
import io.ktor.client.request.setBody

class StatusApi(
    private val httpClient: HttpClient
) {
    suspend fun updateStatus(status: String) {
        httpClient.patch("/status") {
            expectSuccess = true
            setBody(UpdateStatusRequestDto(status = status))
        }
    }
}

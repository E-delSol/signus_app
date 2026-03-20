package es.cronos.duo.data.remote

import es.cronos.duo.data.remote.dto.UpsertDeviceTokenRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.put
import io.ktor.client.request.setBody

class DeviceApi(
    private val httpClient: HttpClient
) {
    suspend fun registerOrUpdateDeviceToken(request: UpsertDeviceTokenRequest) {
        httpClient.put("/devices/fcm-token") {
            expectSuccess = true
            setBody(request)
        }
    }

    suspend fun deactivateDeviceToken(deviceId: String) {
        httpClient.delete("/devices/fcm-token/$deviceId") {
            expectSuccess = true
        }
    }
}

package es.cronos.duo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpsertDeviceTokenRequest(
    val deviceId: String,
    val fcmToken: String,
    val platform: String,
    val appVersion: String
)

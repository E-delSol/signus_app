package es.cronos.duo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreatePairingSessionResponseDto(
    val sessionId: String,
    val linkCode: String,
    val expiresAt: String
)

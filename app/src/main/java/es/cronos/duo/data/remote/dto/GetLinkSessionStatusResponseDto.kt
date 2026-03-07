package es.cronos.duo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GetLinkSessionStatusResponseDto(
    val sessionId: String,
    val status: String
)

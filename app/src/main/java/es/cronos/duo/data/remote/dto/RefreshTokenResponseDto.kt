package es.cronos.duo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenResponseDto (
    val accessToken: String
)
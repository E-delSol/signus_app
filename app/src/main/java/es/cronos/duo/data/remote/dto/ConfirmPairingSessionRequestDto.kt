package es.cronos.duo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ConfirmPairingSessionRequestDto(
    val linkCode: String
)

package es.cronos.duo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PartnerResponseDto(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val partnerId: String? = null,
    val status: String? = null,
    val statusExpiration: Long? = null,
    val statusDuration: Long? = null
)

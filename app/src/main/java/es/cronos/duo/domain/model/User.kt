package es.cronos.duo.domain.model

data class User(
    val id: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val partnerId: String? = null, // ID del usuario vinculado
    val status: SemaphoreStatus? = null // Estado actual del semáforo
)
package es.cronos.duo.domain.model

data class LinkSession(
    val sessionId: String,
    val linkCode: String,
    val expiresAt: String
)

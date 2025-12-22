package es.cronos.duo.domain.repository

interface QrCodeRepository {
    suspend fun generateUniqueCode(): String
    suspend fun linkPartner(code: String): Boolean
    suspend fun deleteSession()
}
package es.cronos.duo.domain.repository

import es.cronos.duo.domain.model.LinkSession
import es.cronos.duo.domain.model.LinkSessionStatus

interface QrCodeRepository {
    suspend fun generateUniqueCode(): LinkSession
    suspend fun linkPartner(code: String): Boolean
    suspend fun getLinkSessionStatus(sessionId: String): LinkSessionStatus
    suspend fun deleteSession()
}

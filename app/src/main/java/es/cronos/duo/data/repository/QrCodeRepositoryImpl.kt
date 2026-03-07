package es.cronos.duo.data.repository

import es.cronos.duo.data.remote.PairingApi
import es.cronos.duo.domain.model.LinkSession
import es.cronos.duo.domain.model.LinkSessionStatus
import es.cronos.duo.domain.repository.QrCodeRepository
import io.ktor.client.plugins.ClientRequestException

class QrCodeRepositoryImpl(
    private val pairingApi: PairingApi
) : QrCodeRepository {

    override suspend fun generateUniqueCode(): LinkSession {
        val response = pairingApi.createPairingSession()
        return LinkSession(
            sessionId = response.sessionId,
            linkCode = response.linkCode,
            expiresAt = response.expiresAt
        )
    }

    override suspend fun linkPartner(code: String): Boolean {
        return runCatching {
            pairingApi.confirmPairingSession(code)
            true
        }.getOrElse {
            false
        }
    }

    override suspend fun getLinkSessionStatus(sessionId: String): LinkSessionStatus {
        return try {
            when (pairingApi.getLinkSessionStatus(sessionId).status.uppercase()) {
                "PENDING" -> LinkSessionStatus.PENDING
                "CONFIRMED" -> LinkSessionStatus.CONFIRMED
                "EXPIRED" -> LinkSessionStatus.EXPIRED
                else -> LinkSessionStatus.EXPIRED
            }
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                LinkSessionStatus.EXPIRED
            } else {
                throw e
            }
        }
    }

    override suspend fun deleteSession() {
        // TODO: Add backend endpoint for unlink/delete session when contract is available.
    }
}

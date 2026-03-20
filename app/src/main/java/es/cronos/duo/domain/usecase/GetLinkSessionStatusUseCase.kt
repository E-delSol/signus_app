package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.LinkSessionStatus
import es.cronos.duo.domain.repository.QrCodeRepository

class GetLinkSessionStatusUseCase(
    private val repository: QrCodeRepository
) {
    suspend operator fun invoke(sessionId: String): LinkSessionStatus {
        return repository.getLinkSessionStatus(sessionId)
    }
}

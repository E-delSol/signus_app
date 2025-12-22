package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.repository.QrCodeRepository

class UnlinkPartnerUseCase(private val repository: QrCodeRepository) {
    suspend operator fun invoke() {
        repository.deleteSession()
    }
}
package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.repository.QrCodeRepository

class LinkPartnerUseCase(private val repository: QrCodeRepository) {
    suspend operator fun invoke(code: String): Boolean {
        return repository.linkPartner(code)
    }
}
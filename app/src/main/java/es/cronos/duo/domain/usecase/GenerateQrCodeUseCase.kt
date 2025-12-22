package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.repository.QrCodeRepository

class GenerateQrCodeUseCase(private val repository: QrCodeRepository) {
    suspend operator fun invoke(): String {
        return repository.generateUniqueCode()
    }
}
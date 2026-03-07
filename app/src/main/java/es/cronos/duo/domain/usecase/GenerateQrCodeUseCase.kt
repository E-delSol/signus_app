package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.LinkSession
import es.cronos.duo.domain.repository.QrCodeRepository

class GenerateQrCodeUseCase(private val repository: QrCodeRepository) {
    suspend operator fun invoke(): LinkSession {
        return repository.generateUniqueCode()
    }
}

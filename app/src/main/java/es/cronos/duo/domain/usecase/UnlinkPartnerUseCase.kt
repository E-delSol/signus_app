package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.repository.UserRepository

class UnlinkPartnerUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke() {
        userRepository.unlinkPartner()
    }
}

package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.repository.UserRepository

class UpdateUserStatusUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(status: SemaphoreStatus, expirationTimestamp: Long? = null, statusDuration: Long? = null) = 
        userRepository.updateUserStatus(status, expirationTimestamp, statusDuration)
}
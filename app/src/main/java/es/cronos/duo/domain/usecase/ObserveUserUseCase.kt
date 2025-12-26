package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserUseCase(private val userRepository: UserRepository) {
    operator fun invoke(): Flow<User?> = userRepository.observeUser()
}
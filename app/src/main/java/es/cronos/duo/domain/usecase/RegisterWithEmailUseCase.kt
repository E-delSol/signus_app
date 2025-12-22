package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.util.Resource
import kotlinx.coroutines.flow.Flow

class RegisterWithEmailUseCase(private val repository: AuthRepository) {
    operator fun invoke(email: String, password: String): Flow<Resource<User>> {
        return repository.registerWithEmail(email, password)
    }
}
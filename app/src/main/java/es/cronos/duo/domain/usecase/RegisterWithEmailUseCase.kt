package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RegisterWithEmailUseCase(private val repository: AuthRepository) {
    operator fun invoke(
        email: String,
        password: String,
        displayName: String
    ): Flow<Resource<User>> {
        return flow {
            emit(Resource.Loading())
            emit(repository.register(email, password, displayName))
        }
    }
}

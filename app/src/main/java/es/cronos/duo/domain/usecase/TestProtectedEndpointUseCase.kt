package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.repository.AuthRepository

class TestProtectedEndpointUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.testProtectedEndpoint()
}

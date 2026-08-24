package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.repository.AuthRepository

class AppStartupUseCase(
    private val authRepository: AuthRepository
) {
    sealed interface Result {
        data object Authenticated : Result
        data object Guest : Result
    }

    suspend operator fun invoke(): Result {
        if (authRepository.isLoggedIn()) {
            return Result.Authenticated
        }
        authRepository.startupCheck()
        return Result.Guest
    }
}

package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.repository.AuthRepository

class SignInWithGoogleUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String) = repository.signInWithGoogle(idToken)
}
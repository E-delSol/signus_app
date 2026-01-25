package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.util.Resource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Test

class SignInWithGoogleUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val signInWithGoogleUseCase = SignInWithGoogleUseCase(repository)

    @Test
    fun `given valid idToken when signInWithGoogle is called then return success with user`() = runTest {
        // Given
        val idToken = "valid_token"
        val user = User(id = "3", email = "googleuser@example.com")
        coEvery { repository.signInWithGoogle(idToken) } returns Resource.Success(user)

        // When
        val result = signInWithGoogleUseCase(idToken)

        // Then
        result shouldBeInstanceOf Resource.Success::class
        (result as Resource.Success).data shouldBeEqualTo user
    }

    @Test
    fun `given invalid idToken when signInWithGoogle is called then return error`() = runTest {
        // Given
        val idToken = "invalid_token"
        val errorMessage = "Google sign in failed"
        coEvery { repository.signInWithGoogle(idToken) } returns Resource.Error(errorMessage)

        // When
        val result = signInWithGoogleUseCase(idToken)

        // Then
        result shouldBeInstanceOf Resource.Error::class
        (result as Resource.Error).message shouldBeEqualTo errorMessage
    }
}

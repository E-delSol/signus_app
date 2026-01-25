package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.util.Resource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class LoginWithEmailUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val loginWithEmailUseCase = LoginWithEmailUseCase(repository)

    @Test
    fun `given valid credentials when login is called then return success with user`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val user = User(id = "1", email = email)
        every { repository.loginWithEmail(email, password) } returns flowOf(
            Resource.Loading(),
            Resource.Success(user)
        )

        // When
        val result = loginWithEmailUseCase(email, password).toList()

        // Then
        result[0] shouldBeInstanceOf Resource.Loading::class
        result[1] shouldBeInstanceOf Resource.Success::class
        (result[1] as Resource.Success).data shouldBeEqualTo user
    }

    @Test
    fun `given invalid credentials when login is called then return error`() = runTest {
        // Given
        val email = "wrong@example.com"
        val password = "wrongpassword"
        val errorMessage = "Invalid credentials"
        every { repository.loginWithEmail(email, password) } returns flowOf(
            Resource.Loading(),
            Resource.Error(errorMessage)
        )

        // When
        val result = loginWithEmailUseCase(email, password).toList()

        // Then
        result[0] shouldBeInstanceOf Resource.Loading::class
        result[1] shouldBeInstanceOf Resource.Error::class
        (result[1] as Resource.Error).message shouldBeEqualTo errorMessage
    }

    @Test
    fun `given empty email when login is called then repository handles it and returns error`() = runTest {
        // Given
        val email = ""
        val password = "password123"
        val errorMessage = "Email cannot be empty"
        every { repository.loginWithEmail(email, password) } returns flowOf(
            Resource.Error(errorMessage)
        )

        // When
        val result = loginWithEmailUseCase(email, password).toList()

        // Then
        result[0] shouldBeInstanceOf Resource.Error::class
        (result[0] as Resource.Error).message shouldBeEqualTo errorMessage
    }
}

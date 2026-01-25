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

class RegisterWithEmailUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val registerWithEmailUseCase = RegisterWithEmailUseCase(repository)

    @Test
    fun `given valid credentials when register is called then return success with user`() = runTest {
        // Given
        val email = "newuser@example.com"
        val password = "password123"
        val user = User(id = "2", email = email)
        every { repository.registerWithEmail(email, password) } returns flowOf(
            Resource.Loading(),
            Resource.Success(user)
        )

        // When
        val result = registerWithEmailUseCase(email, password).toList()

        // Then
        result[0] shouldBeInstanceOf Resource.Loading::class
        result[1] shouldBeInstanceOf Resource.Success::class
        (result[1] as Resource.Success).data shouldBeEqualTo user
    }

    @Test
    fun `given existing email when register is called then return error`() = runTest {
        // Given
        val email = "existing@example.com"
        val password = "password123"
        val errorMessage = "Email already in use"
        every { repository.registerWithEmail(email, password) } returns flowOf(
            Resource.Loading(),
            Resource.Error(errorMessage)
        )

        // When
        val result = registerWithEmailUseCase(email, password).toList()

        // Then
        result[0] shouldBeInstanceOf Resource.Loading::class
        result[1] shouldBeInstanceOf Resource.Error::class
        (result[1] as Resource.Error).message shouldBeEqualTo errorMessage
    }
}

package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.util.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Test

class RegisterWithEmailUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val registerWithEmailUseCase = RegisterWithEmailUseCase(repository)

    @Test
    fun `given valid data when invoke is called then emit loading and repository success`() = runTest {
        val email = "newuser@example.com"
        val password = "password123"
        val displayName = "New User"
        val user = User(id = "backend_user", email = email, displayName = displayName)
        coEvery { repository.register(email, password, displayName) } returns Resource.Success(user)

        val result = registerWithEmailUseCase(email, password, displayName).toList()

        result[0] shouldBeInstanceOf Resource.Loading::class
        result[1] shouldBeInstanceOf Resource.Success::class
        result[1].data shouldBeEqualTo user
        coVerify(exactly = 1) { repository.register(email, password, displayName) }
    }

    @Test
    fun `given conflicting email when invoke is called then emit loading and repository error`() = runTest {
        val email = "existing@example.com"
        val password = "password123"
        val displayName = "Existing User"
        val errorMessage = "Conflicto al registrarse"
        coEvery { repository.register(email, password, displayName) } returns Resource.Error(errorMessage)

        val result = registerWithEmailUseCase(email, password, displayName).toList()

        result[0] shouldBeInstanceOf Resource.Loading::class
        result[1] shouldBeInstanceOf Resource.Error::class
        result[1].message shouldBeEqualTo errorMessage
        coVerify(exactly = 1) { repository.register(email, password, displayName) }
    }
}

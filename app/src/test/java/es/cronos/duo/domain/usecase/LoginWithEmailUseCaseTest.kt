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

class LoginWithEmailUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val loginWithEmailUseCase = LoginWithEmailUseCase(repository)

    @Test
    fun `given valid credentials when invoke is called then emit loading and repository success`() = runTest {
        val email = "test@example.com"
        val password = "password123"
        val user = User(id = "backend_user", email = email)
        coEvery { repository.login(email, password) } returns Resource.Success(user)

        val result = loginWithEmailUseCase(email, password).toList()

        result[0] shouldBeInstanceOf Resource.Loading::class
        result[1] shouldBeInstanceOf Resource.Success::class
        result[1].data shouldBeEqualTo user
        coVerify(exactly = 1) { repository.login(email, password) }
    }

    @Test
    fun `given invalid credentials when invoke is called then emit loading and repository error`() = runTest {
        val email = "wrong@example.com"
        val password = "wrongpassword"
        val errorMessage = "Credenciales inválidas"
        coEvery { repository.login(email, password) } returns Resource.Error(errorMessage)

        val result = loginWithEmailUseCase(email, password).toList()

        result[0] shouldBeInstanceOf Resource.Loading::class
        result[1] shouldBeInstanceOf Resource.Error::class
        result[1].message shouldBeEqualTo errorMessage
        coVerify(exactly = 1) { repository.login(email, password) }
    }
}

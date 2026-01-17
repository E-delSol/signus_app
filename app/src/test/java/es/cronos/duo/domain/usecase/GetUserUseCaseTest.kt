package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class GetUserUseCaseTest {

    private val userRepository: UserRepository = mockk()
    private val getUserUseCase = GetUserUseCase(userRepository)

    @Test
    fun `given a user exists when invoke is called then return the user`() = runTest {
        // Given
        val expectedUser = User(id = "user123")
        coEvery { userRepository.getUser() } returns expectedUser

        // When
        val result = getUserUseCase()

        // Then
        result shouldBeEqualTo expectedUser
    }

    @Test
    fun `given no user exists when invoke is called then return null`() = runTest {
        // Given
        coEvery { userRepository.getUser() } returns null

        // When
        val result = getUserUseCase()

        // Then
        result shouldBeEqualTo null
    }
}

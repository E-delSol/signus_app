package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class ObserveUserUseCaseTest {

    private val userRepository: UserRepository = mockk()
    private val observeUserUseCase = ObserveUserUseCase(userRepository)

    @Test
    fun `when invoke is called then return user flow from repository`() = runTest {
        // Given
        val expectedUser = User(id = "user123")
        every { userRepository.observeUser() } returns flowOf(expectedUser)

        // When
        val result = observeUserUseCase().toList()

        // Then
        result[0] shouldBeEqualTo expectedUser
    }
}

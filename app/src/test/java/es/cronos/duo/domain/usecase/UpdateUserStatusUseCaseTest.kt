package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateUserStatusUseCaseTest {

    private val userRepository: UserRepository = mockk()
    private val updateUserStatusUseCase = UpdateUserStatusUseCase(userRepository)

    @Test
    fun `when invoke is called then call repository to update user status`() = runTest {
        // Given
        val status = SemaphoreStatus.AVAILABLE
        val expiration = 123456789L
        val duration = 3600L
        coEvery { userRepository.updateUserStatus(status, expiration, duration) } returns Unit

        // When
        updateUserStatusUseCase(status, expiration, duration)

        // Then
        coVerify(exactly = 1) { userRepository.updateUserStatus(status, expiration, duration) }
    }

    @Test
    fun `when invoke is called with nulls then call repository with nulls`() = runTest {
        // Given
        val status = SemaphoreStatus.BUSY
        coEvery { userRepository.updateUserStatus(status, null, null) } returns Unit

        // When
        updateUserStatusUseCase(status, null, null)

        // Then
        coVerify(exactly = 1) { userRepository.updateUserStatus(status, null, null) }
    }
}

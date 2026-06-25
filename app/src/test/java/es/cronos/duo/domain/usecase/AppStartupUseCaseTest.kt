package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.util.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AppStartupUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val useCase = AppStartupUseCase(authRepository)

    @Test
    fun `when user is logged in then calls refreshSession and returns Authenticated`() = runTest {
        every { authRepository.isLoggedIn() } returns true
        coEvery { authRepository.refreshSession() } returns true

        val result = useCase()

        result shouldBeEqualTo AppStartupUseCase.Result.Authenticated
        coVerify(exactly = 1) { authRepository.refreshSession() }
        coVerify(exactly = 0) { authRepository.startupCheck() }
    }

    @Test
    fun `when user is not logged in then calls startupCheck and returns Guest`() = runTest {
        every { authRepository.isLoggedIn() } returns false
        coEvery { authRepository.startupCheck() } returns Resource.Success(Unit)

        val result = useCase()

        result shouldBeEqualTo AppStartupUseCase.Result.Guest
        coVerify(exactly = 1) { authRepository.startupCheck() }
        coVerify(exactly = 0) { authRepository.refreshSession() }
    }
}

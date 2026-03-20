package es.cronos.duo.presentation.settings

import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.usecase.GetHealthUseCase
import es.cronos.duo.domain.usecase.UnlinkPartnerUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val unlinkPartnerUseCase: UnlinkPartnerUseCase = mockk()
    private val getHealthUseCase: GetHealthUseCase = mockk()

    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getHealthUseCase() } returns "ok"
        viewModel = SettingsViewModel(authRepository, unlinkPartnerUseCase, getHealthUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given user wants to logout when onLogout is called then authRepository signOut should be invoked`() = runTest {
        viewModel.onLogout()
        advanceUntilIdle()

        coVerify { authRepository.logout() }
    }

    @Test
    fun `given user wants to unlink when onUnlinkPartner is called then unlinkPartnerUseCase should be invoked`() = runTest {
        coEvery { unlinkPartnerUseCase() } returns Unit

        viewModel.onUnlinkPartner()
        advanceUntilIdle()

        coVerify { unlinkPartnerUseCase() }
    }
}

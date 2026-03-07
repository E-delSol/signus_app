package es.cronos.duo.presentation.pairing

import es.cronos.duo.domain.model.LinkSession
import es.cronos.duo.domain.model.LinkSessionStatus
import es.cronos.duo.domain.usecase.GenerateQrCodeUseCase
import es.cronos.duo.domain.usecase.GetLinkSessionStatusUseCase
import es.cronos.duo.domain.usecase.LinkPartnerUseCase
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
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {

    private val generateQrCodeUseCase: GenerateQrCodeUseCase = mockk()
    private val linkPartnerUseCase: LinkPartnerUseCase = mockk()
    private val getLinkSessionStatusUseCase: GetLinkSessionStatusUseCase = mockk()

    private lateinit var viewModel: PairingViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PairingViewModel(
            generateQrCodeUseCase,
            linkPartnerUseCase,
            getLinkSessionStatusUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onGenerateQrClick stores linkCode and polls with sessionId`() = runTest {
        val sessionId = "session-123"
        val linkCode = "ABC123"
        coEvery { generateQrCodeUseCase() } returns LinkSession(
            sessionId = sessionId,
            linkCode = linkCode,
            expiresAt = "2026-03-06T12:00:00Z"
        )
        coEvery { getLinkSessionStatusUseCase(sessionId) } returns LinkSessionStatus.CONFIRMED

        viewModel.onGenerateQrClick()
        advanceUntilIdle()

        viewModel.state.value.isPaired shouldBeEqualTo true
        viewModel.state.value.linkCode shouldBeEqualTo linkCode
        coVerify { generateQrCodeUseCase() }
        coVerify { getLinkSessionStatusUseCase(sessionId) }
    }

    @Test
    fun `onCodeScanned confirms linking and sets paired state`() = runTest {
        val sessionId = "session-123"
        coEvery { linkPartnerUseCase(sessionId) } returns true

        viewModel.onCodeScanned(sessionId)
        advanceUntilIdle()

        viewModel.state.value.isPaired shouldBeEqualTo true
        coVerify { linkPartnerUseCase(sessionId) }
    }
}

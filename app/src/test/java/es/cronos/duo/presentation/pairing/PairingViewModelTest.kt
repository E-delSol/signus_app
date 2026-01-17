package es.cronos.duo.presentation.pairing

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.usecase.GenerateQrCodeUseCase
import es.cronos.duo.domain.usecase.LinkPartnerUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val userRepository: UserRepository = mockk()

    private val userFlow = MutableStateFlow<User?>(null)

    private lateinit var viewModel: PairingViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { userRepository.observeUser() } returns userFlow
        viewModel = PairingViewModel(
            generateQrCodeUseCase,
            linkPartnerUseCase,
            userRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given user with partnerId when observing user then state isPaired should be true`() = runTest {
        val userWithPartner = User(id = "1", partnerId = "partner123")
        
        userFlow.value = userWithPartner
        advanceUntilIdle()

        viewModel.state.value.isPaired shouldBeEqualTo true
    }

    @Test
    fun `given user without partnerId when observing user then state isPaired should be false`() = runTest {
        val userWithoutPartner = User(id = "1", partnerId = null)
        
        userFlow.value = userWithoutPartner
        advanceUntilIdle()

        viewModel.state.value.isPaired shouldBeEqualTo false
    }

    @Test
    fun `given click on generate QR when onGenerateQrClick is called then state should show QR with code`() = runTest {
        val generatedCode = "unique_qr_code"
        coEvery { generateQrCodeUseCase() } returns generatedCode

        viewModel.onGenerateQrClick()
        advanceUntilIdle()

        viewModel.state.value shouldBeEqualTo PairingState(
            uniqueCode = generatedCode,
            showQrCode = true,
            isPaired = false
        )
        coVerify { generateQrCodeUseCase() }
    }

    @Test
    fun `given QR is shown when onDismissQr is called then state showQrCode should be false`() = runTest {
        // Simulate initial state with QR shown
        coEvery { generateQrCodeUseCase() } returns "code"
        viewModel.onGenerateQrClick()
        advanceUntilIdle()

        viewModel.onDismissQr()

        viewModel.state.value.showQrCode shouldBeEqualTo false
    }

    @Test
    fun `given a scanned code when onCodeScanned is called then linkPartnerUseCase should be invoked`() = runTest {
        val scannedCode = "partner_code"
        coEvery { linkPartnerUseCase(scannedCode) } returns true

        viewModel.onCodeScanned(scannedCode)
        advanceUntilIdle()

        coVerify { linkPartnerUseCase(scannedCode) }
    }
}

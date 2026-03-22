package es.cronos.duo.presentation.semaphore

import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.usecase.GetPartnerStatusUseCase
import es.cronos.duo.domain.usecase.ObserveUserUseCase
import es.cronos.duo.domain.usecase.UpdateUserStatusUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
class SemaphoreViewModelTest {

    private val userRepository: UserRepository = mockk(relaxed = true)
    private val observeUserUseCase: ObserveUserUseCase = mockk()
    private val updateUserStatusUseCase: UpdateUserStatusUseCase = mockk()
    private val getPartnerStatusUseCase: GetPartnerStatusUseCase = mockk()

    private val userFlow = MutableSharedFlow<User?>(replay = 1)
    private val partnerFlow = MutableSharedFlow<User?>(replay = 1)

    private lateinit var viewModel: SemaphoreViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { observeUserUseCase() } returns userFlow
        coEvery { getPartnerStatusUseCase(any()) } returns partnerFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel() {
        viewModel = SemaphoreViewModel(
            userRepository,
            observeUserUseCase,
            updateUserStatusUseCase,
            getPartnerStatusUseCase
        )
    }

    @Test
    fun `given initial user state when viewModel is created then state should update with user status`() = runTest {
        val user = User(id = "1", status = SemaphoreStatus.AVAILABLE, partnerId = "partner123")
        userFlow.tryEmit(user)
        partnerFlow.tryEmit(User(id = "partner123", status = SemaphoreStatus.BUSY))

        initViewModel()
        advanceUntilIdle()

        viewModel.state.value.userStatus shouldBeEqualTo SemaphoreStatus.AVAILABLE
        viewModel.state.value.isPaired shouldBeEqualTo true
    }

    @Test
    fun `given user changes status when onUserStatusClick is called then updateUserStatusUseCase should be invoked with next status`() = runTest {
        val user = User(id = "1", status = SemaphoreStatus.AVAILABLE)
        userFlow.tryEmit(user)
        initViewModel()
        advanceUntilIdle()

        coEvery { updateUserStatusUseCase(SemaphoreStatus.BUSY, null, null) } returns Unit

        viewModel.onUserStatusClick()
        advanceUntilIdle()

        coVerify { updateUserStatusUseCase(SemaphoreStatus.BUSY, null, null) }
    }

    @Test
    fun `given partner status changes when partnerFlow emits then state partnerStatus should update`() = runTest {
        val user = User(id = "1", partnerId = "partner123")
        userFlow.tryEmit(user)
        initViewModel()
        advanceUntilIdle()

        val partnerUser = User(
            id = "partner123",
            displayName = "Taylor",
            status = SemaphoreStatus.AVAILABLE
        )
        partnerFlow.tryEmit(partnerUser)
        advanceUntilIdle()

        viewModel.state.value.partnerStatus shouldBeEqualTo SemaphoreStatus.AVAILABLE
        viewModel.state.value.partnerDisplayName shouldBeEqualTo "Taylor"
    }

    @Test
    fun `given timer selected when onTimerSelected is called then pendingDurationMillis should be set and later used in status click`() = runTest {
        val user = User(id = "1", status = SemaphoreStatus.AVAILABLE)
        userFlow.tryEmit(user)
        initViewModel()
        advanceUntilIdle()

        // Select 1 hour timer
        viewModel.onTimerSelected(1, 0)
        advanceUntilIdle()

        // verify state updated (userStatusDuration)
        viewModel.state.value.userStatusDuration shouldBeEqualTo 3600000L

        // Click status to apply timer
        coEvery { updateUserStatusUseCase(SemaphoreStatus.BUSY, any(), 3600000L) } returns Unit
        viewModel.onUserStatusClick()
        advanceUntilIdle()

        // Then
        coVerify { updateUserStatusUseCase(SemaphoreStatus.BUSY, any(), 3600000L) }
    }

    @Test
    fun `given user unlinked when userFlow emits null partnerId then state isPaired should be false`() = runTest {
        val pairedUser = User(id = "1", partnerId = "partner123")
        userFlow.tryEmit(pairedUser)
        initViewModel()
        advanceUntilIdle()

        val unlinkedUser = User(id = "1", partnerId = null)
        userFlow.tryEmit(unlinkedUser)
        advanceUntilIdle()

        viewModel.state.value.isPaired shouldBeEqualTo false
        viewModel.state.value.partnerDisplayName shouldBeEqualTo null
    }

    @Test
    fun `given realtime partner unlink when userFlow clears partnerId then show dialog and clear partner state`() = runTest {
        val pairedUser = User(id = "1", partnerId = "partner123")
        userFlow.tryEmit(pairedUser)
        partnerFlow.tryEmit(User(id = "partner123", status = SemaphoreStatus.AVAILABLE))
        initViewModel()
        advanceUntilIdle()

        val eventDeferred = async { viewModel.eventFlow.first() }

        userFlow.tryEmit(User(id = "1", partnerId = null))
        advanceUntilIdle()

        viewModel.state.value.isPaired shouldBeEqualTo false
        viewModel.state.value.partnerStatus shouldBeEqualTo null
        viewModel.state.value.partnerDisplayName shouldBeEqualTo null
        eventDeferred.await() shouldBeEqualTo SemaphoreViewModel.UiEvent.ShowUnlinkedDialog
    }
}

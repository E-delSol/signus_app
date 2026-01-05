package es.cronos.duo.presentation.semaphore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import es.cronos.duo.data.repository.UserRepositoryImpl
import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.usecase.GetPartnerStatusUseCase
import es.cronos.duo.domain.usecase.ObserveUserUseCase
import es.cronos.duo.domain.usecase.UpdateUserStatusUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SemaphoreViewModel : ViewModel() {

    private val _state = MutableStateFlow(SemaphoreState())
    val state: StateFlow<SemaphoreState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // In a real app, these would be injected by a DI framework like Hilt
    private val userRepository: UserRepository = UserRepositoryImpl()
    private val observeUserUseCase = ObserveUserUseCase(userRepository)
    private val updateUserStatusUseCase = UpdateUserStatusUseCase(userRepository)
    private val getPartnerStatusUseCase = GetPartnerStatusUseCase(userRepository)

    private var pendingDurationMillis: Long? = null
    private var expirationJob: Job? = null

    init {
        viewModelScope.launch {
            // Save FCM token
            try {
                userRepository.saveFcmToken(FirebaseMessaging.getInstance().token.await())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            var previousPartnerStatus: SemaphoreStatus? = null

            observeUserUseCase().collectLatest { user ->
                _state.update { it.copy(
                    userStatus = user?.status ?: SemaphoreStatus.BUSY,
                    userStatusExpiration = user?.statusExpiration,
                    userStatusDuration = user?.statusDuration,
                    isPaired = !user?.partnerId.isNullOrBlank()
                ) }

                user?.statusExpiration?.let { checkExpiration(user.status ?: SemaphoreStatus.BUSY, it) }

                val partnerId = user?.partnerId
                if (partnerId.isNullOrBlank()) {
                    if (_state.value.isPaired) { // Was paired, now is not
                        viewModelScope.launch { _eventFlow.emit(UiEvent.ShowUnlinkedDialog) }
                    }
                    _state.update { it.copy(partnerStatus = SemaphoreStatus.BUSY, partnerStatusExpiration = null) }
                } else {
                    getPartnerStatusUseCase(partnerId).collect { partnerUser ->
                        if (previousPartnerStatus != null && previousPartnerStatus != partnerUser?.status) {
                            viewModelScope.launch { _eventFlow.emit(UiEvent.PlayNotificationSound) }
                        }
                        previousPartnerStatus = partnerUser?.status
                        
                        _state.update { it.copy(
                            partnerStatus = partnerUser?.status,
                            partnerStatusExpiration = partnerUser?.statusExpiration
                        ) }
                    }
                }
            }
        }
    }

    private fun checkExpiration(currentStatus: SemaphoreStatus, expiration: Long) {
        expirationJob?.cancel()
        val remaining = expiration - System.currentTimeMillis()
        if (remaining <= 0) {
            revertStatus(currentStatus)
        } else {
            expirationJob = viewModelScope.launch {
                delay(remaining)
                if (_state.value.userStatusExpiration == expiration) {
                    revertStatus(_state.value.userStatus)
                }
            }
        }
    }

    private fun revertStatus(currentStatus: SemaphoreStatus) {
        val newStatus = currentStatus.next()
        viewModelScope.launch {
            updateUserStatusUseCase(newStatus, null, null)
        }
    }

    fun onUserStatusClick() {
        val newStatus = _state.value.userStatus.next()
        val duration = pendingDurationMillis

        if (duration != null && duration > 0) {
            val expirationTimestamp = System.currentTimeMillis() + duration
            pendingDurationMillis = null
            viewModelScope.launch {
                updateUserStatusUseCase(newStatus, expirationTimestamp, duration)
            }
        } else {
            viewModelScope.launch {
                updateUserStatusUseCase(newStatus, null, null)
            }
        }
    }

    fun onTimerSelected(hours: Int, minutes: Int) {
        val durationMillis = (hours * 60 * 60 * 1000L) + (minutes * 60 * 1000L)
        if (durationMillis > 0) {
            pendingDurationMillis = durationMillis
            _state.update { it.copy(userStatusDuration = durationMillis, userStatusExpiration = null) }
            onDismissTimerDialog()
        }
    }

    fun onShowTimerDialog() = viewModelScope.launch { _eventFlow.emit(UiEvent.ShowTimerDialog) }
    fun onDismissTimerDialog() = viewModelScope.launch { _eventFlow.emit(UiEvent.HideTimerDialog) }

    sealed class UiEvent {
        object PlayNotificationSound : UiEvent()
        object ShowTimerDialog : UiEvent()
        object HideTimerDialog : UiEvent()
        object ShowUnlinkedDialog : UiEvent()
    }
}
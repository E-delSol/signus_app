package es.cronos.duo.presentation.semaphore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class SemaphoreViewModel(
    private val userRepository: UserRepository,
    private val observeUserUseCase: ObserveUserUseCase,
    private val updateUserStatusUseCase: UpdateUserStatusUseCase,
    private val getPartnerStatusUseCase: GetPartnerStatusUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SemaphoreState())
    val state: StateFlow<SemaphoreState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var pendingDurationMillis: Long? = null
    private var expirationJob: Job? = null
    private var partnerStatusJob: Job? = null
    private var currentPartnerId: String? = null
    private var previousPartnerStatus: SemaphoreStatus? = null

    init {
        viewModelScope.launch {
            // Sync FCM token using the repository (abstracted from Firebase)
            userRepository.syncFcmToken()

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
                    if (currentPartnerId != null) {
                        viewModelScope.launch { _eventFlow.emit(UiEvent.ShowUnlinkedDialog) }
                    }
                    currentPartnerId = null
                    previousPartnerStatus = null
                    partnerStatusJob?.cancel()
                    partnerStatusJob = null
                    _state.update { it.copy(partnerStatus = SemaphoreStatus.BUSY, partnerStatusExpiration = null) }
                } else {
                    if (partnerId != currentPartnerId) {
                        currentPartnerId = partnerId
                        startPartnerStatusSubscription(partnerId)
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
            runCatching {
                updateUserStatusUseCase(newStatus, null, null)
            }.onSuccess {
                updateOwnStatusLocally(newStatus, null, null)
            }
        }
    }

    fun onUserStatusClick() {
        val newStatus = _state.value.userStatus.next()
        val duration = pendingDurationMillis

        if (duration != null && duration > 0) {
            val expirationTimestamp = System.currentTimeMillis() + duration
            pendingDurationMillis = null
            viewModelScope.launch {
                runCatching {
                    updateUserStatusUseCase(newStatus, expirationTimestamp, duration)
                }.onSuccess {
                    updateOwnStatusLocally(newStatus, expirationTimestamp, duration)
                }
            }
        } else {
            viewModelScope.launch {
                runCatching {
                    updateUserStatusUseCase(newStatus, null, null)
                }.onSuccess {
                    updateOwnStatusLocally(newStatus, null, null)
                }
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

    override fun onCleared() {
        expirationJob?.cancel()
        partnerStatusJob?.cancel()
        super.onCleared()
    }

    private fun updateOwnStatusLocally(
        status: SemaphoreStatus,
        statusExpiration: Long?,
        statusDuration: Long?
    ) {
        _state.update {
            it.copy(
                userStatus = status,
                userStatusExpiration = statusExpiration,
                userStatusDuration = statusDuration
            )
        }

        if (statusExpiration != null) {
            checkExpiration(status, statusExpiration)
        } else {
            expirationJob?.cancel()
            expirationJob = null
        }
    }

    private fun startPartnerStatusSubscription(partnerId: String) {
        partnerStatusJob?.cancel()
        partnerStatusJob = viewModelScope.launch {
            getPartnerStatusUseCase(partnerId).collect { partnerUser ->
                if (previousPartnerStatus != null && previousPartnerStatus != partnerUser?.status) {
                    _eventFlow.emit(UiEvent.PlayNotificationSound)
                }
                previousPartnerStatus = partnerUser?.status

                _state.update {
                    it.copy(
                        partnerStatus = partnerUser?.status,
                        partnerStatusExpiration = partnerUser?.statusExpiration
                    )
                }
            }
        }
    }

    sealed class UiEvent {
        object PlayNotificationSound : UiEvent()
        object ShowTimerDialog : UiEvent()
        object HideTimerDialog : UiEvent()
        object ShowUnlinkedDialog : UiEvent()
    }
}

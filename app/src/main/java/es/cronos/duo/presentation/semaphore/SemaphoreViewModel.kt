package es.cronos.duo.presentation.semaphore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cronos.duo.data.repository.UserRepositoryImpl
import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.usecase.GetPartnerStatusUseCase
import es.cronos.duo.domain.usecase.ObserveUserUseCase
import es.cronos.duo.domain.usecase.UpdateUserStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SemaphoreViewModel : ViewModel() {

    private val _state = MutableStateFlow(SemaphoreState())
    val state: StateFlow<SemaphoreState> = _state.asStateFlow()

    // In a real app, these would be injected by a DI framework like Hilt
    private val userRepository: UserRepository = UserRepositoryImpl()
    private val observeUserUseCase = ObserveUserUseCase(userRepository)
    private val updateUserStatusUseCase = UpdateUserStatusUseCase(userRepository)
    private val getPartnerStatusUseCase = GetPartnerStatusUseCase(userRepository)

    init {
        viewModelScope.launch {
            // Use collectLatest to automatically cancel and restart the partner status listener
            // whenever the user's partnerId changes.
            observeUserUseCase().collectLatest { user ->
                // Update our own status from the user object
                user?.status?.let { userStatus ->
                    _state.update { it.copy(userStatus = userStatus) }
                }

                val partnerId = user?.partnerId
                if (partnerId.isNullOrBlank()) {
                    // If there's no partner, set a default status
                    _state.update { it.copy(partnerStatus = SemaphoreStatus.BUSY) }
                } else {
                    // If there is a partner, collect their status. This inner collect
                    // will be cancelled and restarted if partnerId changes.
                    getPartnerStatusUseCase(partnerId).collect { partnerStatus ->
                        _state.update { it.copy(partnerStatus = partnerStatus) }
                    }
                }
            }
        }
    }

    fun onUserStatusClick() {
        val newStatus = _state.value.userStatus.next()

        // Update UI immediately for better UX
        _state.update { it.copy(userStatus = newStatus) }

        // Launch a coroutine to update the status in Firestore
        viewModelScope.launch {
            updateUserStatusUseCase(newStatus)
        }
    }
}
package es.cronos.duo.presentation.semaphore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cronos.duo.data.repository.UserRepositoryImpl
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.usecase.GetPartnerStatusUseCase
import es.cronos.duo.domain.usecase.GetUserUseCase
import es.cronos.duo.domain.usecase.UpdateUserStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SemaphoreViewModel : ViewModel() {

    private val _state = MutableStateFlow(SemaphoreState())
    val state: StateFlow<SemaphoreState> = _state.asStateFlow()

    // In a real app, these would be injected by a DI framework like Hilt
    private val userRepository: UserRepository = UserRepositoryImpl()
    private val getUserUseCase = GetUserUseCase(userRepository)
    private val updateUserStatusUseCase = UpdateUserStatusUseCase(userRepository)
    private val getPartnerStatusUseCase = GetPartnerStatusUseCase(userRepository)

    init {
        viewModelScope.launch {
            // Load current user data and initial status
            val user = getUserUseCase()
            user?.status?.let { userStatus ->
                _state.update { it.copy(userStatus = userStatus) }
            }

            // If user has a partner, listen for their status changes
            user?.partnerId?.let { partnerId ->
                if (partnerId.isNotBlank()) {
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
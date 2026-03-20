package es.cronos.duo.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.usecase.GetHealthUseCase
import es.cronos.duo.domain.usecase.UnlinkPartnerUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val unlinkPartnerUseCase: UnlinkPartnerUseCase,
    private val getHealthUseCase: GetHealthUseCase
) : ViewModel() {
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        checkBackendHealth()
    }

    fun onLogout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun onUnlinkPartner() {
        viewModelScope.launch {
            runCatching {
                unlinkPartnerUseCase()
            }.onSuccess {
                _eventFlow.emit(UiEvent.UnlinkCompleted)
            }
        }
    }

    private fun checkBackendHealth() {
        viewModelScope.launch {
            runCatching { getHealthUseCase() }.getOrNull()
        }
    }

    sealed class UiEvent {
        object UnlinkCompleted : UiEvent()
    }
}

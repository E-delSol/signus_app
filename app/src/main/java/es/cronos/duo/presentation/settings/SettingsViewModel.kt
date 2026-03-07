package es.cronos.duo.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.usecase.GetHealthUseCase
import es.cronos.duo.domain.usecase.UnlinkPartnerUseCase
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val unlinkPartnerUseCase: UnlinkPartnerUseCase,
    private val getHealthUseCase: GetHealthUseCase
) : ViewModel() {

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
            unlinkPartnerUseCase()
        }
    }

    private fun checkBackendHealth() {
        viewModelScope.launch {
            runCatching { getHealthUseCase() }.getOrNull()
        }
    }
}

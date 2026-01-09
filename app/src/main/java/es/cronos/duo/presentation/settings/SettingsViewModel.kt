package es.cronos.duo.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.usecase.UnlinkPartnerUseCase
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val unlinkPartnerUseCase: UnlinkPartnerUseCase
) : ViewModel() {

    fun onLogout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun onUnlinkPartner() {
        viewModelScope.launch {
            unlinkPartnerUseCase()
        }
    }
}
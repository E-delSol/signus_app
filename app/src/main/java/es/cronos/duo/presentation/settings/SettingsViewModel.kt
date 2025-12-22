package es.cronos.duo.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.cronos.duo.data.repository.AuthRepositoryImpl
import es.cronos.duo.data.repository.QrCodeRepositoryImpl
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

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val authRepository = AuthRepositoryImpl()
                val qrRepository = QrCodeRepositoryImpl()
                val unlinkUseCase = UnlinkPartnerUseCase(qrRepository)
                return SettingsViewModel(authRepository, unlinkUseCase) as T
            }
        }
    }
}
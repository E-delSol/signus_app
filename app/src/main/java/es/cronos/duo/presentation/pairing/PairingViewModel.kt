package es.cronos.duo.presentation.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.usecase.GenerateQrCodeUseCase
import es.cronos.duo.domain.usecase.LinkPartnerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PairingViewModel(
    private val generateQrCodeUseCase: GenerateQrCodeUseCase,
    private val linkPartnerUseCase: LinkPartnerUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PairingState())
    val state: StateFlow<PairingState> = _state.asStateFlow()

    init {
        listenForPairingChanges()
    }

    private fun listenForPairingChanges() {
        viewModelScope.launch {
            userRepository.observeUser().collect { user ->
                if (!user?.partnerId.isNullOrBlank()) {
                    _state.update { it.copy(isPaired = true) }
                }
            }
        }
    }

    fun onGenerateQrClick() {
        viewModelScope.launch {
            val code = generateQrCodeUseCase()
            _state.update { it.copy(uniqueCode = code, showQrCode = true) }
        }
    }

    fun onDismissQr() {
        _state.update { it.copy(showQrCode = false) }
    }

    fun onCodeScanned(code: String) {
        viewModelScope.launch {
            // The listener will automatically update the state, so we just call the use case
            linkPartnerUseCase(code)
        }
    }
}
package es.cronos.duo.presentation.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.cronos.duo.data.repository.QrCodeRepositoryImpl
import es.cronos.duo.domain.usecase.GenerateQrCodeUseCase
import es.cronos.duo.domain.usecase.LinkPartnerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PairingViewModel(
    private val generateQrCodeUseCase: GenerateQrCodeUseCase,
    private val linkPartnerUseCase: LinkPartnerUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PairingState())
    val state: StateFlow<PairingState> = _state.asStateFlow()

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
            // Evitar escaneos múltiples si ya está emparejado
            if (_state.value.isPaired) return@launch

            val success = linkPartnerUseCase(code)
            if (success) {
                _state.update { it.copy(isPaired = true) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = QrCodeRepositoryImpl()
                val generateUseCase = GenerateQrCodeUseCase(repository)
                val linkUseCase = LinkPartnerUseCase(repository)
                return PairingViewModel(generateUseCase, linkUseCase) as T
            }
        }
    }
}
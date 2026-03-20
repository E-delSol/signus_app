package es.cronos.duo.presentation.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cronos.duo.domain.model.LinkSessionStatus
import es.cronos.duo.domain.usecase.GenerateQrCodeUseCase
import es.cronos.duo.domain.usecase.GetUserUseCase
import es.cronos.duo.domain.usecase.GetLinkSessionStatusUseCase
import es.cronos.duo.domain.usecase.LinkPartnerUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PairingViewModel(
    private val generateQrCodeUseCase: GenerateQrCodeUseCase,
    private val linkPartnerUseCase: LinkPartnerUseCase,
    private val getLinkSessionStatusUseCase: GetLinkSessionStatusUseCase,
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PairingState())
    val state: StateFlow<PairingState> = _state.asStateFlow()
    private var statusPollingJob: Job? = null

    fun onGenerateQrClick() {
        viewModelScope.launch {
            runCatching { generateQrCodeUseCase() }
                .onSuccess { linkSession ->
                    startSessionStatusPolling(linkSession.sessionId)
                    _state.update {
                        it.copy(
                            linkCode = linkSession.linkCode,
                            sessionId = linkSession.sessionId,
                            showQrCode = true,
                            isPollingStatus = true,
                            errorMessage = null
                        )
                    }
                }
                .onFailure {
                    _state.update { state ->
                        state.copy(errorMessage = "No se pudo crear la sesión de vinculación")
                    }
                }
        }
    }

    fun onDismissQr() {
        stopSessionStatusPolling()
        _state.update { it.copy(showQrCode = false, isPollingStatus = false, linkCode = null, sessionId = null) }
    }

    fun onCodeScanned(code: String) {
        viewModelScope.launch {
            val linked = linkPartnerUseCase(code)
            if (!linked) {
                _state.update { state ->
                    state.copy(errorMessage = "No se pudo confirmar la sesión de vinculación")
                }
                return@launch
            }
            syncPairedUserState()
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        stopSessionStatusPolling()
        super.onCleared()
    }

    private fun startSessionStatusPolling(sessionId: String) {
        stopSessionStatusPolling()
        statusPollingJob = viewModelScope.launch {
            while (true) {
                val status = runCatching { getLinkSessionStatusUseCase(sessionId) }.getOrNull()
                when (status) {
                    LinkSessionStatus.CONFIRMED -> {
                        syncPairedUserState(
                            showQrCode = false,
                            isPollingStatus = false
                        )
                        break
                    }
                    LinkSessionStatus.EXPIRED -> {
                        _state.update {
                            it.copy(
                                showQrCode = false,
                                isPollingStatus = false,
                                errorMessage = "La sesión de vinculación expiró"
                            )
                        }
                        break
                    }
                    LinkSessionStatus.PENDING -> Unit
                    null -> {
                        _state.update {
                            it.copy(errorMessage = "No se pudo consultar la sesión de vinculación")
                        }
                    }
                }
                delay(2000L)
            }
        }
    }

    private fun stopSessionStatusPolling() {
        statusPollingJob?.cancel()
        statusPollingJob = null
    }

    private suspend fun syncPairedUserState(
        showQrCode: Boolean = _state.value.showQrCode,
        isPollingStatus: Boolean = _state.value.isPollingStatus
    ) {
        val refreshedUser = runCatching { getUserUseCase() }.getOrNull()
        val isPaired = !refreshedUser?.partnerId.isNullOrBlank()

        _state.update { state ->
            state.copy(
                isPaired = isPaired,
                showQrCode = showQrCode,
                isPollingStatus = isPollingStatus,
                errorMessage = if (isPaired) {
                    null
                } else {
                    "La vinculación se confirmó, pero no se pudo sincronizar el usuario actualizado"
                }
            )
        }
    }
}

package es.cronos.duo.presentation.pairing

data class PairingState(
    val linkCode: String? = null,
    val sessionId: String? = null,
    val showQrCode: Boolean = false,
    val isPaired: Boolean = false,
    val isPollingStatus: Boolean = false,
    val errorMessage: String? = null
)

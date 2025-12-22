package es.cronos.duo.presentation.pairing

data class PairingState(
    val uniqueCode: String? = null,
    val showQrCode: Boolean = false,
    val isPaired: Boolean = false
)
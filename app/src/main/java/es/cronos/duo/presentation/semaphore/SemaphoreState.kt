package es.cronos.duo.presentation.semaphore

import es.cronos.duo.domain.model.SemaphoreStatus

data class SemaphoreState(
    val userStatus: SemaphoreStatus = SemaphoreStatus.AVAILABLE,
    val partnerStatus: SemaphoreStatus? = null, // Ahora empieza como null para distinguir la carga inicial
    val isPaired: Boolean = true,
    val userStatusExpiration: Long? = null, // Expiración del estado del usuario
    val userStatusDuration: Long? = null, // Duración seleccionada original (ms)
    val partnerStatusExpiration: Long? = null // Expiración del estado del partner
)
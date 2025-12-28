package es.cronos.duo.presentation.semaphore

import es.cronos.duo.domain.model.SemaphoreStatus

data class SemaphoreState(
    val userStatus: SemaphoreStatus = SemaphoreStatus.AVAILABLE,
    val partnerStatus: SemaphoreStatus = SemaphoreStatus.AVAILABLE,
    val isPaired: Boolean = true // Asumimos que al entrar en esta pantalla se está emparejado
)
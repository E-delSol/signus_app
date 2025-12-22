package es.cronos.duo.domain.model

enum class SemaphoreStatus {
    AVAILABLE,  // Verde
    BUSY;       // Rojo

    fun next(): SemaphoreStatus {
        return when (this) {
            AVAILABLE -> BUSY
            BUSY -> AVAILABLE
        }
    }
}
package es.cronos.duo.presentation.semaphore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import es.cronos.duo.data.repository.UserRepositoryImpl
import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.usecase.GetPartnerStatusUseCase
import es.cronos.duo.domain.usecase.ObserveUserUseCase
import es.cronos.duo.domain.usecase.UpdateUserStatusUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SemaphoreViewModel : ViewModel() {

    private val _state = MutableStateFlow(SemaphoreState())
    val state: StateFlow<SemaphoreState> = _state.asStateFlow()

    // In a real app, these would be injected by a DI framework like Hilt
    private val userRepository: UserRepository = UserRepositoryImpl()
    private val observeUserUseCase = ObserveUserUseCase(userRepository)
    private val updateUserStatusUseCase = UpdateUserStatusUseCase(userRepository)
    private val getPartnerStatusUseCase = GetPartnerStatusUseCase(userRepository)

    // Variable temporal para guardar la duración seleccionada antes de aplicarla
    private var pendingDurationMillis: Long? = null

    init {
        viewModelScope.launch {
            // Guardar el token de notificaciones para recibir avisos offline
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                userRepository.saveFcmToken(token)
            } catch (e: Exception) {
                // Si falla obtener el token (ej. sin google play services), continuamos sin notificaciones
                e.printStackTrace()
            }

            // Use collectLatest to automatically cancel and restart the partner status listener
            // whenever the user's partnerId changes.
            observeUserUseCase().collectLatest { user ->
                // Update our own status from the user object
                user?.status?.let { userStatus ->
                    _state.update { it.copy(
                        userStatus = userStatus,
                        userStatusExpiration = user.statusExpiration, // Update expiration
                        userStatusDuration = user.statusDuration // Update original duration
                    ) }
                    
                    // Check if timer expired and revert if needed (local check)
                    checkExpiration(userStatus, user.statusExpiration)
                }

                val partnerId = user?.partnerId
                
                if (partnerId.isNullOrBlank()) {
                    // If there's no partner, set default status AND update isPaired to false
                    _state.update { it.copy(
                        partnerStatus = SemaphoreStatus.BUSY,
                        isPaired = false
                    ) }
                } else {
                    // If there is a partner, set isPaired to true and collect status
                    _state.update { it.copy(isPaired = true) }
                    
                    // This inner collect will be cancelled and restarted if partnerId changes.
                    getPartnerStatusUseCase(partnerId).collect { partnerUser ->
                        _state.update { it.copy(
                            partnerStatus = partnerUser?.status,
                            partnerStatusExpiration = partnerUser?.statusExpiration
                        ) }
                    }
                }
            }
        }
    }

    private fun checkExpiration(currentStatus: SemaphoreStatus, expiration: Long?) {
        if (expiration != null && expiration > 0) {
            val remaining = expiration - System.currentTimeMillis()
            if (remaining <= 0) {
                // Time expired! Revert status.
                revertStatus(currentStatus)
            } else {
                // Schedule a check/revert when time is up
                viewModelScope.launch {
                    delay(remaining)
                    // Check again in case it changed in the meantime
                    val currentState = _state.value
                    if (currentState.userStatusExpiration == expiration) {
                         revertStatus(currentState.userStatus)
                    }
                }
            }
        }
    }

    private fun revertStatus(currentStatus: SemaphoreStatus) {
        val newStatus = currentStatus.next()
        _state.update { it.copy(
            userStatus = newStatus, 
            userStatusExpiration = null,
            userStatusDuration = null
        ) }
        viewModelScope.launch {
            updateUserStatusUseCase(newStatus, null, null)
        }
    }

    fun onUserStatusClick() {
        val newStatus = _state.value.userStatus.next()
        
        // Check if there is a pending timer
        if (pendingDurationMillis != null && pendingDurationMillis!! > 0) {
            val durationMillis = pendingDurationMillis!!
            val expirationTimestamp = System.currentTimeMillis() + durationMillis
            
            // Clear pending duration
            pendingDurationMillis = null
            
            // Update UI immediately with timed status
            _state.update { it.copy(
                userStatus = newStatus,
                userStatusExpiration = expirationTimestamp,
                userStatusDuration = durationMillis
            ) }

            // Launch a coroutine to update the status in Firestore with expiration
            viewModelScope.launch {
                updateUserStatusUseCase(newStatus, expirationTimestamp, durationMillis)
            }
             // Start monitoring expiration locally
            checkExpiration(newStatus, expirationTimestamp)
            
        } else {
            // Normal toggle without timer
            _state.update { it.copy(
                userStatus = newStatus, 
                userStatusExpiration = null,
                userStatusDuration = null
            ) }

            // Launch a coroutine to update the status in Firestore
            viewModelScope.launch {
                updateUserStatusUseCase(newStatus, null, null)
            }
        }
    }

    fun onTimerSelected(hours: Int, minutes: Int) {
        val durationMillis = (hours * 60 * 60 * 1000L) + (minutes * 60 * 1000L)
        if (durationMillis > 0) {
            pendingDurationMillis = durationMillis
            // We can optionally update the UI to show the "armed" timer in the button
            // But we need to store it in the state if we want the View to see it.
            // Let's add a temporary field to state or just assume pendingDurationMillis is enough logic
            // The view needs to know "selected time".
            // Let's repurpose userStatusDuration or add a new field 'selectedDuration'.
            // Since userStatusDuration is for the ACTIVE timer, we should probably add 'pendingDuration' to state.
            _state.update { it.copy(userStatusDuration = durationMillis) } 
            // We update userStatusDuration temporarily so the button shows it, 
            // BUT we don't set userStatusExpiration yet.
            // Wait, if we set userStatusDuration but NOT expiration, the button will show "1h 0m" (correct),
            // and the countdown won't start because expiration is null (correct).
            // So this reuse of 'userStatusDuration' works for the "show selected time" requirement
            // IF we ensure countdown logic depends on 'userStatusExpiration'.
        }
    }
    
    // Deprecated/Modified: This was the old direct action
    fun onUserStatusTimed(hours: Int, minutes: Int) {
        onTimerSelected(hours, minutes)
    }
}
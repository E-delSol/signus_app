package es.cronos.duo.presentation.semaphore

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SemaphoreViewModel : ViewModel() {

    private val _state = MutableStateFlow(SemaphoreState())
    val state: StateFlow<SemaphoreState> = _state.asStateFlow()

    fun onUserStatusClick() {
        _state.update { currentState ->
            currentState.copy(userStatus = currentState.userStatus.next())
        }
    }
}
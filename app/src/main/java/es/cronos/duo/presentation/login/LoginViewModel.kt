package es.cronos.duo.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.usecase.LoginWithEmailUseCase
import es.cronos.duo.domain.usecase.RegisterWithEmailUseCase
import es.cronos.duo.domain.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val registerWithEmailUseCase: RegisterWithEmailUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            loginWithEmailUseCase(email, password).collect { result ->
                handleAuthResult(result)
            }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            registerWithEmailUseCase(email, password, displayName).collect { result ->
                handleAuthResult(result)
            }
        }
    }

    private fun handleAuthResult(result: Resource<User>) {
        when (result) {
            is Resource.Success -> {
                _state.value = LoginState(
                    isLoggedIn = true,
                    user = result.data
                )
            }
            is Resource.Error -> {
                _state.value = LoginState(error = result.message ?: "Error inesperado")
            }
            is Resource.Loading -> {
                _state.value = LoginState(isLoading = true)
            }
        }
    }
}

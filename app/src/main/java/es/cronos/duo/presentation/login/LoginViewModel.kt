package es.cronos.duo.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cronos.duo.domain.usecase.LoginWithEmailUseCase
import es.cronos.duo.domain.usecase.RegisterWithEmailUseCase
import es.cronos.duo.domain.usecase.SignInWithGoogleUseCase
import es.cronos.duo.domain.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val registerWithEmailUseCase: RegisterWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            loginWithEmailUseCase(email, password).onEach { result ->
                handleAuthResult(result)
            }.launchIn(this)
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            registerWithEmailUseCase(email, password).onEach { result ->
                handleAuthResult(result)
            }.launchIn(this)
        }
    }

    fun onGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _state.value = LoginState(isLoading = true)
            when (val result = signInWithGoogleUseCase(idToken)) {
                is Resource.Success -> {
                    _state.value = LoginState(user = result.data)
                }
                is Resource.Error -> {
                    _state.value = LoginState(error = result.message ?: "Error al iniciar sesión con Google")
                }
                is Resource.Loading -> {
                    _state.value = LoginState(isLoading = true)
                }
            }
        }
    }

    private fun handleAuthResult(result: Resource<es.cronos.duo.domain.model.User>) {
        when (result) {
            is Resource.Success -> {
                _state.value = LoginState(user = result.data)
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
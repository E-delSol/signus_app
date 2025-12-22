package es.cronos.duo.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.cronos.duo.data.repository.AuthRepositoryImpl
import es.cronos.duo.domain.usecase.LoginWithEmailUseCase
import es.cronos.duo.domain.usecase.RegisterWithEmailUseCase
import es.cronos.duo.domain.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val registerWithEmailUseCase: RegisterWithEmailUseCase
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

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = AuthRepositoryImpl()
                val loginUseCase = LoginWithEmailUseCase(repository)
                val registerUseCase = RegisterWithEmailUseCase(repository)
                return LoginViewModel(loginUseCase, registerUseCase) as T
            }
        }
    }
}
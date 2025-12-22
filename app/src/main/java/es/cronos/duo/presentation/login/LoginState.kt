package es.cronos.duo.presentation.login

import es.cronos.duo.domain.model.User

data class LoginState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)
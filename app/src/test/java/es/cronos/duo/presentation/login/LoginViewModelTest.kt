package es.cronos.duo.presentation.login

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.usecase.LoginWithEmailUseCase
import es.cronos.duo.domain.usecase.RegisterWithEmailUseCase
import es.cronos.duo.domain.usecase.SignInWithGoogleUseCase
import es.cronos.duo.domain.util.Resource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val loginWithEmailUseCase: LoginWithEmailUseCase = mockk()
    private val registerWithEmailUseCase: RegisterWithEmailUseCase = mockk()
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase = mockk()

    private lateinit var viewModel: LoginViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(
            loginWithEmailUseCase,
            registerWithEmailUseCase,
            signInWithGoogleUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given login success when login is called then state is logged in with user`() = runTest {
        val email = "test@example.com"
        val password = "password"
        val user = User(id = "backend_user", email = email)
        every { loginWithEmailUseCase(email, password) } returns flowOf(
            Resource.Loading(),
            Resource.Success(user)
        )

        viewModel.login(email, password)
        advanceUntilIdle()

        viewModel.state.value shouldBeEqualTo LoginState(
            isLoggedIn = true,
            user = user
        )
    }

    @Test
    fun `given login error when login is called then state contains error`() = runTest {
        val email = "test@example.com"
        val password = "wrong_password"
        val errorMessage = "Credenciales inválidas"
        every { loginWithEmailUseCase(email, password) } returns flowOf(
            Resource.Loading(),
            Resource.Error(errorMessage)
        )

        viewModel.login(email, password)
        advanceUntilIdle()

        viewModel.state.value shouldBeEqualTo LoginState(error = errorMessage)
    }

    @Test
    fun `given register success when register is called then state is logged in with user`() = runTest {
        val email = "new@example.com"
        val password = "password123"
        val displayName = "New User"
        val user = User(id = "backend_user", email = email, displayName = displayName)
        every { registerWithEmailUseCase(email, password, displayName) } returns flowOf(
            Resource.Loading(),
            Resource.Success(user)
        )

        viewModel.register(email, password, displayName)
        advanceUntilIdle()

        viewModel.state.value shouldBeEqualTo LoginState(
            isLoggedIn = true,
            user = user
        )
    }

    @Test
    fun `given register error when register is called then state contains error`() = runTest {
        val email = "new@example.com"
        val password = "password123"
        val displayName = "New User"
        val errorMessage = "Conflicto al registrarse"
        every { registerWithEmailUseCase(email, password, displayName) } returns flowOf(
            Resource.Loading(),
            Resource.Error(errorMessage)
        )

        viewModel.register(email, password, displayName)
        advanceUntilIdle()

        viewModel.state.value shouldBeEqualTo LoginState(error = errorMessage)
    }

    @Test
    fun `given google sign in success when onGoogleSignIn is called then state is logged in with user`() = runTest {
        val idToken = "google_token"
        val user = User(id = "3", email = "google@example.com")
        coEvery { signInWithGoogleUseCase(idToken) } returns Resource.Success(user)

        viewModel.onGoogleSignIn(idToken)
        advanceUntilIdle()

        viewModel.state.value shouldBeEqualTo LoginState(
            isLoggedIn = true,
            user = user
        )
    }

    @Test
    fun `given google sign in error when onGoogleSignIn is called then state contains error`() = runTest {
        val idToken = "google_token"
        val errorMessage = "Google sign in failed"
        coEvery { signInWithGoogleUseCase(idToken) } returns Resource.Error(errorMessage)

        viewModel.onGoogleSignIn(idToken)
        advanceUntilIdle()

        viewModel.state.value shouldBeEqualTo LoginState(error = errorMessage)
    }
}

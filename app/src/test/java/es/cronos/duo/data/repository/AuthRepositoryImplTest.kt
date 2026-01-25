package es.cronos.duo.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import es.cronos.duo.domain.util.Resource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private val firebaseAuth: FirebaseAuth = mockk(relaxed = true)
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setup() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        authRepository = AuthRepositoryImpl(firebaseAuth)
    }

    @Test
    fun `when loginWithEmail succeeds then emit Loading and Success`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password"
        val mockUser: FirebaseUser = mockk {
            every { uid } returns "uid123"
            every { getEmail() } returns email
            every { displayName } returns "Test User"
        }
        val mockAuthResult: AuthResult = mockk {
            every { user } returns mockUser
        }
        val mockTask: Task<AuthResult> = mockk()
        
        every { firebaseAuth.signInWithEmailAndPassword(email, password) } returns mockTask
        coEvery { mockTask.await() } returns mockAuthResult

        // When
        val results = authRepository.loginWithEmail(email, password).toList()

        // Then
        results[0] shouldBeInstanceOf Resource.Loading::class
        results[1] shouldBeInstanceOf Resource.Success::class
        (results[1] as Resource.Success).data?.id shouldBeEqualTo "uid123"
    }

    @Test
    fun `when loginWithEmail fails then emit Loading and Error`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password"
        val errorMessage = "Invalid credentials"
        val mockTask: Task<AuthResult> = mockk()
        
        every { firebaseAuth.signInWithEmailAndPassword(email, password) } returns mockTask
        coEvery { mockTask.await() } throws Exception(errorMessage)

        // When
        val results = authRepository.loginWithEmail(email, password).toList()

        // Then
        results[0] shouldBeInstanceOf Resource.Loading::class
        results[1] shouldBeInstanceOf Resource.Error::class
        (results[1] as Resource.Error).message shouldBeEqualTo errorMessage
    }
}

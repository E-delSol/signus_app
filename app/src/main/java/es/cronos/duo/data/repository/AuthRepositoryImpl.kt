package es.cronos.duo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val currentUser: User?
        get() = try {
            firebaseAuth.currentUser?.let {
                User(it.uid, it.email, it.displayName)
            }
        } catch (e: Exception) {
            // Log error or return null if Firebase is not initialized properly
            null
        }

    override fun loginWithEmail(email: String, password: String): Flow<Resource<User>> = callbackFlow {
        try {
            trySend(Resource.Loading())
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                trySend(Resource.Success(User(user.uid, user.email, user.displayName)))
            } else {
                trySend(Resource.Error("Error desconocido al iniciar sesión"))
            }
        } catch (e: Exception) {
            trySend(Resource.Error(e.localizedMessage ?: "Error al iniciar sesión"))
        }
        close()
    }

    override fun registerWithEmail(email: String, password: String): Flow<Resource<User>> = callbackFlow {
        try {
            trySend(Resource.Loading())
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                trySend(Resource.Success(User(user.uid, user.email, user.displayName)))
            } else {
                trySend(Resource.Error("Error desconocido al registrarse"))
            }
        } catch (e: Exception) {
            trySend(Resource.Error(e.localizedMessage ?: "Error al registrarse"))
        }
        close()
    }

    override suspend fun signInWithGoogle(idToken: String): Resource<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                Resource.Success(User(user.uid, user.email, user.displayName))
            } else {
                Resource.Error("Error desconocido con Google Sign-In")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error con Google Sign-In")
        }
    }

    override fun signOut() {
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            // Manejar error de signOut si es necesario
        }
    }
}
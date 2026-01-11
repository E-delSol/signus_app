package es.cronos.duo.domain.repository

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: User?
    fun loginWithEmail(email: String, password: String): Flow<Resource<User>>
    fun registerWithEmail(email: String, password: String): Flow<Resource<User>>
    suspend fun signInWithGoogle(idToken: String): Resource<User>
    fun signOut()
}
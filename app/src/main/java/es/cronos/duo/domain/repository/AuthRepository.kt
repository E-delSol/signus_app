package es.cronos.duo.domain.repository

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.util.Resource

interface AuthRepository {
    val currentUser: User?
    suspend fun login(email: String, password: String): Resource<User>
    suspend fun register(email: String, password: String, displayName: String): Resource<User>
    fun isLoggedIn(): Boolean
    fun logout()
    suspend fun testProtectedEndpoint(): Resource<String>
    suspend fun signInWithGoogle(idToken: String): Resource<User>
    fun signOut()
}

package es.cronos.duo.domain.repository

import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(): User?
    fun observeUser(): Flow<User?>
    suspend fun updateUserStatus(status: SemaphoreStatus)
    fun getPartnerStatus(partnerId: String): Flow<SemaphoreStatus>
}
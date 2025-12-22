package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class GetPartnerStatusUseCase(private val userRepository: UserRepository) {
    operator fun invoke(partnerId: String): Flow<SemaphoreStatus> = userRepository.getPartnerStatus(partnerId)
}
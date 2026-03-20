package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.repository.HealthRepository

class GetHealthUseCase(
    private val healthRepository: HealthRepository
) {
    suspend operator fun invoke(): String = healthRepository.getHealth()
}

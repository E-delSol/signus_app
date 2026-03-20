package es.cronos.duo.data.repository

import es.cronos.duo.data.remote.HealthApi
import es.cronos.duo.domain.repository.HealthRepository

class HealthRepositoryImpl(
    private val healthApi: HealthApi
) : HealthRepository {
    override suspend fun getHealth(): String {
        return healthApi.getHealth()
    }
}

package es.cronos.duo.domain.repository

interface HealthRepository {
    suspend fun getHealth(): String
}

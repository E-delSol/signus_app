package es.cronos.duo.data.remote

import es.cronos.duo.data.remote.dto.AuthResponseDto
import es.cronos.duo.data.remote.dto.LoginRequestDto
import es.cronos.duo.data.remote.dto.RegisterRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class AuthApi(
    private val httpClient: HttpClient
) {
    suspend fun login(email: String, password: String): AuthResponseDto {
        return httpClient.post("/auth/login") {
            expectSuccess = true
            setBody(LoginRequestDto(email = email, password = password))
        }.body()
    }

    suspend fun register(email: String, password: String, displayName: String): AuthResponseDto {
        return httpClient.post("/auth/register") {
            expectSuccess = true
            setBody(
                RegisterRequestDto(
                    email = email,
                    password = password,
                    displayName = displayName
                )
            )
        }.body()
    }

    suspend fun refreshSession(refreshToken: String): AuthResponseDto {
        return httpClient.post("/auth/refresh") {
            expectSuccess = true
            setBody(mapOf("refreshToken" to refreshToken))
        }.body()
    }

    suspend fun testProtected(): String {
        return httpClient.get("/auth/test") {
            expectSuccess = true
        }.body()
    }

    suspend fun bootstrap(): String {
        return httpClient.get("/auth/bootstrap") {
            expectSuccess = true
        }.body()
    }
}

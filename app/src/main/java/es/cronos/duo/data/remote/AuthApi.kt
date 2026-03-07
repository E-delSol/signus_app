package es.cronos.duo.data.remote

import es.cronos.duo.data.remote.dto.AuthResponseDto
import es.cronos.duo.data.remote.dto.LoginRequestDto
import es.cronos.duo.data.remote.dto.RegisterRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class AuthApi(
    private val httpClient: HttpClient
) {
    suspend fun login(email: String, password: String): AuthResponseDto {
        return httpClient.post("/auth/login") {
            setBody(LoginRequestDto(email = email, password = password))
        }.body()
    }

    suspend fun register(email: String, password: String, displayName: String): AuthResponseDto {
        return httpClient.post("/auth/register") {
            setBody(
                RegisterRequestDto(
                    email = email,
                    password = password,
                    displayName = displayName
                )
            )
        }.body()
    }

    suspend fun testProtected(): String {
        return httpClient.get("/auth/test").body()
    }
}

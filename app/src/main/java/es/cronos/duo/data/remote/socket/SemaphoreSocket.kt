package es.cronos.duo.data.remote.socket

import es.cronos.duo.data.local.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.takeFrom
import io.ktor.websocket.close
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

sealed interface SemaphoreSocketEvent
sealed interface PartnerSocketEvent : SemaphoreSocketEvent

data class PartnerStatusChangedSocketEvent(
    val partnerId: String?,
    val status: String?,
    val statusExpiration: Long?,
    val statusDuration: Long?
) : PartnerSocketEvent

data class SelfStatusChangedSocketEvent(
    val userId: String?,
    val status: String?,
    val statusExpiration: Long?,
    val statusDuration: Long?
) : SemaphoreSocketEvent

data class PartnerUnlinkedSocketEvent(
    val partnerId: String?
) : PartnerSocketEvent

class SemaphoreSocket(
    private val httpClient: HttpClient,
    private val tokenStore: TokenStore
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeEvents(): Flow<SemaphoreSocketEvent> = flow {
        val token = tokenStore.getToken()
            ?: throw IllegalStateException("Missing access token for websocket connection")

        val session = httpClient.webSocketSession {
            url.takeFrom("ws://10.0.2.2:8080/ws")
            url.parameters.append("token", token)
        }

        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    parseEvent(frame.readText())?.let { emit(it) }
                }
            }
        } finally {
            session.close()
        }
    }

    fun observePartnerEvents(): Flow<PartnerSocketEvent> {
        return observeEvents().mapNotNull { it as? PartnerSocketEvent }
    }

    fun observePartnerStatusChangedEvents(): Flow<PartnerStatusChangedSocketEvent> {
        return observeEvents().mapNotNull { it as? PartnerStatusChangedSocketEvent }
    }

    fun observeSelfStatusChangedEvents(): Flow<SelfStatusChangedSocketEvent> {
        return observeEvents().mapNotNull { it as? SelfStatusChangedSocketEvent }
    }

    private fun parseEvent(payload: String): SemaphoreSocketEvent? {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return null

        val type = root["type"]?.asStringOrNull()
        return when (type) {
            "PARTNER_STATUS_CHANGED" -> PartnerStatusChangedSocketEvent(
                partnerId = root["senderId"].asStringOrNull() ?: root["partnerId"].asStringOrNull(),
                status = root["status"].asStringOrNull(),
                statusExpiration = root["statusExpiration"].asLongOrNull(),
                statusDuration = root["statusDuration"].asLongOrNull()
            )
            "SELF_STATUS_CHANGED" -> SelfStatusChangedSocketEvent(
                userId = root["userId"].asStringOrNull() ?: root["senderId"].asStringOrNull(),
                status = root["status"].asStringOrNull(),
                statusExpiration = root["statusExpiration"].asLongOrNull(),
                statusDuration = root["statusDuration"].asLongOrNull()
            )
            "PARTNER_UNLINKED" -> PartnerUnlinkedSocketEvent(
                partnerId = root["senderId"].asStringOrNull()
                    ?: root["partnerId"].asStringOrNull()
                    ?: root["userId"].asStringOrNull()
            )
            else -> null
        }
    }

    private fun kotlinx.serialization.json.JsonElement?.asStringOrNull(): String? {
        return (this as? JsonPrimitive)?.contentOrNull
    }

    private fun kotlinx.serialization.json.JsonElement?.asLongOrNull(): Long? {
        return (this as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
    }
}

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

sealed interface PartnerSocketEvent

data class SemaphoreStatusChangedSocketEvent(
    val partnerId: String?,
    val status: String?,
    val statusExpiration: Long?,
    val statusDuration: Long?
) : PartnerSocketEvent

data class PartnerUnlinkedSocketEvent(
    val partnerId: String?
) : PartnerSocketEvent

class SemaphoreSocket(
    private val httpClient: HttpClient,
    private val tokenStore: TokenStore
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observePartnerEvents(): Flow<PartnerSocketEvent> = flow {
        val token = tokenStore.getToken()
            ?: throw IllegalStateException("Missing access token for websocket connection")

        val session = httpClient.webSocketSession {
            url.takeFrom("ws://10.0.2.2:8080/ws")
            url.parameters.append("token", token)
        }

        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    parsePartnerEvent(frame.readText())?.let { emit(it) }
                }
            }
        } finally {
            session.close()
        }
    }

    fun observePartnerStatusChangedEvents(): Flow<SemaphoreStatusChangedSocketEvent> {
        return observePartnerEvents().mapNotNull { it as? SemaphoreStatusChangedSocketEvent }
    }

    private fun parsePartnerEvent(payload: String): PartnerSocketEvent? {
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return null

        val type = root["type"]?.asStringOrNull()
        val isPartnerStatusChanged = type == "PARTNER_STATUS_CHANGED"
        val isPartnerUnlinked = type == "PARTNER_UNLINKED"
        val isSemaphoreStatusChanged = root.containsKey("userId") && root.containsKey("timestamp")

        if (!isPartnerStatusChanged && !isPartnerUnlinked && !isSemaphoreStatusChanged) {
            return null
        }

        if (isPartnerStatusChanged) {
            return SemaphoreStatusChangedSocketEvent(
                partnerId = root["senderId"].asStringOrNull() ?: root["partnerId"].asStringOrNull(),
                status = root["status"].asStringOrNull(),
                statusExpiration = null,
                statusDuration = null
            )
        }

        if (isPartnerUnlinked) {
            return PartnerUnlinkedSocketEvent(
                partnerId = root["senderId"].asStringOrNull()
                    ?: root["partnerId"].asStringOrNull()
                    ?: root["userId"].asStringOrNull()
            )
        }

        return SemaphoreStatusChangedSocketEvent(
            partnerId = root["userId"].asStringOrNull(),
            status = root["status"].asStringOrNull(),
            statusExpiration = root["statusExpiration"].asLongOrNull(),
            statusDuration = null
        )
    }

    private fun kotlinx.serialization.json.JsonElement?.asStringOrNull(): String? {
        return (this as? JsonPrimitive)?.contentOrNull
    }

    private fun kotlinx.serialization.json.JsonElement?.asLongOrNull(): Long? {
        return (this as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
    }
}

package es.cronos.duo.data.network

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class VersionEnforcementInterceptor(
    private val state: VersionEnforcementState
) {
    fun intercept(status: HttpStatusCode, body: String) {
        when {
            status == HttpStatusCode.UpgradeRequired -> handle426(body)
            status == HttpStatusCode.BadRequest -> handle400(body)
        }
    }

    private fun handle426(body: String) {
        val error = parseErrorMessage(body)
        when {
            error?.contains(VersionEnforcementContract.ERROR_UNSUPPORTED_VERSION_KEYWORD) == true -> {
                val minVersion = extractMinimumVersion(body) ?: VersionEnforcementContract.UNKNOWN_VERSION
                state.setUnsupportedVersion(minVersion)
            }
            error?.contains(VersionEnforcementContract.ERROR_MISSING_HEADER_KEYWORD) == true ->
                state.setMissingHeader()
        }
    }

    private fun handle400(body: String) {
        val error = parseErrorMessage(body)
        if (error?.contains(VersionEnforcementContract.ERROR_INVALID_FORMAT_KEYWORD) == true) {
            state.setInvalidFormat()
        }
    }

    private fun parseErrorMessage(json: String): String? {
        return runCatching {
            Json.parseToJsonElement(json)
                .jsonObject[VersionEnforcementContract.JSON_ERROR_KEY]
                ?.jsonPrimitive?.content
        }.getOrNull()
    }

    private fun extractMinimumVersion(text: String): String? {
        val match = Regex(VersionEnforcementContract.MIN_VERSION_REGEX).find(text)
        return match?.groupValues?.getOrNull(1)
    }
}

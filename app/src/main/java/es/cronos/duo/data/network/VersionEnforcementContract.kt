package es.cronos.duo.data.network

object VersionEnforcementContract {
    const val VERSION_HEADER = "X-App-Version"

    const val ERROR_UNSUPPORTED_VERSION_KEYWORD = "no longer supported"
    const val ERROR_MISSING_HEADER_KEYWORD = "header"
    const val ERROR_INVALID_FORMAT_KEYWORD = "Invalid app version format"
    const val MIN_VERSION_REGEX = "Minimum supported version is ([\\d.]+)"
    const val UNKNOWN_VERSION = "unknown"
    const val JSON_ERROR_KEY = "error"
}

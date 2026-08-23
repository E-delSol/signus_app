package es.cronos.duo.data.network

sealed interface VersionStatus {
    data object Supported : VersionStatus
    data class UnsupportedVersion(val minimumVersion: String) : VersionStatus
    data object MissingHeader : VersionStatus
    data object InvalidFormat : VersionStatus
}

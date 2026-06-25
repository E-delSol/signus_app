package es.cronos.duo.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VersionEnforcementState {
    private val _status = MutableStateFlow<VersionStatus>(VersionStatus.Supported)
    val status: StateFlow<VersionStatus> = _status.asStateFlow()

    fun setUnsupportedVersion(minimumVersion: String) {
        _status.value = VersionStatus.UnsupportedVersion(minimumVersion)
    }

    fun setMissingHeader() {
        _status.value = VersionStatus.MissingHeader
    }

    fun setInvalidFormat() {
        _status.value = VersionStatus.InvalidFormat
    }

    fun reset() {
        _status.value = VersionStatus.Supported
    }
}

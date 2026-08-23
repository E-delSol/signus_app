package es.cronos.duo.data.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class VersionEnforcementStateTest {

    private val state = VersionEnforcementState()

    @Test
    fun `initial state is Supported`() = runTest {
        state.status.first() shouldBeEqualTo VersionStatus.Supported
    }

    @Test
    fun `setUnsupportedVersion transitions to UnsupportedVersion`() = runTest {
        state.setUnsupportedVersion("2.0.0")
        state.status.first() shouldBeEqualTo VersionStatus.UnsupportedVersion("2.0.0")
    }

    @Test
    fun `setMissingHeader transitions to MissingHeader`() = runTest {
        state.setMissingHeader()
        state.status.first() shouldBeEqualTo VersionStatus.MissingHeader
    }

    @Test
    fun `setInvalidFormat transitions to InvalidFormat`() = runTest {
        state.setInvalidFormat()
        state.status.first() shouldBeEqualTo VersionStatus.InvalidFormat
    }

    @Test
    fun `reset returns to Supported`() = runTest {
        state.setUnsupportedVersion("2.0.0")
        state.reset()
        state.status.first() shouldBeEqualTo VersionStatus.Supported
    }
}

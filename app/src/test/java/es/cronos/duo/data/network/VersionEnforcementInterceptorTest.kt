package es.cronos.duo.data.network

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class VersionEnforcementInterceptorTest {

    private val state = VersionEnforcementState()
    private val interceptor = VersionEnforcementInterceptor(state)

    @Test
    fun `200 response does not change state`() = runTest {
        interceptor.intercept(HttpStatusCode.OK, """{"id":"1"}""")
        state.status.first() shouldBeEqualTo VersionStatus.Supported
    }

    @Test
    fun `426 unsupported version extracts minimum version`() = runTest {
        interceptor.intercept(
            HttpStatusCode.UpgradeRequired,
            """{"error": "App version is no longer supported. Minimum supported version is 2.0.0"}"""
        )
        state.status.first() shouldBeEqualTo VersionStatus.UnsupportedVersion("2.0.0")
    }

    @Test
    fun `426 missing header sets MissingHeader`() = runTest {
        interceptor.intercept(
            HttpStatusCode.UpgradeRequired,
            """{"error": "App version header X-App-Version is required"}"""
        )
        state.status.first() shouldBeEqualTo VersionStatus.MissingHeader
    }

    @Test
    fun `400 invalid format sets InvalidFormat`() = runTest {
        interceptor.intercept(
            HttpStatusCode.BadRequest,
            """{"error": "Invalid app version format"}"""
        )
        state.status.first() shouldBeEqualTo VersionStatus.InvalidFormat
    }

    @Test
    fun `unknown 426 body leaves state unchanged`() = runTest {
        interceptor.intercept(
            HttpStatusCode.UpgradeRequired,
            """{"error": "Something else"}"""
        )
        state.status.first() shouldBeEqualTo VersionStatus.Supported
    }

    @Test
    fun `426 non json body leaves state unchanged`() = runTest {
        interceptor.intercept(
            HttpStatusCode.UpgradeRequired,
            "plain text error"
        )
        state.status.first() shouldBeEqualTo VersionStatus.Supported
    }

    @Test
    fun `400 non json body leaves state unchanged`() = runTest {
        interceptor.intercept(
            HttpStatusCode.BadRequest,
            "plain text error"
        )
        state.status.first() shouldBeEqualTo VersionStatus.Supported
    }

    @Test
    fun `426 json without error field leaves state unchanged`() = runTest {
        interceptor.intercept(
            HttpStatusCode.UpgradeRequired,
            """{"message": "Upgrade required"}"""
        )
        state.status.first() shouldBeEqualTo VersionStatus.Supported
    }

    @Test
    fun `400 json without error field leaves state unchanged`() = runTest {
        interceptor.intercept(
            HttpStatusCode.BadRequest,
            """{"message": "Bad request"}"""
        )
        state.status.first() shouldBeEqualTo VersionStatus.Supported
    }
}

package es.cronos.duo.data.network

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AppVersionProviderTest {

    @Test
    fun `versionName returns value from BuildConfig`() {
        val provider = AppVersionProvider()
        val version = provider.versionName
        version.isNullOrBlank() shouldBeEqualTo false
    }
}

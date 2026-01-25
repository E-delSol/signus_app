package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.repository.QrCodeRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class LinkPartnerUseCaseTest {

    private val repository: QrCodeRepository = mockk()
    private val linkPartnerUseCase = LinkPartnerUseCase(repository)

    @Test
    fun `given valid code when link partner is called then return true`() = runTest {
        // Given
        val code = "valid_code"
        coEvery { repository.linkPartner(code) } returns true

        // When
        val result = linkPartnerUseCase(code)

        // Then
        result shouldBeEqualTo true
    }

    @Test
    fun `given invalid code when link partner is called then return false`() = runTest {
        // Given
        val code = "invalid_code"
        coEvery { repository.linkPartner(code) } returns false

        // When
        val result = linkPartnerUseCase(code)

        // Then
        result shouldBeEqualTo false
    }
}

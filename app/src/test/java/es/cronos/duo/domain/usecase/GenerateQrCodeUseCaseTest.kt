package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.LinkSession
import es.cronos.duo.domain.repository.QrCodeRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class GenerateQrCodeUseCaseTest {

    private val repository: QrCodeRepository = mockk()
    private val generateQrCodeUseCase = GenerateQrCodeUseCase(repository)

    @Test
    fun `given repository returns a link session when invoke is called then return that link session`() = runTest {
        // Given
        val expectedSession = LinkSession(
            sessionId = "session-123",
            linkCode = "ABC123",
            expiresAt = "2026-03-06T12:00:00Z"
        )
        coEvery { repository.generateUniqueCode() } returns expectedSession

        // When
        val result = generateQrCodeUseCase()

        // Then
        result shouldBeEqualTo expectedSession
    }
}

package es.cronos.duo.domain.usecase

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
    fun `given repository returns a code when invoke is called then return that code`() = runTest {
        // Given
        val expectedCode = "unique_qr_code_123"
        coEvery { repository.generateUniqueCode() } returns expectedCode

        // When
        val result = generateQrCodeUseCase()

        // Then
        result shouldBeEqualTo expectedCode
    }
}

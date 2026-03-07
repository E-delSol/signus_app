package es.cronos.duo.data.repository

import es.cronos.duo.data.remote.PairingApi
import es.cronos.duo.data.remote.dto.CreatePairingSessionResponseDto
import es.cronos.duo.data.remote.dto.GetLinkSessionStatusResponseDto
import es.cronos.duo.domain.model.LinkSession
import es.cronos.duo.domain.model.LinkSessionStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class QrCodeRepositoryImplTest {

    private val pairingApi: PairingApi = mockk()
    private val repository = QrCodeRepositoryImpl(pairingApi)

    @Test
    fun `generateUniqueCode returns sessionId and linkCode from backend`() = runTest {
        val sessionId = "session-123"
        val linkCode = "ABC123"
        coEvery { pairingApi.createPairingSession() } returns CreatePairingSessionResponseDto(
            sessionId = sessionId,
            linkCode = linkCode,
            expiresAt = "2026-03-06T12:00:00Z"
        )

        val result = repository.generateUniqueCode()

        result shouldBeEqualTo LinkSession(
            sessionId = sessionId,
            linkCode = linkCode,
            expiresAt = "2026-03-06T12:00:00Z"
        )
    }

    @Test
    fun `linkPartner returns true when backend confirmation succeeds`() = runTest {
        val linkCode = "ABC123"
        coEvery { pairingApi.confirmPairingSession(linkCode) } returns Unit

        val result = repository.linkPartner(linkCode)

        result shouldBeEqualTo true
    }

    @Test
    fun `getLinkSessionStatus maps CONFIRMED response`() = runTest {
        val sessionId = "session-123"
        coEvery { pairingApi.getLinkSessionStatus(sessionId) } returns GetLinkSessionStatusResponseDto(
            sessionId = sessionId,
            status = "CONFIRMED"
        )

        val result = repository.getLinkSessionStatus(sessionId)

        result shouldBeEqualTo LinkSessionStatus.CONFIRMED
    }
}

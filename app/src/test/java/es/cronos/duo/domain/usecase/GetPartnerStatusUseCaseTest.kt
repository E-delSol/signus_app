package es.cronos.duo.domain.usecase

import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class GetPartnerStatusUseCaseTest {

    private val userRepository: UserRepository = mockk()
    private val getPartnerStatusUseCase = GetPartnerStatusUseCase(userRepository)

    @Test
    fun `given a partnerId when invoke is called then return partner status flow`() = runTest {
        // Given
        val partnerId = "partner123"
        val expectedPartner = User(id = partnerId, email = "partner@example.com")
        every { userRepository.getPartnerStatus(partnerId) } returns flowOf(expectedPartner)

        // When
        val result = getPartnerStatusUseCase(partnerId).toList()

        // Then
        result[0] shouldBeEqualTo expectedPartner
    }

    @Test
    fun `given a non-existent partnerId when invoke is called then return null flow`() = runTest {
        // Given
        val partnerId = "nonexistent"
        every { userRepository.getPartnerStatus(partnerId) } returns flowOf(null)

        // When
        val result = getPartnerStatusUseCase(partnerId).toList()

        // Then
        result[0] shouldBeEqualTo null
    }
}

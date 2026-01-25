package es.cronos.duo.presentation.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import es.cronos.duo.domain.model.User
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.ui.theme.DuoTheme
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)

    @Before
    fun setup() {
        loadKoinModules(module {
            single { authRepository }
            single { userRepository }
        })
    }

    @Test
    fun givenNoUser_whenAppStarts_thenNavigateToWelcome() {
        // Given
        every { authRepository.currentUser } returns null

        // When
        composeTestRule.setContent {
            DuoTheme {
                AppNavigation(startDestination = Splash)
            }
        }

        // Then
        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()
    }

    @Test
    fun givenLoggedInUserWithoutPartner_whenAppStarts_thenNavigateToPairing() {
        // Given
        val mockUser = User(id = "123", email = "test@test.com")
        every { authRepository.currentUser } returns mockUser
        coEvery { userRepository.getUser() } returns mockUser.copy(partnerId = null)

        // When
        composeTestRule.setContent {
            DuoTheme {
                AppNavigation(startDestination = Splash)
            }
        }

        // Then
        composeTestRule.onNodeWithTag("pairing_screen").assertIsDisplayed()
    }

    @Test
    fun givenPairedUser_whenAppStarts_thenNavigateToSemaphore() {
        // Given
        val mockUser = User(id = "123", partnerId = "partner456")
        every { authRepository.currentUser } returns mockUser
        coEvery { userRepository.getUser() } returns mockUser

        // When
        composeTestRule.setContent {
            DuoTheme {
                AppNavigation(startDestination = Splash)
            }
        }

        // Then
        composeTestRule.onNodeWithTag("semaphore_screen").assertIsDisplayed()
    }

    @Test
    fun givenWelcomeScreen_whenClickStart_thenNavigateToLogin() {
        // Given
        every { authRepository.currentUser } returns null
        composeTestRule.setContent {
            DuoTheme {
                AppNavigation(startDestination = Welcome)
            }
        }

        // When
        composeTestRule.onNodeWithTag("start_button").performClick()

        // Then
        composeTestRule.onNodeWithTag("login_screen").assertIsDisplayed()
    }

    @Test
    fun givenLoginScreen_whenClickEmail_thenShowEmailForm() {
        // Given
        composeTestRule.setContent {
            DuoTheme {
                AppNavigation(startDestination = Login)
            }
        }

        // When
        composeTestRule.onNodeWithTag("email_login_button").performClick()

        // Then
        composeTestRule.onNodeWithTag("email_login_form").assertIsDisplayed()
    }

    @Test
    fun givenSemaphoreScreen_whenClickSettings_thenNavigateToSettings() {
        // Given
        val mockUser = User(id = "123", partnerId = "partner456")
        every { authRepository.currentUser } returns mockUser
        composeTestRule.setContent {
            DuoTheme {
                AppNavigation(startDestination = Semaphore)
            }
        }

        // When
        composeTestRule.onNodeWithTag("settings_button").performClick()

        // Then
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    }

    @Test
    fun givenSettingsScreen_whenClickLogout_thenNavigateToWelcome() {
        // Given
        composeTestRule.setContent {
            DuoTheme {
                AppNavigation(startDestination = Settings)
            }
        }

        // When
        composeTestRule.onNodeWithTag("logout_button").performClick()

        // Then
        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()
    }

    @Test
    fun givenSettingsScreen_whenUnlinkPartner_thenNavigateToPairing() {
        // Given
        composeTestRule.setContent {
            DuoTheme {
                AppNavigation(startDestination = Settings)
            }
        }

        // When
        composeTestRule.onNodeWithTag("unlink_partner_button").performClick()
        composeTestRule.onNodeWithTag("unlink_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_unlink_button").performClick()

        // Then
        composeTestRule.onNodeWithTag("pairing_screen").assertIsDisplayed()
    }
}

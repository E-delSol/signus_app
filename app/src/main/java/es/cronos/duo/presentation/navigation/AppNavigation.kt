package es.cronos.duo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.cronos.duo.presentation.pairing.PairingScreen
import es.cronos.duo.presentation.semaphore.SemaphoreScreen
import es.cronos.duo.presentation.settings.SettingsScreen
import es.cronos.duo.presentation.welcome.WelcomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") { WelcomeScreen(navController) }
        composable("pairing") { PairingScreen(navController) }
        composable("semaphore") { SemaphoreScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}
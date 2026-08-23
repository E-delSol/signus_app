package es.cronos.duo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.compose.rememberNavController
import es.cronos.duo.presentation.forceupdate.ForceUpdateScreen
import es.cronos.duo.presentation.login.LoginScreen
import es.cronos.duo.presentation.pairing.PairingScreen
import es.cronos.duo.presentation.semaphore.SemaphoreScreen
import es.cronos.duo.presentation.settings.SettingsScreen
import es.cronos.duo.presentation.splash.SplashScreen
import es.cronos.duo.presentation.welcome.WelcomeScreen

@Composable
fun AppNavigation(startDestination: Any, pendingDeepLink: String? = null) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Splash> { SplashScreen(navController, pendingDeepLink) }
        composable<Welcome> { WelcomeScreen(navController) }
        composable<Login> { LoginScreen(navController) }
        composable<Pairing>(
            deepLinks = listOf(navDeepLink { uriPattern = "signus://pairing" })
        ) { PairingScreen(navController) }
        composable<Semaphore>(
            deepLinks = listOf(navDeepLink { uriPattern = "signus://semaphore" })
        ) { SemaphoreScreen(navController) }
        composable<Settings>(
            deepLinks = listOf(navDeepLink { uriPattern = "signus://settings" })
        ) { SettingsScreen(navController) }
        composable<ForceUpdate> { ForceUpdateScreen() }
    }
}

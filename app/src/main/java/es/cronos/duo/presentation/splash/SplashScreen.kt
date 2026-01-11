package es.cronos.duo.presentation.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.presentation.navigation.Pairing
import es.cronos.duo.presentation.navigation.Semaphore
import es.cronos.duo.presentation.navigation.Splash
import es.cronos.duo.presentation.navigation.Welcome
import org.koin.compose.koinInject

@Composable
fun SplashScreen(
    navController: NavController,
    authRepository: AuthRepository = koinInject(),
    userRepository: UserRepository = koinInject()
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }

    LaunchedEffect(Unit) {
        val currentUser = authRepository.currentUser

        if (currentUser == null) {
            navController.navigate(Welcome) {
                popUpTo(Splash) { inclusive = true }
            }
        } else {
            // Check if user has a partner
            try {
                val user = userRepository.getUser()
                
                if (user?.partnerId != null && user.partnerId.isNotBlank()) {
                    navController.navigate(Semaphore) {
                        popUpTo(Splash) { inclusive = true }
                    }
                } else {
                    navController.navigate(Pairing) {
                        popUpTo(Splash) { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                // In case of error, assume login is valid but maybe network error
                navController.navigate(Pairing) {
                    popUpTo(Splash) { inclusive = true }
                }
            }
        }
    }
}
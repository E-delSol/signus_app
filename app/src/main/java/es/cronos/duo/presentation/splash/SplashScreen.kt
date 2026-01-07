package es.cronos.duo.presentation.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import es.cronos.duo.data.repository.UserRepositoryImpl
import es.cronos.duo.presentation.navigation.Pairing
import es.cronos.duo.presentation.navigation.Semaphore
import es.cronos.duo.presentation.navigation.Splash
import es.cronos.duo.presentation.navigation.Welcome

@Composable
fun SplashScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }

    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            navController.navigate(Welcome) {
                popUpTo(Splash) { inclusive = true }
            }
        } else {
            // Check if user has a partner
            try {
                val userRepository = UserRepositoryImpl()
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
                // In case of error, go to welcome or stay here? 
                // Let's assume session is invalid or network error, maybe go to welcome or retry.
                // For safety, let's go to pairing if user is logged in but check failed? 
                // Or welcome if we suspect auth issue.
                // Let's go to pairing, worst case they can't pair.
                navController.navigate(Pairing) {
                    popUpTo(Splash) { inclusive = true }
                }
            }
        }
    }
}
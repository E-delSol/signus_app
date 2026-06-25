package es.cronos.duo.presentation.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import es.cronos.duo.data.network.VersionEnforcementState
import es.cronos.duo.data.network.VersionStatus
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.usecase.AppStartupUseCase
import es.cronos.duo.presentation.navigation.ForceUpdate
import es.cronos.duo.presentation.navigation.Pairing
import es.cronos.duo.presentation.navigation.Semaphore
import es.cronos.duo.presentation.navigation.Splash
import es.cronos.duo.presentation.navigation.Welcome
import org.koin.compose.koinInject

@Composable
fun SplashScreen(
    navController: NavController,
    appStartupUseCase: AppStartupUseCase = koinInject(),
    userRepository: UserRepository = koinInject(),
    versionEnforcementState: VersionEnforcementState = koinInject()
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }

    LaunchedEffect(Unit) {
        val startupResult = runCatching { appStartupUseCase() }

        if (versionEnforcementState.status.value is VersionStatus.UnsupportedVersion) {
            navController.navigate(ForceUpdate) {
                popUpTo(Splash) { inclusive = true }
            }
            return@LaunchedEffect
        }

        val result = startupResult.getOrNull() ?: return@LaunchedEffect

        when (result) {
            is AppStartupUseCase.Result.Authenticated -> {
                try {
                    userRepository.syncFcmToken()
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
                } catch (_: Exception) {
                    if (versionEnforcementState.status.value is VersionStatus.UnsupportedVersion) {
                        navController.navigate(ForceUpdate) {
                            popUpTo(Splash) { inclusive = true }
                        }
                        return@LaunchedEffect
                    }
                    navController.navigate(Pairing) {
                        popUpTo(Splash) { inclusive = true }
                    }
                }
            }
            is AppStartupUseCase.Result.Guest -> {
                navController.navigate(Welcome) {
                    popUpTo(Splash) { inclusive = true }
                }
            }
        }
    }
}

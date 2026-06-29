package es.cronos.duo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import es.cronos.duo.presentation.navigation.AppNavigation
import es.cronos.duo.presentation.navigation.Splash
import es.cronos.duo.ui.theme.DuoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract deep link target before Navigation auto-resolves it.
        // Clear intent.data so unauthenticated users don't bypass auth.
        val pendingDeepLink = intent.data?.lastPathSegment.also {
            intent.data = null
        }

        val startDestination = Splash

        enableEdgeToEdge()
        setContent {
            DuoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavigation(
                            startDestination = startDestination,
                            pendingDeepLink = pendingDeepLink
                        )
                    }
                }
            }
        }
    }
}
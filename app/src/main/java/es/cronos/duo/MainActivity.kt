package es.cronos.duo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import es.cronos.duo.presentation.navigation.AppNavigation
import es.cronos.duo.ui.theme.DuoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Lógica de comprobación de sesión
        // Si hay un usuario autenticado, vamos directo al semáforo.
        // Si falla la inicialización de Firebase (ej. falta json), asumimos no logueado ("welcome")
        val startDestination = try {
            if (FirebaseAuth.getInstance().currentUser != null) {
                "semaphore"
            } else {
                "welcome"
            }
        } catch (e: Exception) {
            "welcome"
        }

        enableEdgeToEdge()
        setContent {
            DuoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
    }
}
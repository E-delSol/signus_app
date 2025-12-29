package es.cronos.duo.presentation.semaphore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.cronos.duo.R
import es.cronos.duo.components.PartnerLinkButton
import es.cronos.duo.components.PrivacyIndicator
import es.cronos.duo.components.TitleApp
import es.cronos.duo.components.TrafficLight
import es.cronos.duo.domain.model.SemaphoreStatus

@Composable
fun SemaphoreScreen(
    navController: NavController,
    viewModel: SemaphoreViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showUnlinkedDialog by remember { mutableStateOf(false) }
    
    // Obtenemos el ciclo de vida actual para saber si la pantalla está visible
    val lifecycleOwner = LocalLifecycleOwner.current

    // Detectar si se pierde el vínculo
    LaunchedEffect(state.isPaired) {
        if (!state.isPaired) {
            // Solo mostramos el diálogo si esta pantalla está ACTIVA (RESUMED).
            // Si el usuario está en Ajustes desvinculando, esta pantalla estará en PAUSED/STOPPED,
            // por lo que no mostrará el diálogo (lo cual es correcto, ya que Ajustes maneja la navegación).
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                showUnlinkedDialog = true
            }
        }
    }

    // Popup de aviso de desvinculación
    if (showUnlinkedDialog) {
        AlertDialog(
            onDismissRequest = {
                showUnlinkedDialog = false
                navController.navigate("pairing") {
                    popUpTo("semaphore") { inclusive = true }
                }
            },
            title = { Text(stringResource(R.string.dialog_partner_unlinked_title)) },
            text = { Text(stringResource(R.string.dialog_partner_unlinked_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlinkedDialog = false
                        navController.navigate("pairing") {
                            popUpTo("semaphore") { inclusive = true }
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_understood))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            TitleApp()
            Spacer(modifier = Modifier.weight(1f))

            // --- 1. Semáforo Central (Solo Partner) ---
            TrafficLight(
                partnerStatus = state.partnerStatus
            )

            Spacer(modifier = Modifier.height(48.dp))

            // --- 2. Texto Explicativo ---
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.trafic_light_message),
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- 3. Botón de Estado del Usuario ---
            val isAvailable = state.userStatus == SemaphoreStatus.AVAILABLE
            
            PartnerLinkButton(
                title = if (isAvailable) stringResource(R.string.status_available_title) else stringResource(R.string.status_busy_title),
                description = if (isAvailable) stringResource(R.string.status_available_desc) else stringResource(R.string.status_busy_desc),
                icon = if (isAvailable) Icons.Filled.CheckCircle else Icons.Filled.DoNotDisturb,
                containerColor = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFF44336),
                contentColor = Color.White,
                iconBackgroundColor = Color.White.copy(alpha = 0.2f),
                titleColor = Color.White,
                descriptionColor = Color.White.copy(alpha = 0.8f),
                onClick = { viewModel.onUserStatusClick() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Indicador de seguridad
            PrivacyIndicator()

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Botón discreto de Ajustes
        IconButton(
            onClick = { navController.navigate("settings") },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SemaphoreScreenPreview() {
    SemaphoreScreen(navController = NavController(LocalContext.current))
}
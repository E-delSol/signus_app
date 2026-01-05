package es.cronos.duo.presentation.semaphore

import android.media.RingtoneManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.cronos.duo.R
import es.cronos.duo.components.PartnerLinkButton
import es.cronos.duo.components.PrivacyIndicator
import es.cronos.duo.components.TimerPickerDialog
import es.cronos.duo.components.TitleApp
import es.cronos.duo.components.TrafficLight
import es.cronos.duo.domain.model.SemaphoreStatus
import java.util.concurrent.TimeUnit
import java.util.Locale

@Composable
fun SemaphoreScreen(
    navController: NavController,
    viewModel: SemaphoreViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showUnlinkedDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    
    // Obtenemos el ciclo de vida actual para saber si la pantalla está visible
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // Estado para controlar el sonido y evitar que suene al iniciar la app
    var previousPartnerStatus by remember { mutableStateOf<SemaphoreStatus?>(null) }

    // Detectar cambios en el estado del partner para reproducir sonido
    LaunchedEffect(state.partnerStatus) {
        // Solo reproducimos si no es la primera carga (previous no es null) y el estado ha cambiado
        if (previousPartnerStatus != null && previousPartnerStatus != state.partnerStatus) {
            try {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(context, notification)
                r.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Actualizamos el estado previo para la siguiente comparación
        previousPartnerStatus = state.partnerStatus
    }

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

    // Popup del temporizador
    if (showTimerDialog) {
        TimerPickerDialog(
            onDismissRequest = { showTimerDialog = false },
            onConfirm = { hours, minutes ->
                showTimerDialog = false
                viewModel.onTimerSelected(hours, minutes)
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
            // Usamos un Box para controlar el ancho y la proporción
            Box(
                modifier = Modifier
                    .fillMaxWidth() // Ocupa todo el ancho (igual que los botones)
                    .aspectRatio(1f), // Mantiene forma cuadrada (círculo inscrito)
                contentAlignment = Alignment.Center
            ) {
                // Pasamos un valor por defecto (BUSY) si es null mientras carga
                TrafficLight(
                    partnerStatus = state.partnerStatus ?: SemaphoreStatus.BUSY,
                    partnerStatusExpiration = state.partnerStatusExpiration,
                    modifier = Modifier.fillMaxSize() // El semáforo llena este contenedor cuadrado
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // --- 3. Botón de Estado del Usuario ---
            val isAvailable = state.userStatus == SemaphoreStatus.AVAILABLE
            val buttonColor = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFF44336)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // PartnerLinkButton modificado para usar trailingContent
                PartnerLinkButton(
                    title = if (isAvailable) stringResource(R.string.status_available_title) else stringResource(R.string.status_busy_title),
                    description = if (isAvailable) stringResource(R.string.status_available_desc) else stringResource(R.string.status_busy_desc),
                    icon = if (isAvailable) Icons.Filled.CheckCircle else Icons.Filled.DoNotDisturb,
                    containerColor = buttonColor,
                    contentColor = Color.White,
                    iconBackgroundColor = Color.White.copy(alpha = 0.2f),
                    titleColor = Color.White,
                    descriptionColor = Color.White.copy(alpha = 0.8f),
                    onClick = { viewModel.onUserStatusClick() },
                    modifier = Modifier.fillMaxWidth(),
                    trailingContent = {
                        // Solo mostramos la cuenta atrás SI ya hay expiración (timer iniciado)
                         state.userStatusExpiration?.let { expiration ->
                            CountdownTimer(
                                targetTimeMillis = expiration,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White // Texto blanco sobre fondo de botón
                            )
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Botón "Temporizar estado"
                Button(
                    onClick = { showTimerDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.timer_button_text))
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Tiempo seleccionado (armado) dentro del botón
                        // Solo si NO ha comenzado la cuenta atrás (expiration == null) pero tenemos duration (seleccionado)
                        if (state.userStatusExpiration == null) {
                            state.userStatusDuration?.let { duration ->
                                val hours = TimeUnit.MILLISECONDS.toHours(duration)
                                val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
                                
                                val durationText = if (hours > 0) {
                                    String.format(Locale.getDefault(), "%dh %02dm", hours, minutes)
                                } else {
                                    String.format(Locale.getDefault(), "%02dm", minutes)
                                }
                                
                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(3f))

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

@Composable
fun CountdownTimer(
    targetTimeMillis: Long,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.titleMedium
) {
    var timeLeft by remember(targetTimeMillis) { mutableLongStateOf(targetTimeMillis - System.currentTimeMillis()) }

    LaunchedEffect(targetTimeMillis) {
        while (true) {
            val remaining = targetTimeMillis - System.currentTimeMillis()
            if (remaining <= 0) {
                timeLeft = 0
                break
            }
            timeLeft = remaining
            kotlinx.coroutines.delay(1000)
        }
    }

    if (timeLeft > 0) {
        val hours = TimeUnit.MILLISECONDS.toHours(timeLeft)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60
        
        Text(
            text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds),
            style = style,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SemaphoreScreenPreview() {
    SemaphoreScreen(navController = NavController(LocalContext.current))
}
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.cronos.duo.R
import es.cronos.duo.components.PartnerLinkButton
import es.cronos.duo.components.PrivacyIndicator
import es.cronos.duo.components.TimerPickerDialog
import es.cronos.duo.components.TitleApp
import es.cronos.duo.components.TrafficLight
import es.cronos.duo.domain.model.SemaphoreStatus
import es.cronos.duo.presentation.navigation.Pairing
import es.cronos.duo.presentation.navigation.Semaphore
import es.cronos.duo.presentation.navigation.Settings
import java.util.concurrent.TimeUnit
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SemaphoreScreen(
    navController: NavController,
    viewModel: SemaphoreViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    var showTimerDialog by remember { mutableStateOf(false) }
    var showUnlinkedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SemaphoreViewModel.UiEvent.PlayNotificationSound -> {
                    try {
                        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        RingtoneManager.getRingtone(context, notification).play()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                is SemaphoreViewModel.UiEvent.ShowTimerDialog -> showTimerDialog = true
                is SemaphoreViewModel.UiEvent.HideTimerDialog -> showTimerDialog = false
                is SemaphoreViewModel.UiEvent.ShowUnlinkedDialog -> showUnlinkedDialog = true
            }
        }
    }

    if (showUnlinkedDialog) {
        AlertDialog(
            onDismissRequest = { /* Controlled by user action */ },
            title = { Text(stringResource(R.string.dialog_partner_unlinked_title)) },
            text = { Text(stringResource(R.string.dialog_partner_unlinked_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlinkedDialog = false 
                        navController.navigate(Pairing) { popUpTo(Semaphore) { inclusive = true } }
                    }
                ) {
                    Text(stringResource(R.string.action_understood))
                }
            }
        )
    }

    if (showTimerDialog) {
        TimerPickerDialog(
            onDismissRequest = { viewModel.onDismissTimerDialog() },
            onConfirm = { hours, minutes -> viewModel.onTimerSelected(hours, minutes) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .testTag("semaphore_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            TitleApp()
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                TrafficLight(
                    partnerStatus = state.partnerStatus ?: SemaphoreStatus.BUSY,
                    partnerStatusExpiration = state.partnerStatusExpiration,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            val isAvailable = state.userStatus == SemaphoreStatus.AVAILABLE
            val buttonColor = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFF44336)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                         state.userStatusExpiration?.let { expiration ->
                            CountdownTimer(
                                targetTimeMillis = expiration,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { viewModel.onShowTimerDialog() },
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

            PrivacyIndicator()

            Spacer(modifier = Modifier.height(48.dp))
        }

        IconButton(
            onClick = { navController.navigate(Settings) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp)
                .testTag("settings_button")
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

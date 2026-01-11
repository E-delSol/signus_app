package es.cronos.duo.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.cronos.duo.R
import es.cronos.duo.domain.model.SemaphoreStatus
import java.util.concurrent.TimeUnit
import java.util.Locale

@Composable
fun TrafficLight(
    partnerStatus: SemaphoreStatus,
    partnerStatusExpiration: Long? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Partner Disk (Solo mostramos el estado del partner)
        StatusDisk(
            status = partnerStatus,
            expiration = partnerStatusExpiration,
            isInteractive = false,
            onClick = {},
            message = stringResource(
                if (partnerStatus == SemaphoreStatus.AVAILABLE) {
                    R.string.semaphore_partner_message_available
                } else {
                    R.string.semaphore_partner_message_busi
                }
            )
        )
    }
}

@Composable
private fun StatusDisk(
    status: SemaphoreStatus,
    expiration: Long?,
    isInteractive: Boolean,
    onClick: () -> Unit,
    message: String
) {
    val targetColor = when (status) {
        SemaphoreStatus.AVAILABLE -> Color.Green
        SemaphoreStatus.BUSY -> Color.Red
    }

    val messageColor = when (status) {
        SemaphoreStatus.AVAILABLE -> Color.Black
        SemaphoreStatus.BUSY -> Color.White
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "ColorAnimation"
    )

    // Usamos Box en lugar de Card para evitar sombras poligonales (octógonos)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.80f) // Ocupa todo el ancho del padre
            .padding(4.dp) // Un pequeño margen para el borde
            // Forma circular (al ser cuadrada la caja por aspectRatio en el padre, se ve círculo)
            .clip(CircleShape)
            // Fondo del color principal (animado)
            .background(if (isInteractive) animatedColor else animatedColor.copy(alpha = 0.6f))
            // Borde opcional para definir mejor el círculo
            .border(18.dp, animatedColor.copy(alpha = 0.8f), CircleShape)
            .then(
                if (isInteractive) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Círculo interno para efecto de "borde" o profundidad
        Box(
            modifier = Modifier
                .fillMaxSize(0.80f), // Ocupa el 85% del tamaño para dejar el borde visual
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Contenedor del texto
                Text(
                    text = message,
                    color = messageColor,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Cuenta atrás si existe expiración
                if (expiration != null) {
                    CountdownTimerInternal(
                        targetTimeMillis = expiration,
                        color = messageColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownTimerInternal(
    targetTimeMillis: Long,
    color: Color
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
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
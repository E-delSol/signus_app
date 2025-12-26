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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun TrafficLight(
    partnerStatus: SemaphoreStatus
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Partner Disk (Solo mostramos el estado del partner)
        StatusDisk(
            status = partnerStatus,
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
            .size(172.dp)
            // Forma circular
            .clip(CircleShape)
            // Fondo del color principal (animado)
            .background(if (isInteractive) animatedColor else animatedColor.copy(alpha = 0.6f))
            // Borde opcional para definir mejor el círculo
            .border(4.dp, animatedColor.copy(alpha = 0.8f), CircleShape)
            .then(
                if (isInteractive) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Círculo interno para efecto de "borde" o profundidad
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(
                    color = Color.White.copy(alpha = 0.15f) // Brillo interior
                ),
            contentAlignment = Alignment.Center
        ) {
            // Contenedor del texto
            Text(
                text = message,
                color = messageColor,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
package es.cronos.duo.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.cronos.duo.R
import es.cronos.duo.domain.model.SemaphoreStatus

@Composable
fun TrafficLight(
    userStatus: SemaphoreStatus,
    partnerStatus: SemaphoreStatus,
    onUserStatusClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Partner Disk (Top)
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

        Spacer(modifier = Modifier.height(16.dp))

        // User Disk (Bottom)
        StatusDisk(
            status = userStatus,
            isInteractive = true,
            onClick = onUserStatusClick,
            message = stringResource(R.string.semaphore_user_message)
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

    // Mejora de contraste: Texto negro sobre verde, blanco sobre rojo
    val messageColor = when (status) {
        SemaphoreStatus.AVAILABLE -> Color.Black
        SemaphoreStatus.BUSY -> Color.White
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "ColorAnimation"
    )

    // Usamos Card que maneja mejor las sombras circulares y elevación en diferentes dispositivos
    Card(
        modifier = Modifier
            .size(172.dp)
            .then(
                if (isInteractive) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInteractive) animatedColor else animatedColor.copy(alpha = 0.6f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Efecto visual (brillo sutil interno)
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            )
            
            // Contenedor del texto
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .padding(8.dp), // Padding interno para que el texto no toque bordes
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message,
                    color = messageColor,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
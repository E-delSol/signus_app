package es.cronos.duo.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import es.cronos.duo.presentation.util.QrCodeGenerator
import kotlinx.coroutines.delay

@Composable
fun QrCodeDialog(code: String, onDismiss: () -> Unit) {
    val qrBitmap = remember(code) {
        QrCodeGenerator.generateQrBitmap(code)
    }

    // 5 minutos de validez (300 segundos)
    var timeLeft by remember { mutableStateOf(300) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        // Cerrar al expirar
        onDismiss()
    }

    // Formato MM:SS
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Tu Código de Emparejamiento",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.White)
                            .padding(8.dp)
                    )
                } else {
                    Text("Error generando QR")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mostrar el código en texto también por si acaso
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Expira en: $timeFormatted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        }
    }
}
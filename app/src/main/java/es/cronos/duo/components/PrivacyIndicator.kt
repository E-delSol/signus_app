package es.cronos.duo.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.cronos.duo.R

@Composable
fun PrivacyIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f), // Opacidad
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.privacy_policy), // "Tus datos nunca salen de..."
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
            fontWeight = FontWeight.Normal
        )
    }

    Spacer(modifier = Modifier.height(12.dp)) // gap-3

    // Enlace de Política de Privacidad
    Text(
        text = stringResource(R.string.privacy_policy_link), // "Política de Privacidad"
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.secondary, // Un gris un poco más oscuro
        modifier = Modifier.clickable { /* Abrir enlace */ }
    )
}

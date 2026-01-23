package es.cronos.duo.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.cronos.duo.R

@Composable
fun PrivacyIndicator(onPrivacyClick: (() -> Unit)? = null) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        PrivacyDialog(onDismiss = { showDialog = false })
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.privacy_policy),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Enlace de Política de Privacidad con área de clic aumentada
        Text(
            text = stringResource(R.string.privacy_policy_link),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable { 
                    if (onPrivacyClick != null) {
                        onPrivacyClick()
                    } else {
                        showDialog = true
                    }
                }
                .padding(8.dp)
        )
    }
}

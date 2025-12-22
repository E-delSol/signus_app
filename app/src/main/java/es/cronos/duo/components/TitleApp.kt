package es.cronos.duo.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import es.cronos.duo.R

@Composable
fun TitleApp() {
    Text(
        text = stringResource(R.string.app_name), // "Duo: Su Santuario Privado."
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground, // text-slate-900 / text-white
        textAlign = TextAlign.Center
    )
}
package es.cronos.duo.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import es.cronos.duo.R
import es.cronos.duo.ui.theme.Manrope

@Composable
fun TitleApp() {
    Text(
        text = stringResource(R.string.app_name), // "Duo: Su Santuario Privado."
        fontFamily = Manrope,
        fontSize = 38.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = Color(0xFFE6EAF0),
        textAlign = TextAlign.Center
    )
}
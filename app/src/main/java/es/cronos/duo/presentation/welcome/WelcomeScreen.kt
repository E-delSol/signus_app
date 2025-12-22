package es.cronos.duo.presentation.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import es.cronos.duo.R
import es.cronos.duo.components.CentralImage
import es.cronos.duo.components.PartnerLinkButton
import es.cronos.duo.components.PrivacyIndicator
import es.cronos.duo.components.TitleApp

@Composable
fun WelcomeScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp) // px-6 y px-8 del HTML
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacer para empujar el contenido hacia el centro verticalmente
            Spacer(modifier = Modifier.weight(1f))

            // --- 1. Imagen Central (Placeholder) ---
            // Se puede reemplazar con una imagen real o un componente más complejo.
            val primaryColor = MaterialTheme.colorScheme.primary
            CentralImage(primaryColor)

            Spacer(modifier = Modifier.height(48.dp)) // mb-12

            // --- 2. Títulos de Texto ---
            TitleApp()

            Spacer(modifier = Modifier.height(16.dp)) // gap-4

            Text(
                text = stringResource(R.string.welcome_message), // "Comunicación sin límites,..."
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.tertiary, // text-slate-500 / text-slate-400
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp) // max-w-[280px] mx-auto
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- 3. Botón y Privacidad ---
            PartnerLinkButton(
                title = stringResource(R.string.go_to_pairing_title),
                description = stringResource(R.string.go_to_pairing_description),
                icon = Icons.Filled.AddLink,
                onClick = { navController.navigate("login") }
            )

            Spacer(modifier = Modifier.height(32.dp)) // mt-8

            // Indicador de seguridad
            PrivacyIndicator()

            Spacer(modifier = Modifier.height(48.dp)) // pb-12
        }
    }

}


@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen(navController = NavController(LocalContext.current))
}
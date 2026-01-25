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
import androidx.compose.ui.platform.testTag
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
import es.cronos.duo.presentation.navigation.Login

@Composable
fun WelcomeScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .testTag("welcome_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            val primaryColor = MaterialTheme.colorScheme.primary
            CentralImage(primaryColor)

            Spacer(modifier = Modifier.height(48.dp))

            TitleApp()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.welcome_message),
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            PartnerLinkButton(
                title = stringResource(R.string.go_to_pairing_title),
                description = stringResource(R.string.go_to_pairing_description),
                icon = Icons.Filled.AddLink,
                onClick = { navController.navigate(Login) },
                modifier = Modifier.testTag("start_button")
            )

            Spacer(modifier = Modifier.height(32.dp))

            PrivacyIndicator()

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen(navController = NavController(LocalContext.current))
}

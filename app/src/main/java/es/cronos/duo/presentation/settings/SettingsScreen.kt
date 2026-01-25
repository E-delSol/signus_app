package es.cronos.duo.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import es.cronos.duo.presentation.navigation.Pairing
import es.cronos.duo.presentation.navigation.Semaphore
import es.cronos.duo.presentation.navigation.Welcome
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    var showUnlinkDialog by remember { mutableStateOf(false) }

    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text(text = stringResource(R.string.dialog_unlink_title)) },
            text = { Text(stringResource(R.string.dialog_unlink_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showUnlinkDialog = false
                        viewModel.onUnlinkPartner()
                        navController.navigate(Pairing) {
                            popUpTo(Semaphore) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_unlink_button")
                ) {
                    Text(stringResource(R.string.action_unlink))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            modifier = Modifier.testTag("unlink_dialog")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            TitleApp()
            Spacer(modifier = Modifier.weight(1f))

            val primaryColor = MaterialTheme.colorScheme.primary
            CentralImage(primaryColor)

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            PartnerLinkButton(
                title = stringResource(R.string.settings_unlink_title),
                description = stringResource(R.string.settings_unlink_description),
                icon = Icons.Filled.LinkOff,
                onClick = {
                    showUnlinkDialog = true
                },
                modifier = Modifier.testTag("unlink_partner_button")
            )

            Spacer(modifier = Modifier.height(24.dp))

            PartnerLinkButton(
                title = stringResource(R.string.settings_logout_title),
                description = stringResource(R.string.settings_logout_description),
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = {
                    viewModel.onLogout()
                    navController.navigate(Welcome) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.testTag("logout_button")
            )

            Spacer(modifier = Modifier.height(32.dp))

            PrivacyIndicator()

            Spacer(modifier = Modifier.height(48.dp))
        }

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp)
                .testTag("back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(navController = NavController(LocalContext.current))
}

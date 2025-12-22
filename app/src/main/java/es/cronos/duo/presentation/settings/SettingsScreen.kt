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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.cronos.duo.R
import es.cronos.duo.components.CentralImage
import es.cronos.duo.components.PartnerLinkButton
import es.cronos.duo.components.PrivacyIndicator
import es.cronos.duo.components.TitleApp

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    var showUnlinkDialog by remember { mutableStateOf(false) }

    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text(text = "Desvincular pareja") },
            text = { Text("¿Estás seguro de que quieres romper el vínculo con tu pareja? Esta acción eliminará la sesión actual.") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnlinkDialog = false
                        viewModel.onUnlinkPartner()
                        navController.navigate("pairing") {
                            // Al desvincular, volvemos a la pantalla de emparejamiento
                            // eliminando el historial hasta pairing para evitar volver atrás
                            popUpTo("semaphore") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Desvincular")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            TitleApp()
            Spacer(modifier = Modifier.weight(1f))

            // --- 1. Imagen Central ---
            val primaryColor = MaterialTheme.colorScheme.primary
            CentralImage(primaryColor)

            Spacer(modifier = Modifier.height(48.dp))

            // --- 2. Título de la pantalla ---
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- 3. Botones de Acción ---
            
            // Botón Desvincular
            PartnerLinkButton(
                title = stringResource(R.string.settings_unlink_title),
                description = stringResource(R.string.settings_unlink_description),
                icon = Icons.Filled.LinkOff,
                onClick = {
                    showUnlinkDialog = true
                },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Cerrar Sesión
            PartnerLinkButton(
                title = stringResource(R.string.settings_logout_title),
                description = stringResource(R.string.settings_logout_description),
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = {
                    viewModel.onLogout()
                    navController.navigate("welcome") {
                        // Al hacer logout, limpiamos toda la pila de navegación
                        popUpTo(0) { inclusive = true }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Indicador de seguridad
            PrivacyIndicator()

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Botón discreto "Atrás" en la esquina superior izquierda
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.tertiary // Color discreto consistente con el resto de la app
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(navController = NavController(LocalContext.current))
}
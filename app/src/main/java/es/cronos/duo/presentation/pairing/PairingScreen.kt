package es.cronos.duo.presentation.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import es.cronos.duo.components.ManualLinkDialog
import es.cronos.duo.components.PartnerLinkButton
import es.cronos.duo.components.PrivacyIndicator
import es.cronos.duo.components.QrCodeDialog
import es.cronos.duo.components.QrCodeScanner
import es.cronos.duo.components.TitleApp

@Composable
fun PairingScreen(
    navController: NavController,
    viewModel: PairingViewModel = viewModel(factory = PairingViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    var showCamera by remember { mutableStateOf(false) }
    var showManualLinkDialog by remember { mutableStateOf(false) }

    // Navegación automática si se empareja
    LaunchedEffect(state.isPaired) {
        if (state.isPaired) {
            navController.navigate("semaphore") {
                popUpTo("pairing") { inclusive = true }
            }
        }
    }

    if (showCamera) {
        Box(modifier = Modifier.fillMaxSize()) {
            QrCodeScanner(
                onCodeScanned = { code ->
                    showCamera = false
                    viewModel.onCodeScanned(code)
                }
            )
            // Botón para cerrar cámara
            IconButton(
                onClick = { showCamera = false },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar cámara",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    } else {
        // Contenido normal de la pantalla
        if (state.showQrCode && state.uniqueCode != null) {
            QrCodeDialog(
                code = state.uniqueCode!!,
                onDismiss = { viewModel.onDismissQr() }
            )
        }

        if (showManualLinkDialog) {
            ManualLinkDialog(
                onDismiss = { showManualLinkDialog = false },
                onConfirm = { code ->
                    showManualLinkDialog = false
                    viewModel.onCodeScanned(code)
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

                // --- 2. Títulos de Texto ---
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.connect_message),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Botones
                PartnerLinkButton(
                    title = stringResource(R.string.qr_code_title),
                    description = stringResource(R.string.qr_code_description),
                    icon = Icons.Filled.QrCode2,
                    onClick = { viewModel.onGenerateQrClick() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                PartnerLinkButton(
                    title = stringResource(R.string.code_insert_title),
                    description = stringResource(R.string.code_insert_description),
                    icon = Icons.Filled.CenterFocusWeak,
                    onClick = { showCamera = true } // Activar cámara
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                // Botón Manual Link
                PartnerLinkButton(
                    title = stringResource(R.string.manual_code_title),
                    description = stringResource(R.string.manual_code_description),
                    icon = Icons.Filled.Keyboard,
                    onClick = { showManualLinkDialog = true }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Indicador de seguridad
                PrivacyIndicator()

                Spacer(modifier = Modifier.height(48.dp))
            }

            // Botón discreto de Ajustes en la esquina superior derecha
            IconButton(
                onClick = { navController.navigate("settings") },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.tertiary // Color discreto
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PairingScreenPreview() {
    PairingScreen(navController = NavController(LocalContext.current))
}
package es.cronos.duo.presentation.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // Estado local para alternar entre botones y formulario
    var showEmailForm by remember { mutableStateOf(false) }

    // Efecto para navegar si el usuario se loguea correctamente
    LaunchedEffect(state.user) {
        if (state.user != null) {
            // Navegar a Splash para que decida el destino final (Pairing o Semaphore)
            navController.navigate("splash") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // Efecto para mostrar errores
    LaunchedEffect(state.error) {
        if (state.error != null) {
            Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
        }
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
            Spacer(modifier = Modifier.weight(0.5f))

            // --- 1. Imagen Central ---
            val primaryColor = MaterialTheme.colorScheme.primary
            CentralImage(primaryColor)

            Spacer(modifier = Modifier.height(32.dp))

            // --- 2. Títulos de Texto ---
            TitleApp()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.login_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.weight(0.5f))

            // --- 3. Contenido Dinámico (Botones o Formulario) ---
            
            if (state.isLoading) {
                CircularProgressIndicator()
            } else if (showEmailForm) {
                EmailLoginForm(
                    onLoginClick = { email, password -> viewModel.login(email, password) },
                    onRegisterClick = { email, password -> viewModel.register(email, password) },
                    onBackClick = { showEmailForm = false }
                )
            } else {
                LoginSelectionButtons(
                    onGoogleClick = { /* TODO: Implementar Google Sign In */ },
                    onEmailClick = { showEmailForm = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Indicador de seguridad
            PrivacyIndicator()

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun LoginSelectionButtons(
    onGoogleClick: () -> Unit,
    onEmailClick: () -> Unit
) {
    Column {
        // Botón Google
        PartnerLinkButton(
            title = stringResource(R.string.login_google_title),
            description = stringResource(R.string.login_google_description),
            icon = Icons.Default.Email, // Idealmente icono de Google
            onClick = onGoogleClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón Email
        PartnerLinkButton(
            title = stringResource(R.string.login_email_title),
            description = stringResource(R.string.login_email_description),
            icon = Icons.Default.Email,
            onClick = onEmailClick
        )
    }
}

@Composable
fun EmailLoginForm(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: (String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        
        Text(
            text = if (isRegisterMode) stringResource(R.string.login_create_account_title) else stringResource(R.string.login_access_account_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.field_email)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.field_password)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else
                    Icons.Filled.VisibilityOff

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón principal
        PartnerLinkButton(
            title = if (isRegisterMode) stringResource(R.string.action_register) else stringResource(R.string.action_login),
            description = if (isRegisterMode) stringResource(R.string.login_create_account_desc) else stringResource(R.string.login_access_account_desc),
            icon = if (isRegisterMode) Icons.Filled.AppRegistration else Icons.AutoMirrored.Filled.Login,
            onClick = { 
                if (isRegisterMode) {
                    onRegisterClick(email, password)
                } else {
                    onLoginClick(email, password)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Botón para alternar modo
        TextButton(
            onClick = { isRegisterMode = !isRegisterMode },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (isRegisterMode) stringResource(R.string.login_toggle_to_login) else stringResource(R.string.login_toggle_to_register))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_back_home))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(navController = NavController(LocalContext.current))
}
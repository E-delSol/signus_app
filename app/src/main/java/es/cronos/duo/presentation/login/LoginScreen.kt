package es.cronos.duo.presentation.login

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import es.cronos.duo.BuildConfig
import es.cronos.duo.R
import es.cronos.duo.components.CentralImage
import es.cronos.duo.components.PartnerLinkButton
import es.cronos.duo.components.PrivacyIndicator
import es.cronos.duo.components.TitleApp
import es.cronos.duo.presentation.navigation.Login
import es.cronos.duo.presentation.navigation.Splash
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    var showEmailForm by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoggedIn, state.user) {
        if (state.isLoggedIn || state.user != null) {
            navController.navigate(Splash) {
                popUpTo(Login) { inclusive = true }
            }
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            errorDialogMessage = state.error
        }
    }

    if (errorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { errorDialogMessage = null },
            title = { Text("Error de Inicio de Sesión") },
            text = { Text(errorDialogMessage ?: "Error desconocido") },
            confirmButton = {
                TextButton(onClick = { errorDialogMessage = null }) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = { 
                Text(
                    text = stringResource(R.string.privacy_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.privacy_content),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) {
                    Text(stringResource(R.string.action_understood))
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .testTag("login_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            val primaryColor = MaterialTheme.colorScheme.primary
            CentralImage(primaryColor)

            Spacer(modifier = Modifier.height(32.dp))

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

            if (state.isLoading) {
                CircularProgressIndicator()
            } else if (showEmailForm) {
                EmailLoginForm(
                    onLoginClick = { email, password -> viewModel.login(email, password) },
                    onRegisterClick = { email, password, displayName ->
                        viewModel.register(email, password, displayName)
                    },
                    onBackClick = { showEmailForm = false }
                )
            } else {
                LoginSelectionButtons(
                    onEmailClick = { showEmailForm = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (BuildConfig.DEBUG) {
                Button(
                    onClick = { throw RuntimeException("Test Crash") },
                    modifier = Modifier.testTag("test_crash_button")
                ) {
                    Text("Test Crash")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            PrivacyIndicator(onPrivacyClick = { showPrivacyPolicy = true })

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun LoginSelectionButtons(
    onEmailClick: () -> Unit
) {
    Column(modifier = Modifier.testTag("login_selection_buttons")) {

        Spacer(modifier = Modifier.height(16.dp))

        PartnerLinkButton(
            title = stringResource(R.string.login_email_title),
            description = stringResource(R.string.login_email_description),
            icon = Icons.Default.Email,
            onClick = onEmailClick,
            modifier = Modifier.testTag("email_login_button")
        )
    }
}

@Composable
fun EmailLoginForm(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: (String, String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().testTag("email_login_form")) {
        
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
            modifier = Modifier.fillMaxWidth().testTag("email_field"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isRegisterMode) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(R.string.field_display_name)) },
                modifier = Modifier.fillMaxWidth().testTag("display_name_field"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.field_password)) },
            modifier = Modifier.fillMaxWidth().testTag("password_field"),
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

        PartnerLinkButton(
            title = if (isRegisterMode) stringResource(R.string.action_register) else stringResource(R.string.action_login),
            description = if (isRegisterMode) stringResource(R.string.login_create_account_desc) else stringResource(R.string.login_access_account_desc),
            icon = if (isRegisterMode) Icons.Filled.AppRegistration else Icons.AutoMirrored.Filled.Login,
            onClick = { 
                if (isRegisterMode) {
                    onRegisterClick(email, password, displayName)
                } else {
                    onLoginClick(email, password)
                }
            },
            modifier = Modifier.testTag("submit_login_button")
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = { isRegisterMode = !isRegisterMode },
            modifier = Modifier.align(Alignment.CenterHorizontally).testTag("toggle_register_button")
        ) {
            Text(if (isRegisterMode) stringResource(R.string.login_toggle_to_login) else stringResource(R.string.login_toggle_to_register))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.CenterHorizontally).testTag("back_to_selection_button")
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

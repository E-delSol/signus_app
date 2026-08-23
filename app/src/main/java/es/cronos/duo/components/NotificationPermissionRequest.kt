package es.cronos.duo.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import es.cronos.duo.R

@Composable
fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current

    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

    if (hasPermission) return

    var showDialog by remember { mutableStateOf(true) }
    if (!showDialog) return

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            showDialog = false
        }
    )

    AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text(context.getString(R.string.notification_permission_title)) },
        text = { Text(context.getString(R.string.notification_permission_message)) },
        confirmButton = {
            Button(onClick = {
                showDialog = false
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) {
                Text(context.getString(R.string.action_continue))
            }
        },
        dismissButton = {
            Button(onClick = { showDialog = false }) {
                Text(context.getString(R.string.action_cancel))
            }
        }
    )
}

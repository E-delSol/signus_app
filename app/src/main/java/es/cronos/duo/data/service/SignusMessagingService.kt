package es.cronos.duo.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import es.cronos.duo.MainActivity
import es.cronos.duo.R
import es.cronos.duo.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SignusMessagingService : FirebaseMessagingService(), KoinComponent {

    private val userRepository: UserRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) return
        serviceScope.launch {
            userRepository.registerOrUpdateDeviceToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val navigateTo = if (data.isNotEmpty()) data["navigateTo"] else null

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            sendNotification(it.title, it.body, navigateTo)
        }

        // Check if message contains a data payload.
        if (data.isNotEmpty()) {
            val title = data["title"]
            val body = data["body"]
            // Only send if not handled by notification payload
            if (remoteMessage.notification == null && title != null && body != null) {
                sendNotification(title, body, navigateTo)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.coroutineContext.cancel()
        super.onDestroy()
    }

    private fun sendNotification(title: String?, messageBody: String?, navigateTo: String? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            if (navigateTo != null) {
                data = Uri.parse("signus://$navigateTo")
            }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "signus_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_logo_signus_round) // Usar icono de la app
            .setContentTitle(title ?: getString(R.string.app_name))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Importante: Prioridad Alta para notificaciones heads-up
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Signus Updates",
                NotificationManager.IMPORTANCE_HIGH // Importante: Importancia Alta para el canal
            )
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}

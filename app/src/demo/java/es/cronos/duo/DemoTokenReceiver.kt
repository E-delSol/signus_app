package es.cronos.duo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import es.cronos.duo.data.local.TokenStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Broadcast receiver that injects a JWT token into TokenStore for demo mode.
 *
 * Only active in the "demo" build variant. Protected by signature permission
 * so only same-signature apps (or adb shell) can send this broadcast.
 *
 * Usage from adb:
 *   adb shell am broadcast \
 *     -a es.cronos.duo.ACTION_INJECT_DEMO_TOKEN \
 *     --es token "<jwt>" \
 *     -p es.cronos.duo
 */
class DemoTokenReceiver : BroadcastReceiver(), KoinComponent {

    private val tokenStore: TokenStore by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INJECT_DEMO_TOKEN) return

        val token = intent.getStringExtra(EXTRA_TOKEN)
        if (token.isNullOrBlank()) {
            Log.w(TAG, "Received demo broadcast with empty token, ignoring")
            return
        }

        tokenStore.restoreToken(token)
        Log.i(TAG, "Demo token injected successfully")
    }

    companion object {
        private const val TAG = "DemoTokenReceiver"
        const val ACTION_INJECT_DEMO_TOKEN = "es.cronos.duo.ACTION_INJECT_DEMO_TOKEN"
        const val EXTRA_TOKEN = "token"
    }
}

package es.cronos.duo.data.network

import es.cronos.duo.data.local.TokenStore

class ClientInstanceIdProvider(
    private val tokenStore: TokenStore
) {
    fun getId(): String = tokenStore.getOrCreateDeviceId()

    fun getLogLabel(): String = getId().takeLast(LOG_LABEL_LENGTH)

    companion object {
        private const val LOG_LABEL_LENGTH = 8
    }
}

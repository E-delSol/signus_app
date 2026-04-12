package es.cronos.duo.data.network

import es.cronos.duo.BuildConfig

class NetworkEndpointConfig {
    val httpBaseUrl: String = BuildConfig.BASE_URL.trimEnd('/')

    val webSocketUrl: String = httpBaseUrl
        .replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://") + "/ws"
}

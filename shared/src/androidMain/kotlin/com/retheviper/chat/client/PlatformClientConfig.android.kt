package com.retheviper.chat.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

actual object PlatformClientConfig {
    actual val baseUrl: String = "http://10.0.2.2:8080"
    actual val platformName: String = "Android"
}

actual fun platformHttpClient(block: HttpClientConfigBlock): HttpClient = HttpClient(Android, block)

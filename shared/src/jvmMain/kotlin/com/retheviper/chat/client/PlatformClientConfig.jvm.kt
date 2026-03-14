package com.retheviper.chat.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual object PlatformClientConfig {
    actual val baseUrl: String =
        System.getProperty("messaging.baseUrl")
            ?: System.getenv("MESSAGING_BASE_URL")
            ?: "http://localhost:8080"

    actual val platformName: String = "Desktop JVM"
}

actual fun platformHttpClient(block: HttpClientConfigBlock): HttpClient = HttpClient(CIO, block)

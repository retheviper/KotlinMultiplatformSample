package com.retheviper.chat.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual object PlatformClientConfig {
    actual val baseUrl: String = "http://localhost:8080"
    actual val platformName: String = "iOS"
}

actual fun platformHttpClient(block: HttpClientConfigBlock): HttpClient = HttpClient(Darwin, block)

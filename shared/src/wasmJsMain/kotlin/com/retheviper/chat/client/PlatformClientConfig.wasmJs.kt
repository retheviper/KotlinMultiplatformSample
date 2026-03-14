package com.retheviper.chat.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import kotlinx.browser.window

actual object PlatformClientConfig {
    actual val baseUrl: String = window.location.origin
    actual val platformName: String = "Web"
}

actual fun platformHttpClient(block: HttpClientConfigBlock): HttpClient = HttpClient(Js, block)

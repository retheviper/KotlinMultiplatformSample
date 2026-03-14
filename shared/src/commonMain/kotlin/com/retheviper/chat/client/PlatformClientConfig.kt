package com.retheviper.chat.client

import io.ktor.client.HttpClient

expect object PlatformClientConfig {
    val baseUrl: String
    val platformName: String
}

expect fun platformHttpClient(block: HttpClientConfigBlock = {}): HttpClient

typealias HttpClientConfigBlock = io.ktor.client.HttpClientConfig<*>.() -> Unit

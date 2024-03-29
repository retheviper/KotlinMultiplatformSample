package com.retheviper.bbs.infrastructure.client

import com.retheviper.bbs.getPlatform
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json


actual fun getHttpClient(): HttpClient {
    return HttpClient(Android) {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            header("X-Platform", getPlatform().nameWithVersion)
            contentType(ContentType.Application.Json)
            url {
                host = "10.0.2.2"
                port = 8080
            }
        }
    }
}
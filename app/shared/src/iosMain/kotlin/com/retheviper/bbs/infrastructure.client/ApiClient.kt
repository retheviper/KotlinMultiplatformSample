package com.retheviper.bbs.infrastructure.client

import com.retheviper.bbs.getPlatform
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json


actual fun getHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            header("X-Platform", getPlatform().nameWithVersion)
            contentType(ContentType.Application.Json)
            url {
                host = "localhost"
                port = 8080
            }
        }
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
    }
}
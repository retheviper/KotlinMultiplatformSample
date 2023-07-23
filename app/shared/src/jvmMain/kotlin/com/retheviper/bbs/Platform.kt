package com.retheviper.bbs

import com.retheviper.bbs.constant.PlatformName
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

class IOSPlatform : Platform {
    override val name: PlatformName = PlatformName.DESKTOP
    override val nameWithVersion: String = System.getProperty("os.name") + " " + System.getProperty("os.version")
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getHttpClient(): HttpClient {
    return HttpClient(Apache) {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
}
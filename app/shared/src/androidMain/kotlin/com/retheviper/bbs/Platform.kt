package com.retheviper.bbs

import com.retheviper.bbs.constant.PlatformName
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

class AndroidPlatform : Platform {
    override val name: PlatformName = PlatformName.ANDROID
    override val nameWithVersion: String = "${PlatformName.ANDROID.value} ${android.os.Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getHttpClient(): HttpClient {
    return HttpClient(Android) {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
}
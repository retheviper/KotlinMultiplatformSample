package com.retheviper.chat.plugins

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json

fun Application.configureSockets() {
    install(WebSockets) {
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
    }
}

package com.retheviper.chat.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging

fun Application.configureMonitoring() {
    install(CallLogging)
}

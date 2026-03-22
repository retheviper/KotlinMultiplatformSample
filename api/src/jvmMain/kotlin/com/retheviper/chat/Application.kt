package com.retheviper.chat

import com.retheviper.chat.plugins.configureHTTP
import com.retheviper.chat.plugins.configureMonitoring
import com.retheviper.chat.plugins.configureRouting
import com.retheviper.chat.plugins.configureSerialization
import com.retheviper.chat.plugins.configureSockets
import com.retheviper.chat.plugins.configureStatusPages
import com.retheviper.chat.plugins.configureValidation
import com.retheviper.chat.config.ApiConfig
import com.retheviper.chat.infrastructure.database.Database
import com.retheviper.chat.messaging.application.MessagingSampleDataBootstrapper
import com.retheviper.chat.messaging.presentation.configureMessagingRouting
import com.retheviper.chat.wiring.ApplicationDependencies
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    EngineMain.main(args)
}

@Suppress("unused")
fun Application.module() {
    val config = ApiConfig.from(environment.config)
    Database.initialize(config.database)

    monitor.subscribe(ApplicationStopped) {
        Database.close()
    }

    val dependencies = ApplicationDependencies.create()
    if (config.bootstrap.sampleDataEnabled) {
        runBlocking {
            MessagingSampleDataBootstrapper(
                commandService = dependencies.commandService,
                queryService = dependencies.queryService
            ).seedIfNeeded()
        }
    }

    configureMonitoring()
    configureSerialization()
    configureSockets()
    configureHTTP()
    configureValidation()
    configureStatusPages()
    configureRouting()
    configureMessagingRouting(dependencies)
}

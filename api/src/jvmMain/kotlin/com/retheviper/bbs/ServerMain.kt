package com.retheviper.bbs

import com.retheviper.bbs.framework.plugin.*
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>): Unit =
    EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureDependencyInjection()
    configurePersistent()
    configureSecurity()
    configureSerialization()
    configureMonitoring()
    configureRouting()
}

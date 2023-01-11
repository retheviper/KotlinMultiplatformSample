package com.retheviper.bbs

import com.retheviper.bbs.framework.plugins.configureMonitoring
import com.retheviper.bbs.framework.plugins.configureRouting
import com.retheviper.bbs.framework.plugins.configureSecurity
import com.retheviper.bbs.framework.plugins.configureSerialization
import io.ktor.server.application.*
import com.retheviper.bbs.framework.plugins.*
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>): Unit =
    EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureSecurity()
    configureSerialization()
    configureMonitoring()
    configureRouting()
}

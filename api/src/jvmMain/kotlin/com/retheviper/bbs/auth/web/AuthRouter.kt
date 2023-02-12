package com.retheviper.bbs.auth.web

import com.retheviper.bbs.auth.domain.model.Credential
import com.retheviper.bbs.auth.domain.service.JwtService
import com.retheviper.bbs.common.extension.getJwtConfigs
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Routing.routeAuth(application: Application) {

    val service by inject<JwtService>()

    post("/login") {
        val credential = call.receive<Credential>()

        // Check username and password
        // ...

        val jwtConfigs = application.getJwtConfigs()

        val token = service.createToken(
            jwtConfigs = jwtConfigs,
            credential = credential
        )

        call.respond(mapOf("token" to token))
    }
}
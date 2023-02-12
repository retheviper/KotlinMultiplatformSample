package com.retheviper.bbs.auth.web

import com.retheviper.bbs.auth.domain.model.Credential
import com.retheviper.bbs.auth.domain.service.JwtService
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Routing.routeAuth() {

    val service by inject<JwtService>()

    post("/login") {
        val credential = call.receive<Credential>()

        // Check username and password
        // ...TODO

        val token = service.createToken(credential)

        call.application.log.info("Token created for user: ${credential.username}")
        call.response.header("Authorization", "Bearer $token")
        call.respond("Token created")
    }
}
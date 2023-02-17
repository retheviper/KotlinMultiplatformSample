package com.retheviper.bbs.auth.route

import com.retheviper.bbs.auth.domain.model.Credential
import com.retheviper.bbs.auth.domain.service.JwtService
import com.retheviper.bbs.constant.AUTH
import com.retheviper.bbs.constant.LOGIN
import com.retheviper.bbs.constant.REFRESH
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.routeAuth() {

    val service by inject<JwtService>()

    route(AUTH) {
        post(LOGIN) {
            val credential = Credential.from(call.receive())

            val token = service.createToken(credential)

            call.application.log.info("Token created for user: ${credential.username}")
            call.response.header("Authorization", "Bearer $token")
            call.respond("Token created")
        }
        get(REFRESH) {
            val oldToken = call.request.header("Authorization")?.removePrefix("Bearer ") ?: ""
            val newToken = service.refreshToken(oldToken)

            call.response.header("Authorization", "Bearer $newToken")
            call.respond("Token created")
        }
    }
}
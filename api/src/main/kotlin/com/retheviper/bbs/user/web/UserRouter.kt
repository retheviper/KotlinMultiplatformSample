package com.retheviper.bbs.user.web

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.retheviper.bbs.common.extensions.getJwtConfigs
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import java.util.Date

data class User(
    val username: String,
    val password: String
)

private const val ONE_HOUR = 1000 * 60 * 60

fun Routing.authentication(application: Application) {
    post("/login") {
        val user = call.receive<User>()

        // Check username and password
        // ...

        val jwtConfigs = application.getJwtConfigs()

        val token = JWT.create()
            .withAudience(jwtConfigs.audience)
            .withIssuer(jwtConfigs.issuer)
            .withClaim("username", user.username)
            .withExpiresAt(Date(System.currentTimeMillis() + ONE_HOUR))
            .sign(Algorithm.HMAC256(jwtConfigs.secret))

        call.respond(mapOf("token" to token))
    }
}
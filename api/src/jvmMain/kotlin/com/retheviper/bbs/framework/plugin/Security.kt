package com.retheviper.bbs.framework.plugin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.retheviper.bbs.common.property.JwtConfigs
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import org.koin.ktor.ext.inject

fun Application.configureSecurity() {
    val jwtConfigs by inject<JwtConfigs>()

    authentication {
        jwt("auth-jwt") {
            realm = jwtConfigs.realm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtConfigs.secret))
                    .withAudience(jwtConfigs.audience)
                    .withIssuer(jwtConfigs.issuer).build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtConfigs.audience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

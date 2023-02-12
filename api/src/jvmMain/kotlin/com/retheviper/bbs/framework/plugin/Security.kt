package com.retheviper.bbs.framework.plugin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.retheviper.bbs.common.extension.getJwtConfigs
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.configureSecurity() {
    authentication {
        jwt {
            val jwtConfigs = this@configureSecurity.getJwtConfigs()
            realm = jwtConfigs.realm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtConfigs.secret)).withAudience(jwtConfigs.audience)
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

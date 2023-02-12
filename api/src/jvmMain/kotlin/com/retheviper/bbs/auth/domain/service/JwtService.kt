package com.retheviper.bbs.auth.domain.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.retheviper.bbs.auth.domain.model.Credential
import com.retheviper.bbs.common.property.JwtConfigs
import java.util.Date

class JwtService {

    private val oneHour = 1000 * 60 * 60

    fun createToken(jwtConfigs: JwtConfigs, credential: Credential): String {
        return JWT.create()
            .withAudience(jwtConfigs.audience)
            .withIssuer(jwtConfigs.issuer)
            .withClaim("username", credential.username)
            .withExpiresAt(Date(System.currentTimeMillis() + oneHour))
            .sign(Algorithm.HMAC256(jwtConfigs.secret))
    }

    fun checkToken(jwtConfigs: JwtConfigs, token: String): Boolean {
        return try {
            JWT.require(Algorithm.HMAC256(jwtConfigs.secret))
                .withAudience(jwtConfigs.audience)
                .withIssuer(jwtConfigs.issuer)
                .build()
                .verify(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}
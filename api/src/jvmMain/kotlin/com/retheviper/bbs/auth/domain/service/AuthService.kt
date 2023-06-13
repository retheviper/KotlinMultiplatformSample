package com.retheviper.bbs.auth.domain.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.retheviper.bbs.auth.domain.model.Credential
import com.retheviper.bbs.auth.infrastructure.repository.AuthRepository
import com.retheviper.bbs.common.exception.InvalidTokenException
import com.retheviper.bbs.common.exception.PasswordNotMatchException
import com.retheviper.bbs.common.exception.UserNotFoundException
import com.retheviper.bbs.common.extension.notMatchesWith
import com.retheviper.bbs.common.property.JwtConfigs
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.constant.ErrorCode
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Date

class AuthService(
    private val jwtConfigs: JwtConfigs,
    private val repository: AuthRepository
) {

    private val oneHour = 1_000 * 60 * 60
    private val algorithm = Algorithm.HMAC256(jwtConfigs.secret)

    fun findCredential(username: String): Credential? {
        return repository.find(username)
    }

    fun createToken(userId: UserId, username: String): String {
        return JWT.create()
            .withAudience(jwtConfigs.audience)
            .withIssuer(jwtConfigs.issuer)
            .withClaim("userId", userId.value)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + oneHour))
            .sign(algorithm)
    }

    fun refreshToken(token: String): String {
        JWT.decode(token).let {
            val userId = UserId(it.getClaim("userId").asInt())
            val username = it.getClaim("username").asString()
            return createToken(userId, username)
        }
    }

    fun isValidToken(token: String): Boolean {
        return runCatching {
            JWT.require(algorithm)
                .withAudience(jwtConfigs.audience)
                .withIssuer(jwtConfigs.issuer)
                .build()
                .verify(token)
        }.isSuccess
    }
}
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
import com.retheviper.bbs.constant.ErrorCode
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Date

class JwtService(
    private val jwtConfigs: JwtConfigs,
    private val repository: AuthRepository
) {

    private val oneHour = 1_000 * 60 * 60
    private val algorithm = Algorithm.HMAC256(jwtConfigs.secret)

    fun createToken(credential: Credential): String {
        val exist =
            transaction { repository.find(credential.username) } ?: throw UserNotFoundException("User not found.")

        if (credential.password notMatchesWith exist.password) {
            throw PasswordNotMatchException("Invalid password.", ErrorCode.USER_PASSWORD_NOT_MATCH)
        }

        return createToken(checkNotNull(exist.userId).value, exist.username)
    }

    fun refreshToken(token: String): String {
        if (!isValidToken(token)) {
            throw InvalidTokenException("Invalid token.")
        }

        JWT.decode(token).let {
            return createToken(it.getClaim("userId").asInt(), it.getClaim("username").asString())
        }
    }

    private fun createToken(userId: Int, username: String): String {
        return JWT.create()
            .withAudience(jwtConfigs.audience)
            .withIssuer(jwtConfigs.issuer)
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + oneHour))
            .sign(algorithm)
    }

    private fun isValidToken(token: String): Boolean {
        return try {
            JWT.require(algorithm)
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
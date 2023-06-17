package com.retheviper.bbs.auth.domain.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.retheviper.bbs.auth.domain.model.Credential
import com.retheviper.bbs.auth.infrastructure.repository.AuthRepository
import com.retheviper.bbs.common.exception.InvalidTokenException
import com.retheviper.bbs.common.property.JwtConfigs
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.DatabaseFreeSpec
import com.retheviper.bbs.testing.toToken
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.Date

class AuthServiceTest : DatabaseFreeSpec({

    val repository = mockk<AuthRepository>()
    val config = mockk<JwtConfigs> {
        every { secret } returns "secret"
        every { issuer } returns "issuer"
        every { audience } returns "audience"
    }
    val algorithm = Algorithm.HMAC256(config.secret)
    val userId = UserId(1)
    val credential = Credential(userId = userId, username = "username", password = "password")
    val service = AuthService(config, repository)

    "Token creation" - {
        "OK" {
            val token = service.createToken(userId, credential.username)

            shouldNotThrow<JWTVerificationException> {
                JWT.require(algorithm)
                    .withAudience(config.audience)
                    .withIssuer(config.issuer)
                    .build()
                    .verify(token)
            }
        }
    }

    "Refresh Token" - {
        "OK" {
            val token = credential.toToken(config)

            shouldNotThrow<InvalidTokenException> {
                service.refreshToken(token)
            }
        }
    }

    "Token Validation" - {
        "OK" {
            val token = credential.toToken(config)

            val result = service.isValidToken(token)
            result shouldBe true
        }

        "NG - Token expired" {
            val token = JWT.create()
                .withAudience(config.audience)
                .withIssuer(config.issuer)
                .withClaim("userId", userId.value)
                .withClaim("username", credential.username)
                .withExpiresAt(Date(System.currentTimeMillis() - 1))
                .sign(algorithm)

            val result = service.isValidToken(token)
            result shouldBe false
        }
    }
})
package com.retheviper.bbs.auth.domain.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.retheviper.bbs.auth.domain.model.Credential
import com.retheviper.bbs.auth.infrastructure.repository.AuthRepository
import com.retheviper.bbs.common.exception.InvalidTokenException
import com.retheviper.bbs.common.exception.PasswordNotMatchException
import com.retheviper.bbs.common.exception.UserNotFoundException
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.property.JwtConfigs
import com.retheviper.bbs.testing.FreeSpecWithDb
import com.retheviper.bbs.testing.toToken
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Date

class JwtServiceTest : FreeSpecWithDb({

    val repository = mockk<AuthRepository>()
    val config = mockk<JwtConfigs> {
        every { secret } returns "secret"
        every { issuer } returns "issuer"
        every { audience } returns "audience"
    }
    val algorithm = Algorithm.HMAC256(config.secret)
    val credential = Credential("username", "password")
    val service = JwtService(config, repository)

    "Token creation" - {
        "OK" {
            every { repository.find(credential.username) } returns Credential(
                username = credential.username,
                password = credential.password.toHashedString()
            )

            val token = service.createToken(credential)

            shouldNotThrow<JWTVerificationException> {
                JWT.require(algorithm)
                    .withAudience(config.audience)
                    .withIssuer(config.issuer)
                    .build()
                    .verify(token)
            }

            verify {
                repository.find(credential.username)
            }
        }

        "NG - User not found" {
            every { repository.find(credential.username) } returns null

            shouldThrow<UserNotFoundException> {
                service.createToken(credential)
            }

            verify {
                repository.find(credential.username)
            }
        }

        "NG - Password not match" {
            every { repository.find(credential.username) } returns Credential(
                username = credential.username,
                password = "hashed".toHashedString()
            )

            shouldThrow<PasswordNotMatchException> {
                service.createToken(credential)
            }

            verify {
                repository.find(credential.username)
            }
        }
    }

    "Refresh Token" - {
        "OK" {
            val token = credential.username.toToken(config)

            shouldNotThrow<InvalidTokenException> {
                service.refreshToken(token)
            }
        }

        "NG - Token expired" {
            val token = JWT.create()
                .withAudience(config.audience)
                .withIssuer(config.issuer)
                .withClaim("username", credential.username)
                .withExpiresAt(Date(System.currentTimeMillis() - 10 * 60 * 1000))
                .sign(algorithm)

            shouldThrow<InvalidTokenException> {
                service.refreshToken(token)
            }
        }
    }
})
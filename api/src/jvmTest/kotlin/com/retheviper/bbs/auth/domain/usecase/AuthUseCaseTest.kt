package com.retheviper.bbs.auth.domain.usecase

import com.retheviper.bbs.auth.domain.service.AuthService
import com.retheviper.bbs.common.exception.InvalidTokenException
import com.retheviper.bbs.common.exception.PasswordNotMatchException
import com.retheviper.bbs.common.exception.UserNotFoundException
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.TestModelFactory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AuthUseCaseTest : FreeSpec({

    val service = mockk<AuthService>()
    val usecase = AuthUseCase(service)

    beforeAny { clearAllMocks() }

    "Token Creation" - {
        "OK" {
            val id = UserId(1)
            val credential = TestModelFactory.credentialModel()

            every { service.findCredential(credential.username) } returns credential
                .copy(password = credential.password.toHashedString())
            every { service.createToken(id, credential.username) } returns "token"

            val token = usecase.createToken(credential)

            token shouldBe "token"
            verify {
                service.findCredential(credential.username)
                service.createToken(id, credential.username)
            }
        }

        "NG - no user" {
            val credential = TestModelFactory.credentialModel()

            every { service.findCredential(credential.username) } returns null

            shouldThrow<UserNotFoundException> {
                usecase.createToken(credential)
            }

            verify {
                service.findCredential(credential.username)
            }
        }

        "NG - password unmatched" {
            val credential = TestModelFactory.credentialModel()

            every { service.findCredential(credential.username) } returns credential
                .copy(password = credential.password.toHashedString())

            shouldThrow<PasswordNotMatchException> {
                usecase.createToken(credential.copy(password = "wrong password"))
            }

            verify {
                service.findCredential(credential.username)
            }
        }
    }

    "Refresh Token" - {
        "OK" {
            every { service.isValidToken(any()) } returns true
            every { service.refreshToken(any()) } returns "refreshed token"

            val token = usecase.refreshToken("token")

            token shouldBe "refreshed token"

            verify {
                service.isValidToken("token")
                service.refreshToken("token")
            }
        }

        "NG - token is not valid" {
            every { service.isValidToken(any()) } returns false

            shouldThrow<InvalidTokenException> {
                usecase.refreshToken("token")
            }

            verify {
                service.isValidToken("token")
            }
        }
    }
})
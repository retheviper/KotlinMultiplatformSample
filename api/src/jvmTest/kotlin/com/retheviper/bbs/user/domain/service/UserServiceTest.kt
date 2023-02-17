package com.retheviper.bbs.user.domain.service

import com.retheviper.bbs.common.exception.UserAlreadyExistsException
import com.retheviper.bbs.common.exception.UserNotFoundException
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.FreeSpecWithDb
import com.retheviper.bbs.user.domain.model.User
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UserServiceTest : FreeSpecWithDb({

    val repository = mockk<UserRepository>()
    val service = UserService(repository)

    val user = User(
        username = "username",
        password = "password",
        name = "name",
        mail = "user@mail.com"
    )

    "find" - {
        "OK" {
            every { repository.find(UserId(1)) } returns user

            val result = service.find(UserId(1))

            result shouldBe user

            verify {
                repository.find(UserId(1))
            }
        }

        "NG - user not found" {
            every { repository.find(UserId(1)) } returns null

            shouldThrow<UserNotFoundException> { service.find(UserId(1)) }

            verify {
                repository.find(UserId(1))
            }
        }
    }

    "create" - {
        "OK" {
            val expected = user.copy(id = UserId(1), password = user.password.toHashedString())

            every { repository.create(any()) } returns expected
            every { repository.find(user.username) } returns null

            val result = service.create(user)

            result shouldBe expected

            verify {
                repository.create(any())
            }
        }

        "NG - user already exists" {
            every { repository.find(user.username) } returns user

            shouldThrow<UserAlreadyExistsException> { service.create(user) }

            verify {
                repository.find(user.username)
            }
        }
    }
})
package com.retheviper.bbs.user.domain.service

import com.retheviper.bbs.common.exception.UserAlreadyExistsException
import com.retheviper.bbs.common.exception.UserNotFoundException
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.DatabaseFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import com.retheviper.bbs.user.domain.service.UserService
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UserServiceTest : DatabaseFreeSpec({

    val user = TestModelFactory.userModel()

    "find" - {
        "OK" {
            val repository = mockk<UserRepository> {
                every { find(UserId(1)) } returns user
            }
            val service = UserService(repository)

            val result = service.find(UserId(1))

            result shouldBe user

            verify(exactly = 1) { repository.find(UserId(1)) }
        }

        "NG - user not found" {
            val repository = mockk<UserRepository> {
                every { find(UserId(1)) } returns null
            }
            val service = UserService(repository)

            shouldThrow<UserNotFoundException> { service.find(UserId(1)) }

            verify(exactly = 1) { repository.find(UserId(1)) }
        }
    }

    "create" - {
        "OK" {
            val expected = user.copy(id = UserId(1), password = user.password.toHashedString())

            val repository = mockk<UserRepository> {
                every { create(any()) } returns expected
                every { find(user.username) } returns null
            }
            val service = UserService(repository)

            val result = service.create(user)

            result shouldBe expected

            verify(exactly = 1) { repository.create(any()) }
        }

        "NG - user already exists" {
            val repository = mockk<UserRepository> {
                every { find(user.username) } returns user
            }
            val service = UserService(repository)

            shouldThrow<UserAlreadyExistsException> { service.create(user) }

            verify(exactly = 1) { repository.find(user.username) }
        }
    }
})
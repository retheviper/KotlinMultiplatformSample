package com.retheviper.bbs.user.domain.service

import com.retheviper.bbs.common.exception.UserAlreadyExistsException
import com.retheviper.bbs.common.exception.UserNotFoundException
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.DatabaseFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UserServiceTest : DatabaseFreeSpec({

    val repository = mockk<UserRepository>()
    val service = UserService(repository)
    val user = TestModelFactory.userModel()

    beforeAny { clearAllMocks() }

    "find" - {
        "OK" {
            every { repository.find(UserId(1)) } returns user

            val result = service.find(UserId(1))

            result shouldBe user

            verify(exactly = 1) { repository.find(UserId(1)) }
        }

        "OK - user not found" {
            every { repository.find(UserId(1)) } returns null

            val result = service.find(UserId(1))

            result shouldBe null

            verify(exactly = 1) { repository.find(UserId(1)) }
        }
    }

    "create" - {
        "OK" {
            val expected = user.copy(id = UserId(1), password = user.password.toHashedString())

            every { repository.create(any()) } returns expected
            every { repository.find(user.username) } returns null

            val result = service.create(user)

            result shouldBe expected

            verify(exactly = 1) { repository.create(any()) }
        }
    }
})
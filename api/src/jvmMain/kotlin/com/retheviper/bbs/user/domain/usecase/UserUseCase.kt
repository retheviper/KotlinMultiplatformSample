package com.retheviper.bbs.user.domain.usecase

import com.retheviper.bbs.common.domain.service.SensitiveWordService
import com.retheviper.bbs.common.domain.usecase.HasSensitiveWordService
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.exception.UserAlreadyExistsException
import com.retheviper.bbs.common.exception.UserNotFoundException
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.user.domain.model.User
import com.retheviper.bbs.user.domain.service.UserService

class UserUseCase(
    private val service: UserService,
    override val sensitiveWordService: SensitiveWordService
) : HasSensitiveWordService {

    @Throws(UserAlreadyExistsException::class)
    fun find(id: UserId): User {
        return service.find(id) ?: throw UserNotFoundException("User not found with id: $id.")
    }

    fun create(user: User): User {
        checkValidUsername(user.username)
        return service.create(user)
    }

    @Throws(BadRequestException::class)
    fun update(user: User): User {
        user.id ?: throw BadRequestException("User id is null.")
        find(user.id)
        checkValidUsername(user.username)
        return service.update(user)
    }

    fun delete(id: UserId) {
        find(id)
        service.delete(id)
    }

    private fun checkValidUsername(username: String) {
        val existingUser = service.find(username)

        existingUser?.let {
            throw UserAlreadyExistsException("User already exists with username: $username")
        }

        checkSensitiveWords(username)
    }
}
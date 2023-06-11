package com.retheviper.bbs.user.domain.usecase

import com.retheviper.bbs.common.exception.UserAlreadyExistsException
import com.retheviper.bbs.common.exception.UserNotFoundException
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.user.domain.model.User
import com.retheviper.bbs.user.domain.service.UserService

class UserUseCase(private val service: UserService) {

    @Throws(UserNotFoundException::class)
    fun find(id: UserId): User {
        return service.find(id) ?: throw UserNotFoundException("User not found with id: $id.")
    }

    @Throws(UserAlreadyExistsException::class)
    fun create(user: User): User {
        if (service.find(user.username) != null) {
            throw UserAlreadyExistsException("User already exists with username: ${user.username}.")
        }

        return service.create(user.copy(password = user.password.toHashedString()))
    }
}
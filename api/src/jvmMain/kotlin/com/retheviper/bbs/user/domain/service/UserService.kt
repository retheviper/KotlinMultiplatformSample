package com.retheviper.bbs.user.domain.service

import com.retheviper.bbs.common.domain.usecase.HasSensitiveWordService
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.exception.UserAlreadyExistsException
import com.retheviper.bbs.common.exception.UserNotFoundException
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.user.domain.model.User
import com.retheviper.bbs.user.infrastructure.repository.UserRepository

class UserService(private val repository: UserRepository) {

    fun find(id: UserId): User? {
        return repository.find(id)
    }

    fun find(username: String): User? {
        return repository.find(username)
    }

    fun create(user: User): User {
        return repository.create(user)
    }

    @Throws(BadRequestException::class)
    fun update(user: User): User {
        user.id ?: throw BadRequestException("User id is null.")
        return repository.update(user)
    }

    fun delete(id: UserId) {
        repository.delete(id)
    }
}
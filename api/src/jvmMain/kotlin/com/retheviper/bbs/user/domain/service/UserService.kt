package com.retheviper.bbs.user.domain.service

import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.exception.UserAlreadyExistsException
import com.retheviper.bbs.common.exception.UserNotFoundException
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.user.domain.model.User
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import org.jetbrains.exposed.sql.transactions.transaction

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
}
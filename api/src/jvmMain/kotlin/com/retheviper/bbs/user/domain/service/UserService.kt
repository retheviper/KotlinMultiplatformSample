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

    @Throws(BadRequestException::class)
    fun find(id: UserId): User {
        return transaction {
            repository.find(id)
        } ?: throw UserNotFoundException("User not found with id: $id.")
    }

    @Throws(BadRequestException::class)
    fun create(user: User): User? {
        require(user.username.isNotEmpty() && Regex("^[a-zA-Z0-9]{4,16}$").matches(user.username)) {
            throw BadRequestException("Username must be 4 to 16 characters long and only contain alphanumeric characters.")
        }

        require(user.password.isNotEmpty() && Regex("^[a-zA-Z0-9]{8,16}$").matches(user.password)) {
            throw BadRequestException("Password must be 8 to 16 characters long and only contain alphanumeric characters.")
        }

        require(user.name.isNotEmpty() && user.name.length in 2..16) {
            throw BadRequestException("Name must be 2 to 16 characters long.")
        }

        require(user.mail.isNotEmpty() && Regex("^[a-zA-Z0-9]+@[a-zA-Z0-9]+\\.[a-zA-Z0-9]+$").matches(user.mail)) {
            throw BadRequestException("Email must be in the required format.")
        }

        return transaction {
            if (repository.find(user.username) != null) {
                throw UserAlreadyExistsException("User already exists with username: ${user.username}.")
            }

            repository.create(user.copy(password = user.password.toHashedString()))
        }
    }
}
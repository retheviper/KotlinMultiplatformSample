package com.retheviper.bbs.user.domain.service

import com.retheviper.bbs.common.exception.UserAlreadyExistsException
import com.retheviper.bbs.common.exception.UserNotFoundException
import com.retheviper.bbs.user.domain.model.User
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import org.jetbrains.exposed.sql.transactions.transaction

class UserService(private val repository: UserRepository) {

    fun getUser(id: Int): User {
        return transaction {
            repository.find(id)
        } ?: throw UserNotFoundException("User not found with id: $id.")
    }

    fun createUser(dto: User): User? {
        return transaction {
            if (repository.find(dto.username) != null) {
                throw UserAlreadyExistsException("User already exists with username: ${dto.username}.")
            }

            repository.create(dto.copy(password = dto.password.reversed())) // TODO encrypt
        }
    }
}
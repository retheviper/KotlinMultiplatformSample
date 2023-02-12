package com.retheviper.bbs.user.domain.service

import com.retheviper.bbs.user.domain.model.UserDto
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import org.jetbrains.exposed.sql.transactions.transaction

class UserService(private val repository: UserRepository) {

    fun getUser(id: Int): UserDto? {
        return transaction {
            repository.find(id)
        }
    }
}
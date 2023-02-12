package com.retheviper.bbs.auth.infrastructure.repository

import com.retheviper.bbs.auth.domain.model.Credential
import com.retheviper.bbs.common.infrastructure.table.User
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

class AuthRepository {

    fun find(username: String): Credential? =
        User.select { (User.username eq username) and (User.deleted eq false) }
            .firstOrNull()
            ?.let {
                Credential(
                    username = it[User.username],
                    password = it[User.password]
                )
            }
}
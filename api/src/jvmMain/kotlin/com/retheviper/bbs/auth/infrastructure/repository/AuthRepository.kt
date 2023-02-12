package com.retheviper.bbs.auth.infrastructure.repository

import com.retheviper.bbs.auth.domain.model.Credential
import com.retheviper.bbs.common.infrastructure.table.Users
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

class AuthRepository {

    fun find(username: String): Credential? =
        Users.select { (Users.username eq username) and (Users.deleted eq false) }
            .firstOrNull()
            ?.let {
                Credential(
                    username = it[Users.username],
                    password = it[Users.password]
                )
            }
}
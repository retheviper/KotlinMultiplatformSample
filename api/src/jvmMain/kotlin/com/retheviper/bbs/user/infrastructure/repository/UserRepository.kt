package com.retheviper.bbs.user.infrastructure.repository

import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.infrastructure.table.Users
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.user.domain.model.User
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select

class UserRepository {

    fun find(id: UserId): User? {
        return find { Users.id eq id.value }
    }

    fun find(username: String): User? {
        return find { Users.username eq username }
    }

    private fun find(operation: SqlExpressionBuilder.() -> Op<Boolean>): User? =
        Users.select { operation(this) and (Users.deleted eq false) }
            .firstOrNull()
            ?.toDto()

    fun create(dto: User): User {
        val id = Users.insertAndGetId {
            it[username] = dto.username
            it[password] = dto.password
            it[name] = dto.name
            it[mail] = dto.mail
            insertAuditInfos(it, dto.username)
        }

        return checkNotNull(find { Users.id eq id })
    }

    private fun ResultRow.toDto() = User(
        id = UserId(this[Users.id].value),
        username = this[Users.username],
        password = this[Users.password],
        name = this[Users.name],
        mail = this[Users.mail]
    )
}
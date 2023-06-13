package com.retheviper.bbs.user.infrastructure.repository

import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.extension.updateAuditInfos
import com.retheviper.bbs.common.infrastructure.table.Users
import com.retheviper.bbs.common.infrastructure.table.Users.mail
import com.retheviper.bbs.common.infrastructure.table.Users.username
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.user.domain.model.User
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

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
            it[password] = dto.password.toHashedString()
            it[name] = dto.name
            it[mail] = dto.mail
            insertAuditInfos(it, dto.username)
        }

        return dto.copy(id = UserId(id.value))
    }

    fun update(dto: User): User {
        dto.id ?: throw BadRequestException("User id is null.")

        Users.update({ Users.id eq dto.id.value }) {
            it[username] = dto.username
            it[password] = dto.password.toHashedString()
            it[name] = dto.name
            it[mail] = dto.mail
            updateAuditInfos(it, dto.username)
        }

        return dto
    }

    fun delete(id: UserId) {
        Users.update({ Users.id eq id.value }) {
            it[Users.deleted] = true
        }
    }

    private fun ResultRow.toDto() = User(
        id = UserId(this[Users.id].value),
        username = this[username],
        password = this[Users.password],
        name = this[Users.name],
        mail = this[mail]
    )
}
package com.retheviper.bbs.user.infrastructure.repository

import com.retheviper.bbs.common.infrastructure.table.User
import com.retheviper.bbs.user.domain.model.UserDto
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import java.time.LocalDateTime

class UserRepository {

    fun find(id: Int): UserDto? {
        return find { User.id eq id }
    }

    fun find(username: String): UserDto? {
        return find { User.username eq username }
    }

    private fun find(operation: SqlExpressionBuilder.() -> Op<Boolean>): UserDto? =
        User.select { operation(this) and (User.deleted eq false) }
            .firstOrNull()
            ?.toDto()

    fun create(dto: UserDto): UserDto? {
        val id = User.insertAndGetId {
            it[username] = dto.username
            it[password] = requireNotNull(dto.password)
            it[name] = dto.name
            it[mail] = dto.mail
            it[createdBy] = dto.username
            it[createdDate] = LocalDateTime.now()
            it[lastModifiedBy] = dto.username
            it[lastModifiedDate] = LocalDateTime.now()
            it[deleted] = false
        }

        return find { User.id eq id }
    }

    private fun ResultRow.toDto() = UserDto(
        id = this[User.id].value,
        username = this[User.username],
        password = this[User.password],
        name = this[User.name],
        mail = this[User.mail]
    )
}
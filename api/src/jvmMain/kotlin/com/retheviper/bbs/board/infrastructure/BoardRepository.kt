package com.retheviper.bbs.board.infrastructure

import com.retheviper.bbs.board.domain.model.Board
import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.common.infrastructure.table.Boards
import com.retheviper.bbs.common.infrastructure.table.Boards.authorId
import com.retheviper.bbs.common.infrastructure.table.Comments
import com.retheviper.bbs.common.infrastructure.table.Users
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.select
import java.time.LocalDateTime

class BoardRepository {

    fun findAll(page: Int, pageSize: Int, limit: Int): List<Board> {
        return Boards
            .leftJoin(Users, { authorId }, { Users.id })
            .slice(Boards.columns + Users.name)
            .select(Boards.deleted eq false)
            .limit(limit)
            .limit(pageSize, ((page - 1) * pageSize).toLong())
            .orderBy(Boards.id, SortOrder.ASC)
            .map {
                Board(
                    id = it[Boards.id].value,
                    title = it[Boards.title],
                    content = it[Boards.content],
                    password = it[Boards.password],
                    authorId = it[Boards.authorId].value,
                    authorName = it[Users.name]
                )
            }
    }

    fun find(id: Int): Board? {
        return Boards
            .leftJoin(Users, { authorId }, { Users.id })
            .slice(Boards.columns + Users.name)
            .select { (Boards.id eq id) and (Boards.deleted eq false) }
            .map {
                Board(
                    id = it[Boards.id].value,
                    title = it[Boards.title],
                    content = it[Boards.content],
                    password = it[Boards.password],
                    authorId = it[authorId].value,
                    authorName = it[Users.name],
                    comments = Comments
                        .leftJoin(Users, { authorId }, { Users.id })
                        .slice(Comments.columns + Users.name + Users.id)
                        .select { (Comments.boardId eq id) and (Comments.deleted eq false) }
                        .map { comment ->
                            Comment(
                                boardId = it[Boards.id].value,
                                id = comment[Comments.id].value,
                                content = comment[Comments.content],
                                password = comment[Comments.password],
                                authorId = comment[Users.id].value,
                                authorName = comment[Users.name]
                            )
                        }
                )
            }.firstOrNull()
    }

    fun create(board: Board) {
        Boards.insert {
            it[title] = board.title
            it[content] = board.content
            it[password] = board.password
            it[authorId] = board.authorId
            it[createdBy] = board.authorName ?: ""
            it[createdDate] = LocalDateTime.now()
            it[lastModifiedBy] = board.authorName ?: ""
            it[lastModifiedDate] = LocalDateTime.now()
            it[deleted] = false
        }
    }
}
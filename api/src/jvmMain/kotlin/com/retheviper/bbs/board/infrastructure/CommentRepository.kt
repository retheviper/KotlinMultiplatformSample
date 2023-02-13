package com.retheviper.bbs.board.infrastructure

import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.extension.updateAuditInfos
import com.retheviper.bbs.common.infrastructure.table.Comments
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class CommentRepository {

    fun findAll(authorId: Int, page: Int, pageSize: Int, limit: Int): List<Comment> {
        return Comments
            .select { (Comments.authorId eq authorId) and (Comments.deleted eq false) }
            .limit(limit)
            .limit(pageSize, ((page - 1) * pageSize).toLong())
            .map {
                Comment(
                    id = it[Comments.id].value,
                    content = it[Comments.content],
                    password = it[Comments.password],
                    authorId = it[Comments.authorId].value,
                    authorName = it[Comments.createdBy],
                    boardId = it[Comments.articleId].value
                )
            }
    }

    fun find(id: Int): Comment? {
        return Comments
            .select { (Comments.id eq id) and (Comments.deleted eq false) }
            .map {
                Comment(
                    id = it[Comments.id].value,
                    content = it[Comments.content],
                    password = it[Comments.password],
                    authorId = it[Comments.authorId].value,
                    authorName = it[Comments.createdBy],
                    boardId = it[Comments.articleId].value
                )
            }
            .firstOrNull()
    }

    fun create(comment: Comment) {
        Comments.insert {
            it[content] = comment.content
            it[password] = comment.password
            it[authorId] = comment.authorId
            it[articleId] = comment.boardId
            insertAuditInfos(it, comment.authorName ?: "")
        }
    }

    fun update(comment: Comment) {
        Comments.update({ Comments.id eq comment.id!! }) {
            it[content] = comment.content
            it[password] = comment.password
            updateAuditInfos(it, comment.authorName ?: "")
        }
    }

    fun delete(id: Int) {
        Comments.update({ Comments.id eq id }) {
            it[deleted] = true
        }
    }
}
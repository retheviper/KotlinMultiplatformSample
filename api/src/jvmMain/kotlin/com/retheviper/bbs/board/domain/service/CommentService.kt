package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.infrastructure.CommentRepository
import com.retheviper.bbs.common.exception.CommentNotFoundException
import com.retheviper.bbs.common.exception.PasswordNotMatchException
import com.retheviper.bbs.constant.ErrorCode
import org.jetbrains.exposed.sql.transactions.transaction

class CommentService(private val repository: CommentRepository) {

    fun findAll(authorId: Int, page: Int, pageSize: Int, limit: Int): List<Comment> {
        return transaction {
            repository.findAll(
                authorId = authorId,
                page = page,
                pageSize = pageSize,
                limit = limit
            )
        }
    }

    fun create(comment: Comment) {
        transaction {
            repository.create(comment) // TODO Encrypt
        }
    }

    fun update(comment: Comment) {
        transaction {
            val id = comment.id ?: throw CommentNotFoundException("Comment id is null.")
            val exist =
                repository.find(id) ?: throw CommentNotFoundException("Comment not found with id: ${comment.id}.")

            if (exist.password != comment.password) { // TODO Encrypt
                throw PasswordNotMatchException(
                    "Comment not found with id: ${comment.id}.",
                    ErrorCode.COMMENT_PASSWORD_NOT_MATCH
                )
            }

            repository.update(comment)
        }
    }

    fun delete(id: Int, password: String) {
        transaction {
            val exist = repository.find(id) ?: throw CommentNotFoundException("Comment not found with id: $id")

            if (exist.password != password) { // TODO Encrypt
                throw PasswordNotMatchException("Comment not found with id: $id", ErrorCode.COMMENT_PASSWORD_NOT_MATCH)
            }

            repository.delete(id)
        }
    }
}
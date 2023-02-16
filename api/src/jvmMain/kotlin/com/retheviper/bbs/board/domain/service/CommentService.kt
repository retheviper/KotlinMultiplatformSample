package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.infrastructure.repository.CommentRepository
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.exception.CommentNotFoundException
import com.retheviper.bbs.common.exception.PasswordNotMatchException
import com.retheviper.bbs.common.extension.notMatchesWith
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.constant.ErrorCode
import org.jetbrains.exposed.sql.transactions.transaction

class CommentService(private val repository: CommentRepository) {

    fun findAll(boardIds: List<Int>): List<Comment> {
        return transaction {
            repository.findAll(boardIds).map {
                Comment.from(it)
            }
        }
    }

    fun findAll(boardId: Int): List<Comment> {
        return transaction {
            repository.findAll(boardId).map {
                Comment.from(it)
            }
        }
    }

    fun findAll(authorId: Int, page: Int, pageSize: Int, limit: Int): List<Comment> {
        return transaction {
            repository.findAll(
                authorId = authorId,
                page = page,
                pageSize = pageSize,
                limit = limit
            ).map {
                Comment.from(it)
            }
        }
    }

    fun create(comment: Comment) {
        transaction {
            repository.create(comment.copy(password = comment.password.toHashedString()))
        }
    }

    @Throws(BadRequestException::class)
    fun update(comment: Comment) {
        transaction {
            val id = comment.id ?: throw CommentNotFoundException("Comment id is null.")
            val exist =
                repository.find(id) ?: throw CommentNotFoundException("Comment not found with id: ${comment.id}.")

            if (comment.password notMatchesWith exist.password) {
                throw PasswordNotMatchException(
                    "Comment's password not match with id: ${comment.id}.",
                    ErrorCode.COMMENT_PASSWORD_NOT_MATCH
                )
            }

            repository.update(comment)
        }
    }

    @Throws(BadRequestException::class)
    fun delete(id: Int, password: String) {
        transaction {
            val exist = repository.find(id) ?: throw CommentNotFoundException("Comment not found with id: $id")

            if (password notMatchesWith exist.password) {
                throw PasswordNotMatchException("Comment not found with id: $id", ErrorCode.COMMENT_PASSWORD_NOT_MATCH)
            }

            repository.delete(id)
        }
    }
}
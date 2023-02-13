package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.infrastructure.CommentRepository
import com.retheviper.bbs.common.exception.CommentNotFoundException
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
            repository.create(comment)
        }
    }

    fun update(comment: Comment) {
        transaction {
            repository.update(comment)
        }
    }

    fun delete(id: Int, password: String) {
        transaction {
            val exists = repository.find(id) ?: throw CommentNotFoundException("Comment not found with id: $id")

            if (exists.password != password) {
                throw CommentNotFoundException("Comment not found with id: $id")
            }


            repository.delete(id)
        }
    }
}
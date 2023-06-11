package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.infrastructure.repository.CommentRepository
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.exception.CommentNotFoundException
import com.retheviper.bbs.common.exception.PasswordNotMatchException
import com.retheviper.bbs.common.extension.notMatchesWith
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CommentId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.constant.ErrorCode
import com.retheviper.bbs.model.common.PaginationProperties

class CommentService(private val repository: CommentRepository) {

    fun find(id: CommentId): Comment? {
        return repository.find(id)?.let { Comment.from(it) }
    }

    fun create(comment: Comment): Comment {
        val id = repository.create(comment)
        return comment.copy(id = id)
    }

    @Throws(BadRequestException::class)
    fun update(comment: Comment): Comment {
        return comment.also { repository.update(it) }
    }

    @Throws(BadRequestException::class)
    fun delete(id: CommentId) {
        repository.delete(id)
    }
}
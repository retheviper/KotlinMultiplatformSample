package com.retheviper.bbs.board.domain.usecase

import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.domain.service.CommentService
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.exception.CommentAuthorNotMatchException
import com.retheviper.bbs.common.exception.CommentNotFoundException
import com.retheviper.bbs.common.exception.IdNotMatchException
import com.retheviper.bbs.common.exception.PasswordNotMatchException
import com.retheviper.bbs.common.extension.notMatchesWith
import com.retheviper.bbs.common.value.CommentId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.constant.ErrorCode

class CommentUseCase(
    private val commentService: CommentService
) {
    fun create(comment: Comment): Comment {
        return commentService.create(comment)
    }

    fun update(comment: Comment): Comment {
        comment.id ?: throw BadRequestException("Comment id must not be null.")

        if (comment.password.isBlank()) {
            throw PasswordNotMatchException("Comment password must not be null or blank.", ErrorCode.COMMENT_PASSWORD_NOT_MATCH)
        }

        val exist = commentService.find(comment.id) ?: throw CommentNotFoundException("Comment not found with id: ${comment.id}.")

        if (comment.articleId != exist.articleId) {
            throw IdNotMatchException("Comment article not match with id: ${comment.id}.")
        }

        if (comment.authorId != exist.authorId) {
            throw CommentAuthorNotMatchException("Comment author not match with id: ${comment.id}.")
        }

        if (comment.password notMatchesWith exist.password) {
            throw PasswordNotMatchException("Comment password not match with id: ${comment.id}", ErrorCode.COMMENT_PASSWORD_NOT_MATCH)
        }

        return commentService.update(comment)
    }

    fun delete(id: CommentId, userId: UserId, password: String) {
        val exist = commentService.find(id) ?: throw CommentNotFoundException("Comment not found with id: $id")

        if (userId != exist.authorId) {
            throw CommentAuthorNotMatchException("Comment author not match with id: $id")
        }

        if (password notMatchesWith exist.password) {
            throw PasswordNotMatchException("Comment password not match with id: $id", ErrorCode.COMMENT_PASSWORD_NOT_MATCH)
        }

        commentService.delete(id)
    }
}
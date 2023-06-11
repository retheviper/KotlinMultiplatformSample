package com.retheviper.bbs.board.domain.model

import com.retheviper.bbs.board.infrastructure.model.CommentRecord
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.extension.trimAll
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CommentId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.model.request.CreateCommentRequest
import com.retheviper.bbs.model.request.UpdateCommentRequest

data class Comment(
    val articleId: ArticleId,
    val id: CommentId? = null,
    val content: String,
    val password: String,
    val authorId: UserId,
    val authorName: String? = null
) {
    companion object {
        fun from(commentRecord: CommentRecord): Comment {
            return Comment(
                articleId = commentRecord.articleId,
                id = commentRecord.id,
                content = commentRecord.content,
                password = commentRecord.password,
                authorId = commentRecord.authorId,
                authorName = commentRecord.authorName
            )
        }

        fun from(articleId: ArticleId, request: CreateCommentRequest): Comment {
            return Comment(
                articleId = articleId,
                content = request.content.trimAll(),
                password = request.password.trimAll().toHashedString(),
                authorId = UserId(request.authorId)
            )
        }

        fun from(articleId: ArticleId, commentId: CommentId, authorId: UserId, request: UpdateCommentRequest): Comment {
            return Comment(
                articleId = articleId,
                id = commentId,
                content = request.content.trimAll(),
                password = request.password.trimAll().toHashedString(),
                authorId = authorId
            )
        }
    }
}

package com.retheviper.bbs.board.domain.model

import com.retheviper.bbs.board.infrastructure.model.CommentRecord
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CommentId
import com.retheviper.bbs.common.value.UserId

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
    }
}

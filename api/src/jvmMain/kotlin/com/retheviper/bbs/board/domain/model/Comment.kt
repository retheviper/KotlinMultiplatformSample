package com.retheviper.bbs.board.domain.model

import com.retheviper.bbs.board.infrastructure.model.CommentRecord

data class Comment(
    val articleId: Int,
    val id: Int? = null,
    val content: String,
    val password: String,
    val authorId: Int,
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

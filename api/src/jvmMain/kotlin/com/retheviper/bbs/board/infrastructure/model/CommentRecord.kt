package com.retheviper.bbs.board.infrastructure.model

import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CommentId
import com.retheviper.bbs.common.value.UserId

data class CommentRecord(
    val articleId: ArticleId,
    val id: CommentId,
    val content: String,
    val password: String,
    val authorId: UserId,
    val authorName: String
)

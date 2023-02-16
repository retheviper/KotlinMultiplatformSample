package com.retheviper.bbs.board.infrastructure.model

import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.UserId

data class ArticleRecord(
    val id: ArticleId,
    val title: String,
    val content: String,
    val password: String,
    val authorId: UserId,
    val authorName: String,
)

package com.retheviper.bbs.board.infrastructure.model

import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CategoryId
import com.retheviper.bbs.common.value.UserId
import java.time.LocalDateTime

data class ArticleRecord(
    val id: ArticleId,
    val title: String,
    val content: String,
    val password: String,
    val authorId: UserId,
    val authorName: String,
    val categoryId: CategoryId,
    val likeCount: Int,
    val dislikeCount: Int,
    val viewCount: Int,
    val createdDate: LocalDateTime,
    val updatedDate: LocalDateTime
)

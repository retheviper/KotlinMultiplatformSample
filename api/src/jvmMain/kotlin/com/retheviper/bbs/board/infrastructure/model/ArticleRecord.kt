package com.retheviper.bbs.board.infrastructure.model

import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.CategoryId
import com.retheviper.bbs.common.value.UserId
import java.time.LocalDateTime

data class ArticleRecord(
    val boardId: BoardId,
    val id: ArticleId,
    val title: String,
    val content: String,
    val password: String,
    val authorId: UserId,
    val authorName: String,
    val categoryId: CategoryId?,
    val categoryName: String,
    val likeCount: UInt,
    val dislikeCount: UInt,
    val viewCount: UInt,
    val createdDate: LocalDateTime,
    val updatedDate: LocalDateTime
)

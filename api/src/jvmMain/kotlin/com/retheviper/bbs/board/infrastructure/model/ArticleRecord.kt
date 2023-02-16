package com.retheviper.bbs.board.infrastructure.model

data class ArticleRecord(
    val id: Int,
    val title: String,
    val content: String,
    val password: String,
    val authorId: Int,
    val authorName: String,
)

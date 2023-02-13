package com.retheviper.bbs.board.domain.model

data class Article(
    val id: Int? = null,
    val title: String,
    val content: String,
    val password: String,
    val authorId: Int,
    val authorName: String? = null,
    val comments: List<Comment>? = null
)

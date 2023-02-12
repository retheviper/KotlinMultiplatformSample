package com.retheviper.bbs.board.domain.model

data class Comment(
    val boardId: Int,
    val id: Int? = null,
    val content: String,
    val password: String,
    val authorId: Int,
    val authorName: String? = null
)

package com.retheviper.bbs.board.infrastructure.model

data class CommentRecord(
    val articleId: Int,
    val id: Int,
    val content: String,
    val password: String,
    val authorId: Int,
    val authorName: String
)

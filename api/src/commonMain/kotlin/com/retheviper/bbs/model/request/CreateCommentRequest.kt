package com.retheviper.bbs.model.request

class CreateCommentRequest(
    val content: String,
    val authorId: Int,
    val password: String,
    val authorName: String,
    val boardId: Int
)
package com.retheviper.bbs.board.infrastructure.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentRequest(
    val content: String,
    val authorId: Int,
    val password: String,
    val authorName: String,
    val boardId: Int
)
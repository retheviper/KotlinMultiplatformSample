package com.retheviper.bbs.model.request

import kotlinx.serialization.Serializable

@Serializable
class CreateCommentRequest(
    val content: String,
    val authorId: Int,
    val password: String,
    val authorName: String,
    val boardId: Int
)
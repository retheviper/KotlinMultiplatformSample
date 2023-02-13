package com.retheviper.bbs.model.response

import kotlinx.serialization.Serializable

@Serializable
data class GetCommentResponse(
    val id: Int,
    val content: String,
    val author: String
)
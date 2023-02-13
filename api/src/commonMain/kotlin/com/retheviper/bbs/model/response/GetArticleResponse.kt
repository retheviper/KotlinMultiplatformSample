package com.retheviper.bbs.model.response

import kotlinx.serialization.Serializable

@Serializable
data class GetArticleResponse(
    val id: Int,
    val title: String,
    val content: String,
    val author: String,
    val comments: List<GetCommentResponse>
)

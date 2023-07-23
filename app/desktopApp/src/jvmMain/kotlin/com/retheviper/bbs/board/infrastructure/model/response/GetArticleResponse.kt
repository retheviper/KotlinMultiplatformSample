package com.retheviper.bbs.board.infrastructure.model.response

import kotlinx.serialization.Serializable

@Serializable
data class GetArticleResponse(
    val id: Int,
    val title: String,
    val content: String,
    val author: String,
    val viewCount: Int,
    val likeCount: Int,
    val dislikeCount: Int,
    val categoryName: String,
    val tags: List<String>,
    val comments: List<GetCommentResponse>
)

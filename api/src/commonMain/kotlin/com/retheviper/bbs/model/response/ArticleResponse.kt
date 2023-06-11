package com.retheviper.bbs.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ArticleResponse(
    val id: Int,
    val title: String,
    val content: String,
    val author: String,
    val viewCount: UInt,
    val likeCount: UInt,
    val dislikeCount: UInt,
    val categoryName: String,
    val tags: List<String>,
    val comments: List<CommentResponse>
)

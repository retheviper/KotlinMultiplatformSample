package com.retheviper.bbs.model.response

import kotlinx.serialization.Serializable

@Serializable
data class GetBoardResponse(
    val id: Int,
    val title: String,
    val content: String,
    val author: String,
    val comments: List<Comment>
) {
    @Serializable
    data class Comment(
        val id: Int,
        val content: String,
        val author: String
    )
}

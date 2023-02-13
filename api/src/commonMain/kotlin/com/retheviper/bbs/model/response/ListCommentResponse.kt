package com.retheviper.bbs.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ListCommentResponse(
    val page: Int,
    val limit: Int,
    val pageSize: Int,
    val comments: List<GetCommentResponse>
)

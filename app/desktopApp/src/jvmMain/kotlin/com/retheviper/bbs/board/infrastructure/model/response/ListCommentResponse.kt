package com.retheviper.bbs.board.infrastructure.model.response

import com.retheviper.bbs.model.common.PaginationProperties
import kotlinx.serialization.Serializable

@Serializable
data class ListCommentResponse(
    val paginationProperties: PaginationProperties,
    val comments: List<GetCommentResponse>
)

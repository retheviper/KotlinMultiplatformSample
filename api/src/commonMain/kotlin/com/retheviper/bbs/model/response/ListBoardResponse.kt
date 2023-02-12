package com.retheviper.bbs.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ListBoardResponse(
    val page: Int,
    val limit: Int,
    val pageSize: Int,
    val boardInfos: List<BoardInfo>
) {
    @Serializable
    data class BoardInfo(
        val id: Int,
        val title: String,
        val authorName: String,
    )
}

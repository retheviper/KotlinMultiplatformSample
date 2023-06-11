package com.retheviper.bbs.board.infrastructure.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ListMessagesResponse(
    val id: Int,
    val messageGroupId: Int,
    val userId: Int,
    val username: String,
    val content: String,
    val createdDate: String,
    val updatedDate: String
)
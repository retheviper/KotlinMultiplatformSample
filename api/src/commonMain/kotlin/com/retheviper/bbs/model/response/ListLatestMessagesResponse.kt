package com.retheviper.bbs.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ListLatestMessagesResponse(
    val id: Int,
    val messageGroupId: Int,
    val userId: Int,
    val username: String,
    val content: String,
    val createdDate: String,
    val updatedDate: String
)
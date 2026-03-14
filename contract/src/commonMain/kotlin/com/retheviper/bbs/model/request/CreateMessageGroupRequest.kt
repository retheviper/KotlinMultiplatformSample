package com.retheviper.bbs.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateMessageGroupRequest(
    val memberIds : List<Int>
)
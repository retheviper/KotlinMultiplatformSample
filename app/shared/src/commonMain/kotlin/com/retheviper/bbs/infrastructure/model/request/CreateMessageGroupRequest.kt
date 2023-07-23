package com.retheviper.bbs.infrastructure.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateMessageGroupRequest(
    val memberIds : List<Int>
)
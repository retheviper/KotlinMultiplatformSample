package com.retheviper.bbs.common.model

import kotlinx.serialization.Serializable

@Serializable
data class Count(
    val platform: String,
    val number: Int
)
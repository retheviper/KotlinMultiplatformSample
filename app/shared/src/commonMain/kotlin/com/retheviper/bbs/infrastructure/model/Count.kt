package com.retheviper.bbs.infrastructure.model

import kotlinx.serialization.Serializable

@Serializable
data class Count(
    val platform: String,
    val number: Int
)
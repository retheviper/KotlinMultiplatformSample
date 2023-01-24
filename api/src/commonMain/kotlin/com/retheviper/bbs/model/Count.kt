package com.retheviper.bbs.model

import kotlinx.serialization.Serializable

@Serializable
data class Count(
    val platform: String = "Web",
    val number: Int
)
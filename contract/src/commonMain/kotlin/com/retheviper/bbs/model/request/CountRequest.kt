package com.retheviper.bbs.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CountRequest(
    val platform: String = "Web",
    val number: Int
)
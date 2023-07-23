package com.retheviper.bbs.model.common

import kotlinx.serialization.Serializable

@Serializable
data class PaginationProperties(
    val page: Int,
    val size: Int,
    val limit: Int,
)
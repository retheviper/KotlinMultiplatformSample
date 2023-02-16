package com.retheviper.bbs.model.response

import kotlinx.serialization.Serializable

@Serializable
data class GetHealthResponse(
    val health: String,
    val databaseConnection: String
)

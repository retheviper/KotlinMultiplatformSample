package com.retheviper.bbs.infrastructure.model.response

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val health: String,
    val databaseConnection: String
)

package com.retheviper.bbs.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ExceptionResponse(
    val code: String,
    val message: String
)
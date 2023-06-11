package com.retheviper.bbs.common.domain.model

import com.retheviper.bbs.common.value.UserId

data class AuthUser(
    val id: UserId,
    val username: String
)

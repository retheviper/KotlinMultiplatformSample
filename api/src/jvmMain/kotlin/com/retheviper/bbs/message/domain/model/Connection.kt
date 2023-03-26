package com.retheviper.bbs.message.domain.model

import com.retheviper.bbs.common.value.UserId
import io.ktor.websocket.DefaultWebSocketSession


data class Connection(
    val session: DefaultWebSocketSession,
    val userId: UserId,
    val username: String
)
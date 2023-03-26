package com.retheviper.bbs.message.infrastructure.model

import com.retheviper.bbs.common.value.MessageGroupId
import com.retheviper.bbs.common.value.UserId

data class MessageGroupRecord(
    val id: MessageGroupId,
    val ownerId: UserId
)
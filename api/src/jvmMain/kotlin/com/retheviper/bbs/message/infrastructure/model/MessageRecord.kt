package com.retheviper.bbs.message.infrastructure.model

import com.retheviper.bbs.common.value.MessageGroupId
import com.retheviper.bbs.common.value.MessageId
import com.retheviper.bbs.common.value.UserId
import java.time.LocalDateTime

data class MessageRecord(
    val id: MessageId,
    val messageGroupId: MessageGroupId,
    val userId: UserId,
    val username: String,
    val content: String,
    val createdDate: LocalDateTime,
    val updatedDate: LocalDateTime
)
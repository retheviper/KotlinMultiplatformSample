package com.retheviper.bbs.message.domain.model

import com.retheviper.bbs.common.value.MessageGroupId
import com.retheviper.bbs.common.value.MessageId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.message.infrastructure.model.MessageRecord
import java.time.LocalDateTime

data class Message(
    val id: MessageId? = null,
    val messageGroupId: MessageGroupId,
    val userId: UserId,
    val username: String,
    val content: String,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val updatedDate: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun from(record: MessageRecord): Message {
            return Message(
                id = record.id,
                messageGroupId = record.messageGroupId,
                userId = record.userId,
                username = record.username,
                content = record.content,
                createdDate = record.createdDate,
                updatedDate = record.updatedDate
            )
        }
    }
}
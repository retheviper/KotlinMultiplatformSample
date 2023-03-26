package com.retheviper.bbs.message.domain.model

import com.retheviper.bbs.common.value.MessageGroupId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.message.infrastructure.model.MessageGroupRecord
import com.retheviper.bbs.model.request.CreateMessageGroupRequest

data class MessageGroup(
    val id: MessageGroupId? = null,
    val ownerId: UserId,
    val members: List<UserId>
) {
    companion object {
        fun from(request: CreateMessageGroupRequest, ownerId: UserId): MessageGroup {
            return MessageGroup(
                ownerId = ownerId,
                members = request.memberIds.map { UserId(it) }
            )
        }

        fun from(record: MessageGroupRecord, members: List<UserId>): MessageGroup {
            return MessageGroup(
                id = record.id,
                ownerId = record.ownerId,
                members = members
            )
        }
    }
}
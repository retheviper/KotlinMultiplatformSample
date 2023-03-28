package com.retheviper.bbs.message.infrastructure.repository

import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.infrastructure.table.MessageGroupMembers
import com.retheviper.bbs.common.infrastructure.table.MessageGroups
import com.retheviper.bbs.common.infrastructure.table.Messages
import com.retheviper.bbs.common.infrastructure.table.Users
import com.retheviper.bbs.common.value.MessageGroupId
import com.retheviper.bbs.common.value.MessageId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.message.domain.model.Message
import com.retheviper.bbs.message.domain.model.MessageGroup
import com.retheviper.bbs.message.infrastructure.model.MessageGroupRecord
import com.retheviper.bbs.message.infrastructure.model.MessageRecord
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.select
import java.time.LocalDateTime

class MessageRepository {

    fun findLatestMessages(userId: UserId): List<MessageRecord> {
        return Messages.join(Users, JoinType.LEFT, additionalConstraint = { Messages.userId eq Users.id })
            .select {
                (Messages.userId eq userId.value) and (Messages.deleted eq false)
            }.andWhere {
                Messages.createdDate inSubQuery Messages.slice(Messages.createdDate.max())
                    .select { (Messages.messageGroupId eq Messages.messageGroupId) }
                    .groupBy(Messages.messageGroupId)
            }.map {
                it.toMessageRecord()
            }
    }

    fun findGroup(messageGroupId: MessageGroupId): MessageGroup? {
        val group = MessageGroups.select {
                    (MessageGroups.id eq messageGroupId.value) and (MessageGroups.deleted eq false)
                }.firstOrNull()?.toMessageGroupRecord() ?: return null

        val members = MessageGroupMembers.select {
            (MessageGroupMembers.messageGroupId eq messageGroupId.value)
        }.map {
            UserId(it[MessageGroupMembers.userId].value)
        }

        return MessageGroup.from(group, members)
    }

    fun createGroup(dto: MessageGroup) {
        val messageGroupId = MessageGroups.insertAndGetId {
            it[ownerId] = dto.ownerId.value
            insertAuditInfos(it, "System")
        }
        MessageGroupMembers.batchInsert(dto.members) {
            this[MessageGroupMembers.messageGroupId] = messageGroupId
            this[MessageGroupMembers.userId] = it.value
            MessageGroupMembers.insertAuditInfos(this, "System")
        }
    }

    fun findGroupMessages(messageGroupId: MessageGroupId, after: LocalDateTime?): List<MessageRecord> {
        return Messages.join(Users, JoinType.LEFT, additionalConstraint = { Messages.userId eq Users.id })
            .select {
                (Messages.messageGroupId eq messageGroupId.value) and (Messages.deleted eq false)
            }
            .apply {
                if (after != null) {
                    andWhere { Messages.createdDate greaterEq after }
                }
            }
            .map {
                it.toMessageRecord()
            }
    }

    fun storeMessage(dto: Message) {
        Messages.insert {
            it[messageGroupId] = dto.messageGroupId.value
            it[userId] = dto.userId.value
            it[content] = dto.content
            insertAuditInfos(it, dto.username)
        }
    }

    private fun ResultRow.toMessageGroupRecord() = MessageGroupRecord(
        id = MessageGroupId(this[MessageGroups.id].value),
        ownerId = UserId(this[MessageGroups.ownerId].value),
    )

    private fun ResultRow.toMessageRecord() = MessageRecord(
        id = MessageId(this[Messages.id].value),
        messageGroupId = MessageGroupId(this[Messages.messageGroupId].value),
        userId = UserId(this[Messages.userId].value),
        username = this[Users.username],
        content = this[Messages.content],
        createdDate = this[Messages.createdDate],
        updatedDate = this[Messages.lastModifiedDate]
    )
}
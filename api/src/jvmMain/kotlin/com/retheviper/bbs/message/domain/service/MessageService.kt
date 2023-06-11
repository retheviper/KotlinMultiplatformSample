package com.retheviper.bbs.message.domain.service

import com.retheviper.bbs.common.domain.usecase.HasSensitiveWordService
import com.retheviper.bbs.common.domain.service.SensitiveWordService
import com.retheviper.bbs.common.value.MessageGroupId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.message.domain.model.Message
import com.retheviper.bbs.message.domain.model.MessageGroup
import com.retheviper.bbs.message.infrastructure.repository.MessageRepository
import io.ktor.server.plugins.BadRequestException
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class MessageService(
    private val repository: MessageRepository,
    override val sensitiveWordService: SensitiveWordService
) : HasSensitiveWordService {

    fun findLatestMessages(userId: UserId): List<Message> {
        return transaction {
            repository.findLatestMessages(userId).map { Message.from(it) }
        }
    }

    fun findGroup(messageGroupId: MessageGroupId): MessageGroup {
        return transaction {
            val group = repository.findGroup(messageGroupId) ?: throw BadRequestException("Message group not found.")

            group
        }
    }

    fun createGroup(dto: MessageGroup) {
        val members = dto.members.distinct()

        if (members.size < 2) {
            throw BadRequestException("Message group must have at least two members.")
        }

        if (members.contains(dto.ownerId).not()) {
            throw BadRequestException("Message group owner must be a member.")
        }

        return transaction {
            repository.createGroup(dto)
        }
    }

    fun findGroupMessages(messageGroupId: MessageGroupId, userId: UserId, lastUpdatedTime: LocalDateTime?): List<Message> {
        val groupMessages = transaction {
            repository.findGroupMessages(messageGroupId, lastUpdatedTime).map { Message.from(it) }
        }

        if (groupMessages.none { it.userId == userId }) {
            throw BadRequestException("User is not a member of the message group.")
        }

        return groupMessages
    }

    fun createMessage(dto: Message): Message {
        val sensitiveWords = findSensitiveWords(dto.content)

        val message = if (sensitiveWords.isNotEmpty()) {
            var content = dto.content
            sensitiveWords.forEach { word ->
                content = content.replace(word, "*".repeat(word.length))
            }
            dto.copy(content = content)
        } else {
            dto
        }

        transaction {
            repository.storeMessage(message)
        }

        return message
    }
}
package com.retheviper.chat.messaging.application

import com.retheviper.chat.messaging.domain.Channel
import com.retheviper.chat.messaging.domain.ChannelRepository
import com.retheviper.chat.messaging.domain.DomainValidationException
import com.retheviper.chat.messaging.domain.Message
import com.retheviper.chat.messaging.domain.MessageReactionRepository
import com.retheviper.chat.messaging.domain.MentionNotification
import com.retheviper.chat.messaging.domain.MentionNotificationRepository
import com.retheviper.chat.messaging.domain.NotFoundException
import com.retheviper.chat.messaging.domain.Workspace
import com.retheviper.chat.messaging.domain.WorkspaceMember
import com.retheviper.chat.messaging.domain.WorkspaceRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

@OptIn(ExperimentalUuidApi::class)
class MessagingQueryService(
    private val workspaceRepository: WorkspaceRepository,
    private val channelRepository: ChannelRepository,
    private val messageRepository: com.retheviper.chat.messaging.domain.MessageRepository,
    private val messageReactionRepository: MessageReactionRepository,
    private val mentionNotificationRepository: MentionNotificationRepository,
    private val transactionsEnabled: Boolean
) {
    suspend fun getWorkspace(workspaceId: Uuid): Workspace {
        return inTransaction {
            requireWorkspace(workspaceId)
        }
    }

    suspend fun listWorkspaces(): List<Workspace> {
        return inTransaction {
            workspaceRepository.listWorkspaces()
        }
    }

    suspend fun getWorkspaceBySlug(slug: String): Workspace {
        return inTransaction {
            val normalizedSlug = slug.trim()
            if (normalizedSlug.isBlank()) {
                throw DomainValidationException("workspace slug must not be blank")
            }
            workspaceRepository.findWorkspaceBySlug(normalizedSlug)
                ?: throw NotFoundException("workspace not found")
        }
    }

    suspend fun listMembers(workspaceId: Uuid): List<WorkspaceMember> {
        return inTransaction {
            requireWorkspace(workspaceId)
            workspaceRepository.listMembers(workspaceId)
        }
    }

    suspend fun listChannels(workspaceId: Uuid): List<Channel> {
        return inTransaction {
            requireWorkspace(workspaceId)
            channelRepository.listChannels(workspaceId)
        }
    }

    suspend fun listChannelMessages(channelId: Uuid, beforeMessageId: Uuid?, limit: Int): List<Message> {
        return inTransaction {
            if (limit !in 1..100) {
                throw DomainValidationException("limit must be between 1 and 100")
            }
            hydrateMessages(
                messages = messageRepository.listChannelMessages(channelId, beforeMessageId, limit),
                messageReactionRepository = messageReactionRepository
            )
        }
    }

    suspend fun getThread(messageId: Uuid): Pair<Message, List<Message>> {
        return inTransaction {
            val target = messageRepository.findMessage(messageId)
                ?: throw NotFoundException("message not found")
            val root = target.threadRootMessageId?.let { messageRepository.findMessage(it) ?: throw NotFoundException("thread root not found") }
                ?: target
            val hydrated = hydrateMessages(
                messages = listOf(root) + messageRepository.listThread(root.id),
                messageReactionRepository = messageReactionRepository
            )
            hydrated.first() to hydrated.drop(1)
        }
    }

    suspend fun listMemberNotifications(memberId: Uuid, unreadOnly: Boolean): List<MentionNotification> {
        return inTransaction {
            requireMember(memberId)
            mentionNotificationRepository.listMemberNotifications(memberId, unreadOnly)
        }
    }

    private suspend fun requireWorkspace(workspaceId: Uuid): Workspace {
        return workspaceRepository.findWorkspace(workspaceId)
            ?: throw NotFoundException("workspace not found")
    }

    private suspend fun requireMember(memberId: Uuid): WorkspaceMember {
        return workspaceRepository.findMember(memberId)
            ?: throw NotFoundException("member not found")
    }

    private suspend fun <T> inTransaction(block: suspend () -> T): T {
        return if (transactionsEnabled) suspendTransaction { block() } else block()
    }

}

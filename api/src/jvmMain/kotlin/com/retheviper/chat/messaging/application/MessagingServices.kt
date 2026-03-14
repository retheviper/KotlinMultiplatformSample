package com.retheviper.chat.messaging.application

import com.retheviper.chat.contract.AddWorkspaceMemberRequest
import com.retheviper.chat.contract.CreateChannelRequest
import com.retheviper.chat.contract.CreateWorkspaceRequest
import com.retheviper.chat.contract.PostMessageRequest
import com.retheviper.chat.contract.UpdateWorkspaceMemberRequest
import com.retheviper.chat.contract.WorkspaceMemberRole
import com.retheviper.chat.messaging.domain.Channel
import com.retheviper.chat.messaging.domain.ChannelRepository
import com.retheviper.chat.messaging.domain.DomainValidationException
import com.retheviper.chat.messaging.domain.Message
import com.retheviper.chat.messaging.domain.MessageReaction
import com.retheviper.chat.messaging.domain.MessageReactionRepository
import com.retheviper.chat.messaging.domain.MessageRepository
import com.retheviper.chat.messaging.domain.MentionNotification
import com.retheviper.chat.messaging.domain.MentionNotificationRepository
import com.retheviper.chat.messaging.domain.NotFoundException
import com.retheviper.chat.messaging.domain.NotificationKind
import com.retheviper.chat.messaging.domain.Workspace
import com.retheviper.chat.messaging.domain.WorkspaceMember
import com.retheviper.chat.messaging.domain.WorkspaceRepository
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

@OptIn(ExperimentalUuidApi::class)
class MessagingCommandService(
    private val workspaceRepository: WorkspaceRepository,
    private val channelRepository: ChannelRepository,
    private val messageRepository: MessageRepository,
    private val messageReactionRepository: MessageReactionRepository,
    private val mentionNotificationRepository: MentionNotificationRepository,
    private val notificationEventBus: NotificationEventBus,
    private val transactionsEnabled: Boolean
) {
    suspend fun createWorkspace(request: CreateWorkspaceRequest): Workspace {
        return inTransaction {
            validateSlug(request.slug)
            requireNotBlank(request.name, "workspace name")
            requireNotBlank(request.ownerUserId, "owner user id")
            requireNotBlank(request.ownerDisplayName, "owner display name")

            if (workspaceRepository.existsBySlug(request.slug)) {
                throw DomainValidationException("workspace slug already exists")
            }

            val now = Instant.now()
            val workspaceId = Uuid.generateV7()
            val ownerMember = WorkspaceMember(
                id = Uuid.generateV7(),
                workspaceId = workspaceId,
                userId = request.ownerUserId.trim(),
                displayName = request.ownerDisplayName.trim(),
                role = WorkspaceMemberRole.OWNER,
                joinedAt = now
            )
            val workspace = Workspace(
                id = workspaceId,
                slug = request.slug.trim(),
                name = request.name.trim(),
                ownerMemberId = ownerMember.id,
                createdAt = now
            )
            val defaultChannel = Channel(
                id = Uuid.generateV7(),
                workspaceId = workspaceId,
                slug = "general",
                name = "general",
                topic = "Company-wide announcements and work-based matters",
                visibility = com.retheviper.chat.contract.ChannelVisibility.PUBLIC,
                createdByMemberId = ownerMember.id,
                createdAt = now
            )

            workspaceRepository.saveWorkspace(workspace)
            workspaceRepository.saveMember(ownerMember)
            channelRepository.saveChannel(defaultChannel)

            workspace
        }
    }

    suspend fun addMember(workspaceId: Uuid, request: AddWorkspaceMemberRequest): WorkspaceMember {
        return inTransaction {
            requireWorkspace(workspaceId)
            requireNotBlank(request.userId, "user id")
            requireNotBlank(request.displayName, "display name")

            if (workspaceRepository.findMemberByUserId(workspaceId, request.userId.trim()) != null) {
                throw DomainValidationException("member already exists in workspace")
            }

            val member = WorkspaceMember(
                id = Uuid.generateV7(),
                workspaceId = workspaceId,
                userId = request.userId.trim(),
                displayName = request.displayName.trim(),
                role = request.role,
                joinedAt = Instant.now()
            )

            workspaceRepository.saveMember(member)
            member
        }
    }

    suspend fun updateMember(memberId: Uuid, request: UpdateWorkspaceMemberRequest): WorkspaceMember {
        return inTransaction {
            val existing = requireMember(memberId)
            requireNotBlank(request.displayName, "display name")
            val updated = existing.copy(displayName = request.displayName.trim())
            workspaceRepository.updateMemberDisplayName(memberId, updated.displayName)
            updated
        }
    }

    suspend fun createChannel(workspaceId: Uuid, request: CreateChannelRequest): Channel {
        return inTransaction {
            validateSlug(request.slug)
            requireNotBlank(request.name, "channel name")
            val workspace = requireWorkspace(workspaceId)
            val creator = requireMember(Uuid.parse(request.createdByMemberId))
            if (creator.workspaceId != workspace.id) {
                throw DomainValidationException("channel creator must belong to the workspace")
            }
            if (channelRepository.existsBySlug(workspaceId, request.slug)) {
                throw DomainValidationException("channel slug already exists in workspace")
            }

            val channel = Channel(
                id = Uuid.generateV7(),
                workspaceId = workspaceId,
                slug = request.slug.trim(),
                name = request.name.trim(),
                topic = request.topic?.trim()?.takeIf { it.isNotBlank() },
                visibility = request.visibility,
                createdByMemberId = creator.id,
                createdAt = Instant.now()
            )

            channelRepository.saveChannel(channel)
            channel
        }
    }

    suspend fun postChannelMessage(channelId: Uuid, request: PostMessageRequest): Message {
        return inTransaction {
            val channel = requireChannel(channelId)
            val author = requireMember(Uuid.parse(request.authorMemberId))
            requireMemberInWorkspace(author, channel.workspaceId)
            requireNotBlank(request.body, "message body")
            val workspaceMembers = workspaceRepository.listMembers(channel.workspaceId)

            val message = Message(
                id = Uuid.generateV7(),
                channelId = channelId,
                authorMemberId = author.id,
                authorDisplayName = author.displayName,
                body = request.body.trim(),
                linkPreview = sanitizePreview(request.body, request.linkPreview?.toDomain()),
                threadRootMessageId = null,
                createdAt = Instant.now()
            )
            messageRepository.saveMessage(message)
            saveMentionNotifications(
                message = message,
                author = author,
                workspaceMembers = workspaceMembers
            )
            hydrateMessage(message)
        }
    }

    suspend fun replyToMessage(messageId: Uuid, request: PostMessageRequest): Message {
        return inTransaction {
            val parent = requireMessage(messageId)
            val channel = requireChannel(parent.channelId)
            val author = requireMember(Uuid.parse(request.authorMemberId))
            requireMemberInWorkspace(author, channel.workspaceId)
            requireNotBlank(request.body, "message body")
            val workspaceMembers = workspaceRepository.listMembers(channel.workspaceId)

            val rootId = parent.threadRootMessageId ?: parent.id
            val reply = Message(
                id = Uuid.generateV7(),
                channelId = channel.id,
                authorMemberId = author.id,
                authorDisplayName = author.displayName,
                body = request.body.trim(),
                linkPreview = sanitizePreview(request.body, request.linkPreview?.toDomain()),
                threadRootMessageId = rootId,
                createdAt = Instant.now()
            )
            messageRepository.saveMessage(reply)
            saveMentionNotifications(
                message = reply,
                author = author,
                workspaceMembers = workspaceMembers
            )
            saveThreadActivityNotifications(
                reply = reply,
                rootMessageId = rootId,
                author = author,
                workspaceMembers = workspaceMembers
            )
            hydrateMessage(reply)
        }
    }

    suspend fun toggleReaction(messageId: Uuid, memberId: Uuid, emoji: String): Message {
        return inTransaction {
            val message = requireMessage(messageId)
            val channel = requireChannel(message.channelId)
            val member = requireMember(memberId)
            requireMemberInWorkspace(member, channel.workspaceId)
            validateEmoji(emoji)

            val normalizedEmoji = emoji.trim()
            val existingReaction = messageReactionRepository.findReaction(message.id, member.id, normalizedEmoji)
            if (existingReaction == null) {
                messageReactionRepository.saveReaction(
                    MessageReaction(
                        id = Uuid.generateV7(),
                        messageId = message.id,
                        memberId = member.id,
                        emoji = normalizedEmoji,
                        createdAt = Instant.now()
                    )
                )
            } else {
                messageReactionRepository.deleteReaction(existingReaction.id)
            }

            hydrateMessage(message)
        }
    }

    suspend fun markNotificationsRead(memberId: Uuid, notificationIds: List<Uuid>) {
        inTransaction {
            requireMember(memberId)
            mentionNotificationRepository.markRead(memberId, notificationIds)
        }
        notificationEventBus.publish(memberId)
    }

    private suspend fun requireWorkspace(workspaceId: Uuid): Workspace {
        return workspaceRepository.findWorkspace(workspaceId)
            ?: throw NotFoundException("workspace not found")
    }

    private suspend fun requireMember(memberId: Uuid): WorkspaceMember {
        return workspaceRepository.findMember(memberId)
            ?: throw NotFoundException("member not found")
    }

    private suspend fun requireChannel(channelId: Uuid): Channel {
        return channelRepository.findChannel(channelId)
            ?: throw NotFoundException("channel not found")
    }

    private suspend fun requireMessage(messageId: Uuid): Message {
        return messageRepository.findMessage(messageId)
            ?: throw NotFoundException("message not found")
    }

    private fun requireMemberInWorkspace(member: WorkspaceMember, workspaceId: Uuid) {
        if (member.workspaceId != workspaceId) {
            throw DomainValidationException("member does not belong to the required workspace")
        }
    }

    private fun requireNotBlank(value: String, fieldName: String) {
        if (value.isBlank()) {
            throw DomainValidationException("$fieldName must not be blank")
        }
    }

    private fun validateSlug(slug: String) {
        val trimmed = slug.trim()
        if (!SLUG_REGEX.matches(trimmed)) {
            throw DomainValidationException("slug must match ${SLUG_REGEX.pattern}")
        }
    }

    private fun validateEmoji(emoji: String) {
        val trimmed = emoji.trim()
        if (trimmed.isBlank() || trimmed.length > 16) {
            throw DomainValidationException("emoji must not be blank")
        }
        if (trimmed.any(Char::isWhitespace)) {
            throw DomainValidationException("emoji must not contain whitespace")
        }
    }

    companion object {
        private val SLUG_REGEX = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
        private val MENTION_REGEX = Regex("(^|\\s)@([a-zA-Z0-9._-]+)")
    }

    private suspend fun <T> inTransaction(block: suspend () -> T): T {
        return if (transactionsEnabled) suspendTransaction { block() } else block()
    }

    private suspend fun saveMentionNotifications(
        message: Message,
        author: WorkspaceMember,
        workspaceMembers: List<WorkspaceMember>
    ) {
        val mentionedMembers = workspaceMembers
            .associateBy { it.userId }
            .let { membersByUserId ->
                MENTION_REGEX.findAll(message.body)
                    .mapNotNull { match -> membersByUserId[match.groupValues[2]] }
                    .filter { it.id != author.id }
                    .distinctBy { it.id }
                    .toList()
            }

        if (mentionedMembers.isEmpty()) {
            return
        }

        mentionNotificationRepository.saveNotifications(
            mentionedMembers.map { mentionedMember ->
                MentionNotification(
                    id = Uuid.generateV7(),
                    kind = NotificationKind.MENTION,
                    memberId = mentionedMember.id,
                    channelId = message.channelId,
                    messageId = message.id,
                    threadRootMessageId = message.threadRootMessageId,
                    authorDisplayName = author.displayName,
                    messagePreview = message.body.take(160),
                    createdAt = message.createdAt
                )
            }
        )
        notificationEventBus.publish(mentionedMembers.map { it.id })
    }

    private suspend fun saveThreadActivityNotifications(
        reply: Message,
        rootMessageId: Uuid,
        author: WorkspaceMember,
        workspaceMembers: List<WorkspaceMember>
    ) {
        val rootMessage = requireMessage(rootMessageId)
        val participants = buildSet {
            add(rootMessage.authorMemberId)
            addAll(messageRepository.listThreadParticipantIds(rootMessageId, reply.createdAt))
            addAll(mentionNotificationRepository.listThreadSubscriberIds(rootMessageId))
        }.filter { it != author.id }

        if (participants.isEmpty()) {
            return
        }

        val mentionedUserIds = MENTION_REGEX.findAll(reply.body)
            .map { it.groupValues[2] }
            .toSet()
        val workspaceMembersById = workspaceMembers.associateBy { it.id }
        val notifications = participants.mapNotNull { participantId ->
            val participant = workspaceMembersById[participantId] ?: return@mapNotNull null
            if (participant.userId in mentionedUserIds) {
                return@mapNotNull null
            }

            MentionNotification(
                id = Uuid.generateV7(),
                kind = NotificationKind.THREAD_ACTIVITY,
                memberId = participant.id,
                channelId = reply.channelId,
                messageId = reply.id,
                threadRootMessageId = rootMessageId,
                authorDisplayName = author.displayName,
                messagePreview = reply.body.take(160),
                createdAt = reply.createdAt
            )
        }

        if (notifications.isNotEmpty()) {
            mentionNotificationRepository.saveNotifications(notifications)
            notificationEventBus.publish(notifications.map { it.memberId })
        }
    }

    private suspend fun hydrateMessage(message: Message): Message {
        return hydrateMessages(listOf(message), messageReactionRepository).single()
    }
}

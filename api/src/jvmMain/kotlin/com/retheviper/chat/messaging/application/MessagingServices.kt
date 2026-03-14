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
import com.retheviper.chat.messaging.domain.LinkPreview
import com.retheviper.chat.messaging.domain.Message
import com.retheviper.chat.messaging.domain.MessageReaction
import com.retheviper.chat.messaging.domain.MessageReactionRepository
import com.retheviper.chat.messaging.domain.MessageReactionSummary
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

@OptIn(ExperimentalUuidApi::class)
class MessagingCommandService(
    private val workspaceRepository: WorkspaceRepository,
    private val channelRepository: ChannelRepository,
    private val messageRepository: MessageRepository,
    private val messageReactionRepository: MessageReactionRepository,
    private val mentionNotificationRepository: MentionNotificationRepository,
    private val transactionRunner: TransactionRunner = ExposedTransactionRunner
) {
    suspend fun createWorkspace(request: CreateWorkspaceRequest): Workspace {
        return transactional {
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
        return transactional {
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
        return transactional {
            val existing = requireMember(memberId)
            requireNotBlank(request.displayName, "display name")
            val updated = existing.copy(displayName = request.displayName.trim())
            workspaceRepository.updateMemberDisplayName(memberId, updated.displayName)
            updated
        }
    }

    suspend fun createChannel(workspaceId: Uuid, request: CreateChannelRequest): Channel {
        return transactional {
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
        return transactional {
            val channel = requireChannel(channelId)
            val author = requireMember(Uuid.parse(request.authorMemberId))
            requireMemberInWorkspace(author, channel.workspaceId)
            requireNotBlank(request.body, "message body")

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
                workspaceId = channel.workspaceId,
                author = author
            )
            hydrateMessage(message)
        }
    }

    suspend fun replyToMessage(messageId: Uuid, request: PostMessageRequest): Message {
        return transactional {
            val parent = requireMessage(messageId)
            val channel = requireChannel(parent.channelId)
            val author = requireMember(Uuid.parse(request.authorMemberId))
            requireMemberInWorkspace(author, channel.workspaceId)
            requireNotBlank(request.body, "message body")

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
                workspaceId = channel.workspaceId,
                author = author
            )
            saveThreadActivityNotifications(
                reply = reply,
                rootMessageId = rootId,
                workspaceId = channel.workspaceId,
                author = author
            )
            hydrateMessage(reply)
        }
    }

    suspend fun toggleReaction(messageId: Uuid, memberId: Uuid, emoji: String): Message {
        return transactional {
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
        transactional {
            requireMember(memberId)
            mentionNotificationRepository.markRead(memberId, notificationIds)
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

    private fun sanitizePreview(body: String, preview: LinkPreview?): LinkPreview? {
        val candidate = preview ?: return null
        val normalizedUrl = candidate.url.trim()
        if (normalizedUrl.isBlank() || !body.contains(normalizedUrl)) {
            return null
        }
        return candidate.copy(url = normalizedUrl)
    }

    companion object {
        private val SLUG_REGEX = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
        private val MENTION_REGEX = Regex("(^|\\s)@([a-zA-Z0-9._-]+)")
    }

    private suspend fun <T> transactional(block: suspend () -> T): T {
        return transactionRunner.run(block)
    }

    private suspend fun saveMentionNotifications(
        message: Message,
        workspaceId: Uuid,
        author: WorkspaceMember
    ) {
        val mentionedMembers = workspaceRepository.listMembers(workspaceId)
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
    }

    private suspend fun saveThreadActivityNotifications(
        reply: Message,
        rootMessageId: Uuid,
        workspaceId: Uuid,
        author: WorkspaceMember
    ) {
        val rootMessage = requireMessage(rootMessageId)
        val participants = buildSet {
            add(rootMessage.authorMemberId)
            messageRepository.listThread(rootMessageId)
                .asSequence()
                .filter { it.createdAt < reply.createdAt }
                .mapTo(this) { it.authorMemberId }
        }.filter { it != author.id }

        if (participants.isEmpty()) {
            return
        }

        val mentionedUserIds = MENTION_REGEX.findAll(reply.body)
            .map { it.groupValues[2] }
            .toSet()
        val workspaceMembersById = workspaceRepository.listMembers(workspaceId).associateBy { it.id }
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
        }
    }

    private suspend fun hydrateMessage(message: Message): Message {
        return hydrateMessages(listOf(message)).single()
    }

    private suspend fun hydrateMessages(messages: List<Message>): List<Message> {
        if (messages.isEmpty()) {
            return messages
        }

        val reactionsByMessageId = messageReactionRepository.listReactions(messages.map { it.id })
            .groupBy { it.messageId }

        return messages.map { message ->
            message.copy(reactions = reactionsByMessageId[message.id].toReactionSummaries())
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
class MessagingQueryService(
    private val workspaceRepository: WorkspaceRepository,
    private val channelRepository: ChannelRepository,
    private val messageRepository: MessageRepository,
    private val messageReactionRepository: MessageReactionRepository,
    private val mentionNotificationRepository: MentionNotificationRepository,
    private val transactionRunner: TransactionRunner = ExposedTransactionRunner
) {
    suspend fun getWorkspace(workspaceId: Uuid): Workspace {
        return transactional {
            requireWorkspace(workspaceId)
        }
    }

    suspend fun listWorkspaces(): List<Workspace> {
        return transactional {
            workspaceRepository.listWorkspaces()
        }
    }

    suspend fun getWorkspaceBySlug(slug: String): Workspace {
        return transactional {
            val normalizedSlug = slug.trim()
            if (normalizedSlug.isBlank()) {
                throw DomainValidationException("workspace slug must not be blank")
            }
            workspaceRepository.findWorkspaceBySlug(normalizedSlug)
                ?: throw NotFoundException("workspace not found")
        }
    }

    suspend fun listMembers(workspaceId: Uuid): List<WorkspaceMember> {
        return transactional {
            requireWorkspace(workspaceId)
            workspaceRepository.listMembers(workspaceId)
        }
    }

    suspend fun listChannels(workspaceId: Uuid): List<Channel> {
        return transactional {
            requireWorkspace(workspaceId)
            channelRepository.listChannels(workspaceId)
        }
    }

    suspend fun listChannelMessages(channelId: Uuid, beforeMessageId: Uuid?, limit: Int): List<Message> {
        return transactional {
            if (limit !in 1..100) {
                throw DomainValidationException("limit must be between 1 and 100")
            }
            hydrateMessages(messageRepository.listChannelMessages(channelId, beforeMessageId, limit))
        }
    }

    suspend fun getThread(messageId: Uuid): Pair<Message, List<Message>> {
        return transactional {
            val target = messageRepository.findMessage(messageId)
                ?: throw NotFoundException("message not found")
            val root = target.threadRootMessageId?.let { messageRepository.findMessage(it) ?: throw NotFoundException("thread root not found") }
                ?: target
            val hydrated = hydrateMessages(listOf(root) + messageRepository.listThread(root.id))
            hydrated.first() to hydrated.drop(1)
        }
    }

    suspend fun listMemberNotifications(memberId: Uuid, unreadOnly: Boolean): List<MentionNotification> {
        return transactional {
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

    private suspend fun <T> transactional(block: suspend () -> T): T {
        return transactionRunner.run(block)
    }

    private suspend fun hydrateMessages(messages: List<Message>): List<Message> {
        if (messages.isEmpty()) {
            return messages
        }

        val reactionsByMessageId = messageReactionRepository.listReactions(messages.map { it.id })
            .groupBy { it.messageId }

        return messages.map { message ->
            message.copy(reactions = reactionsByMessageId[message.id].toReactionSummaries())
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun List<MessageReaction>?.toReactionSummaries(): List<MessageReactionSummary> {
    return this.orEmpty()
        .groupBy { it.emoji }
        .map { (emoji, reactions) ->
            MessageReactionSummary(
                emoji = emoji,
                count = reactions.size,
                memberIds = reactions.map { it.memberId }
            )
        }
        .sortedWith(compareByDescending<MessageReactionSummary> { it.count }.thenBy { it.emoji })
}

private fun com.retheviper.chat.contract.LinkPreviewResponse.toDomain(): LinkPreview {
    return LinkPreview(
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        siteName = siteName
    )
}

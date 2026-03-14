package com.retheviper.chat.messaging.infrastructure.persistence

import com.retheviper.chat.contract.ChannelVisibility
import com.retheviper.chat.contract.WorkspaceMemberRole
import com.retheviper.chat.messaging.domain.Channel
import com.retheviper.chat.messaging.domain.ChannelRepository
import com.retheviper.chat.messaging.domain.LinkPreview
import com.retheviper.chat.messaging.domain.Message
import com.retheviper.chat.messaging.domain.MessageReaction
import com.retheviper.chat.messaging.domain.MessageReactionRepository
import com.retheviper.chat.messaging.domain.MessageRepository
import com.retheviper.chat.messaging.domain.MentionNotification
import com.retheviper.chat.messaging.domain.MentionNotificationRepository
import com.retheviper.chat.messaging.domain.NotificationKind
import com.retheviper.chat.messaging.domain.NotFoundException
import com.retheviper.chat.messaging.domain.Workspace
import com.retheviper.chat.messaging.domain.WorkspaceMember
import com.retheviper.chat.messaging.domain.WorkspaceRepository
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update

@OptIn(ExperimentalUuidApi::class)
class DatabaseWorkspaceRepository : WorkspaceRepository {
    override suspend fun existsBySlug(slug: String): Boolean =
        WorkspacesTable.selectAll()
            .where { WorkspacesTable.slug eq slug }
            .limit(1)
            .toList()
            .singleOrNull() != null

    override suspend fun saveWorkspace(workspace: Workspace) {
        WorkspacesTable.insert { statement ->
            statement[id] = workspace.id
            statement[slug] = workspace.slug
            statement[name] = workspace.name
            statement[ownerMemberId] = workspace.ownerMemberId
            statement[createdAtEpochMillis] = workspace.createdAt.toEpochMilli()
        }
    }

    override suspend fun findWorkspace(id: Uuid): Workspace? =
        WorkspacesTable.selectAll()
            .where { WorkspacesTable.id eq id }
            .toList()
            .singleOrNull()
            ?.toWorkspace()

    override suspend fun findWorkspaceBySlug(slug: String): Workspace? =
        WorkspacesTable.selectAll()
            .where { WorkspacesTable.slug eq slug.trim() }
            .toList()
            .singleOrNull()
            ?.toWorkspace()

    override suspend fun listWorkspaces(): List<Workspace> =
        WorkspacesTable.selectAll()
            .orderBy(WorkspacesTable.createdAtEpochMillis to SortOrder.ASC)
            .toList()
            .map { it.toWorkspace() }

    override suspend fun saveMember(member: WorkspaceMember) {
        WorkspaceMembersTable.insert { statement ->
            statement[id] = member.id
            statement[workspaceId] = member.workspaceId
            statement[userId] = member.userId
            statement[displayName] = member.displayName
            statement[role] = member.role.name
            statement[joinedAtEpochMillis] = member.joinedAt.toEpochMilli()
        }
    }

    override suspend fun updateMemberDisplayName(memberId: Uuid, displayName: String) {
        WorkspaceMembersTable.update({ WorkspaceMembersTable.id eq memberId }) { statement ->
            statement[WorkspaceMembersTable.displayName] = displayName
        }
    }

    override suspend fun findMember(id: Uuid): WorkspaceMember? =
        WorkspaceMembersTable.selectAll()
            .where { WorkspaceMembersTable.id eq id }
            .toList()
            .singleOrNull()
            ?.toWorkspaceMember()

    override suspend fun findMemberByUserId(workspaceId: Uuid, userId: String): WorkspaceMember? =
        WorkspaceMembersTable.selectAll()
            .where { (WorkspaceMembersTable.workspaceId eq workspaceId) and (WorkspaceMembersTable.userId eq userId) }
            .toList()
            .singleOrNull()
            ?.toWorkspaceMember()

    override suspend fun listMembers(workspaceId: Uuid): List<WorkspaceMember> =
        WorkspaceMembersTable.selectAll()
            .where { WorkspaceMembersTable.workspaceId eq workspaceId }
            .orderBy(WorkspaceMembersTable.joinedAtEpochMillis to SortOrder.ASC)
            .toList()
            .map { it.toWorkspaceMember() }

    private fun ResultRow.toWorkspace(): Workspace = Workspace(
        id = this[WorkspacesTable.id],
        slug = this[WorkspacesTable.slug],
        name = this[WorkspacesTable.name],
        ownerMemberId = this[WorkspacesTable.ownerMemberId],
        createdAt = Instant.ofEpochMilli(this[WorkspacesTable.createdAtEpochMillis])
    )

    private fun ResultRow.toWorkspaceMember(): WorkspaceMember = WorkspaceMember(
        id = this[WorkspaceMembersTable.id],
        workspaceId = this[WorkspaceMembersTable.workspaceId],
        userId = this[WorkspaceMembersTable.userId],
        displayName = this[WorkspaceMembersTable.displayName],
        role = WorkspaceMemberRole.valueOf(this[WorkspaceMembersTable.role]),
        joinedAt = Instant.ofEpochMilli(this[WorkspaceMembersTable.joinedAtEpochMillis])
    )
}

@OptIn(ExperimentalUuidApi::class)
class DatabaseChannelRepository : ChannelRepository {
    override suspend fun existsBySlug(workspaceId: Uuid, slug: String): Boolean =
        ChannelsTable.selectAll()
            .where { (ChannelsTable.workspaceId eq workspaceId) and (ChannelsTable.slug eq slug) }
            .limit(1)
            .toList()
            .singleOrNull() != null

    override suspend fun saveChannel(channel: Channel) {
        ChannelsTable.insert { statement ->
            statement[id] = channel.id
            statement[workspaceId] = channel.workspaceId
            statement[slug] = channel.slug
            statement[name] = channel.name
            statement[topic] = channel.topic
            statement[visibility] = channel.visibility.name
            statement[createdByMemberId] = channel.createdByMemberId
            statement[createdAtEpochMillis] = channel.createdAt.toEpochMilli()
        }
    }

    override suspend fun findChannel(id: Uuid): Channel? =
        ChannelsTable.selectAll()
            .where { ChannelsTable.id eq id }
            .toList()
            .singleOrNull()
            ?.toChannel()

    override suspend fun listChannels(workspaceId: Uuid): List<Channel> =
        ChannelsTable.selectAll()
            .where { ChannelsTable.workspaceId eq workspaceId }
            .orderBy(ChannelsTable.createdAtEpochMillis to SortOrder.ASC)
            .toList()
            .map { it.toChannel() }

    private fun ResultRow.toChannel(): Channel = Channel(
        id = this[ChannelsTable.id],
        workspaceId = this[ChannelsTable.workspaceId],
        slug = this[ChannelsTable.slug],
        name = this[ChannelsTable.name],
        topic = this[ChannelsTable.topic],
        visibility = ChannelVisibility.valueOf(this[ChannelsTable.visibility]),
        createdByMemberId = this[ChannelsTable.createdByMemberId],
        createdAt = Instant.ofEpochMilli(this[ChannelsTable.createdAtEpochMillis])
    )
}

@OptIn(ExperimentalUuidApi::class)
class DatabaseMessageRepository : MessageRepository {
    override suspend fun saveMessage(message: Message) {
        MessagesTable.insert { statement ->
            statement[id] = message.id
            statement[channelId] = message.channelId
            statement[authorMemberId] = message.authorMemberId
            statement[body] = message.body
            statement[previewUrl] = message.linkPreview?.url
            statement[previewTitle] = message.linkPreview?.title
            statement[previewDescription] = message.linkPreview?.description
            statement[previewImageUrl] = message.linkPreview?.imageUrl
            statement[previewSiteName] = message.linkPreview?.siteName
            message.threadRootMessageId?.let { statement[threadRootMessageId] = it }
            statement[createdAtEpochMillis] = message.createdAt.toEpochMilli()
        }
    }

    override suspend fun findMessage(id: Uuid): Message? {
        val join = MessagesTable.join(
            otherTable = WorkspaceMembersTable,
            joinType = JoinType.INNER,
            additionalConstraint = { MessagesTable.authorMemberId eq WorkspaceMembersTable.id }
        )

        return join.selectAll()
            .where { MessagesTable.id eq id }
            .toList()
            .singleOrNull()
            ?.toMessage()
    }

    override suspend fun listChannelMessages(channelId: Uuid, beforeMessageId: Uuid?, limit: Int): List<Message> {
        val beforeTimestamp = beforeMessageId?.let { messageId ->
            findMessage(messageId)?.createdAt?.toEpochMilli()
                ?: throw NotFoundException("before message not found")
        }

        val join = MessagesTable.join(
            otherTable = WorkspaceMembersTable,
            joinType = JoinType.INNER,
            additionalConstraint = { MessagesTable.authorMemberId eq WorkspaceMembersTable.id }
        )
        val filter = if (beforeTimestamp == null) {
            (MessagesTable.channelId eq channelId) and MessagesTable.threadRootMessageId.isNull()
        } else {
            ((MessagesTable.channelId eq channelId) and MessagesTable.threadRootMessageId.isNull()) and
                (MessagesTable.createdAtEpochMillis less beforeTimestamp)
        }

        return join.selectAll()
            .where { filter }
            .orderBy(MessagesTable.createdAtEpochMillis to SortOrder.DESC)
            .limit(limit)
            .toList()
            .map { it.toMessage() }
            .map { message ->
                if (message.threadRootMessageId == null) {
                    message.copy(threadReplyCount = listThread(message.id).size)
                } else {
                    message
                }
            }
            .reversed()
    }

    override suspend fun listThread(rootMessageId: Uuid): List<Message> {
        val join = MessagesTable.join(
            otherTable = WorkspaceMembersTable,
            joinType = JoinType.INNER,
            additionalConstraint = { MessagesTable.authorMemberId eq WorkspaceMembersTable.id }
        )

        return join.selectAll()
            .where { MessagesTable.threadRootMessageId eq rootMessageId }
            .orderBy(MessagesTable.createdAtEpochMillis to SortOrder.ASC)
            .toList()
            .map { it.toMessage() }
    }

    private fun ResultRow.toMessage(): Message = Message(
        id = this[MessagesTable.id],
        channelId = this[MessagesTable.channelId],
        authorMemberId = this[MessagesTable.authorMemberId],
        authorDisplayName = this[WorkspaceMembersTable.displayName],
        body = this[MessagesTable.body],
        linkPreview = this[MessagesTable.previewUrl]?.let {
            LinkPreview(
                url = it,
                title = this[MessagesTable.previewTitle],
                description = this[MessagesTable.previewDescription],
                imageUrl = this[MessagesTable.previewImageUrl],
                siteName = this[MessagesTable.previewSiteName]
            )
        },
        threadRootMessageId = this[MessagesTable.threadRootMessageId],
        threadReplyCount = 0,
        createdAt = Instant.ofEpochMilli(this[MessagesTable.createdAtEpochMillis])
    )
}

@OptIn(ExperimentalUuidApi::class)
class DatabaseMessageReactionRepository : MessageReactionRepository {
    override suspend fun saveReaction(reaction: MessageReaction) {
        MessageReactionsTable.insert { statement ->
            statement[id] = reaction.id
            statement[messageId] = reaction.messageId
            statement[memberId] = reaction.memberId
            statement[emoji] = reaction.emoji
            statement[createdAtEpochMillis] = reaction.createdAt.toEpochMilli()
        }
    }

    override suspend fun findReaction(messageId: Uuid, memberId: Uuid, emoji: String): MessageReaction? =
        MessageReactionsTable.selectAll()
            .where {
                (MessageReactionsTable.messageId eq messageId) and
                    (MessageReactionsTable.memberId eq memberId) and
                    (MessageReactionsTable.emoji eq emoji)
            }
            .toList()
            .singleOrNull()
            ?.toMessageReaction()

    override suspend fun deleteReaction(reactionId: Uuid) {
        MessageReactionsTable.deleteWhere { MessageReactionsTable.id eq reactionId }
    }

    override suspend fun listReactions(messageIds: List<Uuid>): List<MessageReaction> {
        if (messageIds.isEmpty()) {
            return emptyList()
        }

        return MessageReactionsTable.selectAll()
            .where { MessageReactionsTable.messageId inList messageIds }
            .orderBy(MessageReactionsTable.createdAtEpochMillis to SortOrder.ASC)
            .toList()
            .map { it.toMessageReaction() }
    }

    private fun ResultRow.toMessageReaction(): MessageReaction = MessageReaction(
        id = this[MessageReactionsTable.id],
        messageId = this[MessageReactionsTable.messageId],
        memberId = this[MessageReactionsTable.memberId],
        emoji = this[MessageReactionsTable.emoji],
        createdAt = Instant.ofEpochMilli(this[MessageReactionsTable.createdAtEpochMillis])
    )
}

@OptIn(ExperimentalUuidApi::class)
class DatabaseMentionNotificationRepository : MentionNotificationRepository {
    override suspend fun saveNotifications(notifications: List<MentionNotification>) {
        notifications.forEach { notification ->
            MentionNotificationsTable.insert { statement ->
                statement[id] = notification.id
                statement[kind] = notification.kind.name
                statement[memberId] = notification.memberId
                statement[channelId] = notification.channelId
                statement[messageId] = notification.messageId
                notification.threadRootMessageId?.let { statement[threadRootMessageId] = it }
                statement[authorDisplayName] = notification.authorDisplayName
                statement[messagePreview] = notification.messagePreview
                statement[createdAtEpochMillis] = notification.createdAt.toEpochMilli()
                statement[readAtEpochMillis] = notification.readAt?.toEpochMilli()
            }
        }
    }

    override suspend fun listMemberNotifications(memberId: Uuid, unreadOnly: Boolean): List<MentionNotification> {
        val filter = if (unreadOnly) {
            (MentionNotificationsTable.memberId eq memberId) and MentionNotificationsTable.readAtEpochMillis.isNull()
        } else {
            MentionNotificationsTable.memberId eq memberId
        }

        return MentionNotificationsTable.selectAll()
            .where { filter }
            .orderBy(MentionNotificationsTable.createdAtEpochMillis to SortOrder.DESC)
            .toList()
            .map { row ->
                MentionNotification(
                    id = row[MentionNotificationsTable.id],
                    kind = NotificationKind.valueOf(row[MentionNotificationsTable.kind]),
                    memberId = row[MentionNotificationsTable.memberId],
                    channelId = row[MentionNotificationsTable.channelId],
                    messageId = row[MentionNotificationsTable.messageId],
                    threadRootMessageId = row[MentionNotificationsTable.threadRootMessageId],
                    authorDisplayName = row[MentionNotificationsTable.authorDisplayName],
                    messagePreview = row[MentionNotificationsTable.messagePreview],
                    createdAt = Instant.ofEpochMilli(row[MentionNotificationsTable.createdAtEpochMillis]),
                    readAt = row[MentionNotificationsTable.readAtEpochMillis]?.let(Instant::ofEpochMilli)
                )
            }
    }

    override suspend fun markRead(memberId: Uuid, notificationIds: List<Uuid>, readAt: Instant) {
        if (notificationIds.isEmpty()) {
            return
        }

        MentionNotificationsTable.update({
            (MentionNotificationsTable.memberId eq memberId) and (MentionNotificationsTable.id inList notificationIds)
        }) { statement ->
            statement[readAtEpochMillis] = readAt.toEpochMilli()
        }
    }
}

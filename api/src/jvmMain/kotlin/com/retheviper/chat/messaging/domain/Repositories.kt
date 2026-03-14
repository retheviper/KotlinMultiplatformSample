package com.retheviper.chat.messaging.domain

import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface WorkspaceRepository {
    suspend fun existsBySlug(slug: String): Boolean
    suspend fun saveWorkspace(workspace: Workspace)
    suspend fun findWorkspace(id: Uuid): Workspace?
    suspend fun findWorkspaceBySlug(slug: String): Workspace?
    suspend fun listWorkspaces(): List<Workspace>
    suspend fun saveMember(member: WorkspaceMember)
    suspend fun updateMemberDisplayName(memberId: Uuid, displayName: String)
    suspend fun findMember(id: Uuid): WorkspaceMember?
    suspend fun findMemberByUserId(workspaceId: Uuid, userId: String): WorkspaceMember?
    suspend fun listMembers(workspaceId: Uuid): List<WorkspaceMember>
}

@OptIn(ExperimentalUuidApi::class)
interface ChannelRepository {
    suspend fun existsBySlug(workspaceId: Uuid, slug: String): Boolean
    suspend fun saveChannel(channel: Channel)
    suspend fun findChannel(id: Uuid): Channel?
    suspend fun listChannels(workspaceId: Uuid): List<Channel>
}

@OptIn(ExperimentalUuidApi::class)
interface MessageRepository {
    suspend fun saveMessage(message: Message)
    suspend fun findMessage(id: Uuid): Message?
    suspend fun listChannelMessages(channelId: Uuid, beforeMessageId: Uuid?, limit: Int): List<Message>
    suspend fun listThread(rootMessageId: Uuid): List<Message>
}

@OptIn(ExperimentalUuidApi::class)
interface MessageReactionRepository {
    suspend fun saveReaction(reaction: MessageReaction)
    suspend fun findReaction(messageId: Uuid, memberId: Uuid, emoji: String): MessageReaction?
    suspend fun deleteReaction(reactionId: Uuid)
    suspend fun listReactions(messageIds: List<Uuid>): List<MessageReaction>
}

@OptIn(ExperimentalUuidApi::class)
interface MentionNotificationRepository {
    suspend fun saveNotifications(notifications: List<MentionNotification>)
    suspend fun listMemberNotifications(memberId: Uuid, unreadOnly: Boolean): List<MentionNotification>
    suspend fun listThreadSubscriberIds(rootMessageId: Uuid): Set<Uuid>
    suspend fun markRead(memberId: Uuid, notificationIds: List<Uuid>, readAt: Instant = Instant.now())
}

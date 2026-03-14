package com.retheviper.chat.messaging.domain

import com.retheviper.chat.contract.ChannelVisibility
import com.retheviper.chat.contract.WorkspaceMemberRole
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class NotificationKind {
    MENTION,
    THREAD_ACTIVITY
}

@OptIn(ExperimentalUuidApi::class)
data class Workspace(
    val id: Uuid,
    val slug: String,
    val name: String,
    val ownerMemberId: Uuid,
    val createdAt: Instant
)

@OptIn(ExperimentalUuidApi::class)
data class WorkspaceMember(
    val id: Uuid,
    val workspaceId: Uuid,
    val userId: String,
    val displayName: String,
    val role: WorkspaceMemberRole,
    val joinedAt: Instant
)

@OptIn(ExperimentalUuidApi::class)
data class Channel(
    val id: Uuid,
    val workspaceId: Uuid,
    val slug: String,
    val name: String,
    val topic: String?,
    val visibility: ChannelVisibility,
    val createdByMemberId: Uuid,
    val createdAt: Instant
)

@OptIn(ExperimentalUuidApi::class)
data class Message(
    val id: Uuid,
    val channelId: Uuid,
    val authorMemberId: Uuid,
    val authorDisplayName: String,
    val body: String,
    val linkPreview: LinkPreview? = null,
    val threadRootMessageId: Uuid?,
    val threadReplyCount: Int = 0,
    val reactions: List<MessageReactionSummary> = emptyList(),
    val createdAt: Instant
)

data class LinkPreview(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val siteName: String? = null
)

@OptIn(ExperimentalUuidApi::class)
data class MessageReaction(
    val id: Uuid,
    val messageId: Uuid,
    val memberId: Uuid,
    val emoji: String,
    val createdAt: Instant
)

@OptIn(ExperimentalUuidApi::class)
data class MessageReactionSummary(
    val emoji: String,
    val count: Int,
    val memberIds: List<Uuid>
)

@OptIn(ExperimentalUuidApi::class)
data class MentionNotification(
    val id: Uuid,
    val kind: NotificationKind,
    val memberId: Uuid,
    val channelId: Uuid,
    val messageId: Uuid,
    val threadRootMessageId: Uuid?,
    val authorDisplayName: String,
    val messagePreview: String,
    val createdAt: Instant,
    val readAt: Instant? = null
)

class DomainValidationException(message: String) : RuntimeException(message)

class NotFoundException(message: String) : RuntimeException(message)

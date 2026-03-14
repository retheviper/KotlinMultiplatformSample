@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.retheviper.chat.messaging.presentation

import com.retheviper.chat.contract.ChannelResponse
import com.retheviper.chat.contract.LinkPreviewResponse
import com.retheviper.chat.contract.MentionNotificationResponse
import com.retheviper.chat.contract.MessageReactionResponse
import com.retheviper.chat.contract.MessageResponse
import com.retheviper.chat.contract.NotificationKind
import com.retheviper.chat.contract.WorkspaceMemberResponse
import com.retheviper.chat.contract.WorkspaceResponse
import com.retheviper.chat.messaging.domain.Channel
import com.retheviper.chat.messaging.domain.LinkPreview
import com.retheviper.chat.messaging.domain.Message
import com.retheviper.chat.messaging.domain.MessageReactionSummary
import com.retheviper.chat.messaging.domain.MentionNotification
import com.retheviper.chat.messaging.domain.Workspace
import com.retheviper.chat.messaging.domain.WorkspaceMember
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.uuid.Uuid

internal fun Workspace.toResponse(): WorkspaceResponse = WorkspaceResponse(
    id = id.toString(),
    slug = slug,
    name = name,
    ownerMemberId = ownerMemberId.toString(),
    createdAt = createdAt.toApiString()
)

internal fun WorkspaceMember.toResponse(): WorkspaceMemberResponse = WorkspaceMemberResponse(
    id = id.toString(),
    workspaceId = workspaceId.toString(),
    userId = userId,
    displayName = displayName,
    role = role,
    joinedAt = joinedAt.toApiString()
)

internal fun Channel.toResponse(): ChannelResponse = ChannelResponse(
    id = id.toString(),
    workspaceId = workspaceId.toString(),
    slug = slug,
    name = name,
    topic = topic,
    visibility = visibility,
    createdByMemberId = createdByMemberId.toString(),
    createdAt = createdAt.toApiString()
)

internal fun Message.toResponse(): MessageResponse = MessageResponse(
    id = id.toString(),
    channelId = channelId.toString(),
    authorMemberId = authorMemberId.toString(),
    authorDisplayName = authorDisplayName,
    body = body,
    linkPreview = linkPreview?.toResponse(),
    threadRootMessageId = threadRootMessageId?.toString(),
    threadReplyCount = threadReplyCount,
    reactions = reactions.map { it.toResponse() },
    createdAt = createdAt.toApiString()
)

internal fun LinkPreview.toResponse(): LinkPreviewResponse = LinkPreviewResponse(
    url = url,
    title = title,
    description = description,
    imageUrl = imageUrl,
    siteName = siteName
)

internal fun MessageReactionSummary.toResponse(): MessageReactionResponse = MessageReactionResponse(
    emoji = emoji,
    count = count,
    memberIds = memberIds.map(Uuid::toString)
)

internal fun MentionNotification.toResponse(): MentionNotificationResponse = MentionNotificationResponse(
    id = id.toString(),
    kind = NotificationKind.valueOf(kind.name),
    memberId = memberId.toString(),
    channelId = channelId.toString(),
    messageId = messageId.toString(),
    threadRootMessageId = threadRootMessageId?.toString(),
    authorDisplayName = authorDisplayName,
    messagePreview = messagePreview,
    createdAt = createdAt.toApiString(),
    readAt = readAt?.toApiString()
)

internal fun java.time.Instant.toApiString(): String {
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(atOffset(ZoneOffset.UTC))
}

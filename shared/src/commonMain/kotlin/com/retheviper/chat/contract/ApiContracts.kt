package com.retheviper.chat.contract

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val code: String,
    val message: String
)

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String
)

@Serializable
enum class WorkspaceMemberRole {
    OWNER,
    ADMIN,
    MEMBER
}

@Serializable
enum class ChannelVisibility {
    PUBLIC,
    PRIVATE
}

@Serializable
data class CreateWorkspaceRequest(
    val slug: String,
    val name: String,
    val ownerUserId: String,
    val ownerDisplayName: String
)

@Serializable
data class WorkspaceResponse(
    val id: String,
    val slug: String,
    val name: String,
    val ownerMemberId: String,
    val createdAt: String
)

@Serializable
data class AddWorkspaceMemberRequest(
    val userId: String,
    val displayName: String,
    val role: WorkspaceMemberRole = WorkspaceMemberRole.MEMBER
)

@Serializable
data class UpdateWorkspaceMemberRequest(
    val displayName: String
)

@Serializable
data class WorkspaceMemberResponse(
    val id: String,
    val workspaceId: String,
    val userId: String,
    val displayName: String,
    val role: WorkspaceMemberRole,
    val joinedAt: String
)

@Serializable
data class CreateChannelRequest(
    val slug: String,
    val name: String,
    val topic: String? = null,
    val visibility: ChannelVisibility = ChannelVisibility.PUBLIC,
    val createdByMemberId: String
)

@Serializable
data class ChannelResponse(
    val id: String,
    val workspaceId: String,
    val slug: String,
    val name: String,
    val topic: String?,
    val visibility: ChannelVisibility,
    val createdByMemberId: String,
    val createdAt: String
)

@Serializable
data class PostMessageRequest(
    val authorMemberId: String,
    val body: String,
    val linkPreview: LinkPreviewResponse? = null
)

@Serializable
data class ResolveLinkPreviewRequest(
    val url: String
)

@Serializable
data class ResolveLinkPreviewResponse(
    val preview: LinkPreviewResponse? = null
)

@Serializable
data class LinkPreviewResponse(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val siteName: String? = null
)

@Serializable
data class ToggleReactionRequest(
    val memberId: String,
    val emoji: String
)

@Serializable
data class MarkNotificationsReadRequest(
    val notificationIds: List<String>
)

@Serializable
data class MessageResponse(
    val id: String,
    val channelId: String,
    val authorMemberId: String,
    val authorDisplayName: String,
    val body: String,
    val linkPreview: LinkPreviewResponse? = null,
    val threadRootMessageId: String?,
    val threadReplyCount: Int = 0,
    val reactions: List<MessageReactionResponse> = emptyList(),
    val createdAt: String
)

@Serializable
data class MessageReactionResponse(
    val emoji: String,
    val count: Int,
    val memberIds: List<String>
)

@Serializable
enum class NotificationKind {
    MENTION,
    THREAD_ACTIVITY
}

@Serializable
data class MentionNotificationResponse(
    val id: String,
    val kind: NotificationKind,
    val memberId: String,
    val channelId: String,
    val messageId: String,
    val threadRootMessageId: String?,
    val authorDisplayName: String,
    val messagePreview: String,
    val createdAt: String,
    val readAt: String? = null
)

@Serializable
data class MessagePageResponse(
    val messages: List<MessageResponse>
)

@Serializable
data class ThreadResponse(
    val root: MessageResponse,
    val replies: List<MessageResponse>
)

@Serializable
enum class ChatCommandType {
    LOAD_RECENT,
    POST_MESSAGE,
    REPLY_MESSAGE,
    TOGGLE_REACTION
}

@Serializable
data class ChatCommand(
    val type: ChatCommandType,
    val authorMemberId: String? = null,
    val body: String? = null,
    val linkPreview: LinkPreviewResponse? = null,
    val parentMessageId: String? = null,
    val messageId: String? = null,
    val emoji: String? = null,
    val limit: Int? = null
)

@Serializable
enum class ChatEventType {
    SNAPSHOT,
    MESSAGE_POSTED,
    REPLY_POSTED,
    REACTION_UPDATED,
    ERROR
}

@Serializable
data class ChatEvent(
    val type: ChatEventType,
    val message: MessageResponse? = null,
    val messages: List<MessageResponse> = emptyList(),
    val error: ApiErrorResponse? = null
)

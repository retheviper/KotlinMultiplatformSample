package com.retheviper.chat.app

import com.retheviper.chat.contract.AddWorkspaceMemberRequest
import com.retheviper.chat.contract.ChatCommand
import com.retheviper.chat.contract.ChatCommandType
import com.retheviper.chat.contract.ChatEvent
import com.retheviper.chat.contract.ChatEventType
import com.retheviper.chat.contract.LinkPreviewResponse
import com.retheviper.chat.contract.MessageResponse
import com.retheviper.chat.contract.ThreadResponse
import com.retheviper.chat.contract.WorkspaceMemberResponse

data class MentionDraft(
    val query: String,
    val startIndex: Int
)

data class WorkspaceJoinPlan(
    val existingMember: WorkspaceMemberResponse? = null,
    val createRequest: AddWorkspaceMemberRequest? = null
)

data class ChatFeedState(
    val messages: List<MessageResponse> = emptyList()
)

fun reduceChatFeed(current: ChatFeedState, event: ChatEvent): ChatFeedState {
    val nextMessages = when (event.type) {
        ChatEventType.SNAPSHOT -> event.messages
        ChatEventType.MESSAGE_POSTED -> event.message?.let(current.messages::plus) ?: current.messages
        ChatEventType.REACTION_UPDATED -> event.message?.let { current.messages.replaceMessage(it) } ?: current.messages
        ChatEventType.REPLY_POSTED -> {
            val rootId = event.message?.threadRootMessageId
            if (rootId == null) {
                current.messages
            } else {
                current.messages.map { message ->
                    if (message.id == rootId) {
                        message.copy(threadReplyCount = message.threadReplyCount + 1)
                    } else {
                        message
                    }
                }
            }
        }
        ChatEventType.ERROR -> current.messages
    }
    return current.copy(messages = nextMessages)
}

fun List<MessageResponse>.replaceMessage(updated: MessageResponse): List<MessageResponse> {
    return map { current ->
        if (current.id == updated.id) updated else current
    }
}

fun buildOutgoingChatCommand(
    primaryAuthorId: String?,
    body: String,
    replyParentMessageId: String,
    linkPreview: LinkPreviewResponse? = null
): ChatCommand? {
    val trimmedBody = body.trim()
    if (trimmedBody.isBlank()) {
        return null
    }

    val trimmedParentId = replyParentMessageId.trim()
    val isReply = trimmedParentId.isNotEmpty()
    val authorId = primaryAuthorId ?: return null

    return if (isReply) {
        ChatCommand(
            type = ChatCommandType.REPLY_MESSAGE,
            authorMemberId = authorId,
            body = trimmedBody,
            linkPreview = linkPreview,
            parentMessageId = trimmedParentId
        )
    } else {
        ChatCommand(
            type = ChatCommandType.POST_MESSAGE,
            authorMemberId = authorId,
            body = trimmedBody,
            linkPreview = linkPreview
        )
    }
}

fun toThreadMessages(thread: ThreadResponse): List<MessageResponse> {
    return buildList {
        add(thread.root)
        addAll(thread.replies)
    }
}

fun findActiveMention(text: String): MentionDraft? {
    val atIndex = text.lastIndexOf('@')
    if (atIndex < 0) return null
    val suffix = text.substring(atIndex + 1)
    if (suffix.contains(' ') || suffix.contains('\n') || suffix.contains('\t')) {
        return null
    }
    return MentionDraft(query = suffix, startIndex = atIndex)
}

fun suggestMentionCandidates(
    members: List<WorkspaceMemberResponse>,
    text: String
): List<WorkspaceMemberResponse> {
    val mention = findActiveMention(text) ?: return emptyList()
    val query = mention.query.trim().lowercase()
    return members
        .filter {
            query.isBlank() ||
                it.userId.lowercase().contains(query) ||
                it.displayName.lowercase().contains(query)
        }
        .take(5)
}

fun applyMentionCandidate(text: String, userId: String): String {
    val mention = findActiveMention(text) ?: return text
    return text.substring(0, mention.startIndex) + "@$userId "
}

fun planWorkspaceJoin(
    members: List<WorkspaceMemberResponse>,
    userId: String,
    displayName: String
): WorkspaceJoinPlan? {
    val trimmedUserId = userId.trim()
    if (trimmedUserId.isBlank()) {
        return null
    }

    val existingMember = members.firstOrNull { it.userId == trimmedUserId }
    if (existingMember != null) {
        return WorkspaceJoinPlan(existingMember = existingMember)
    }

    val trimmedDisplayName = displayName.trim()
    if (trimmedDisplayName.isBlank()) {
        return null
    }

    return WorkspaceJoinPlan(
        createRequest = AddWorkspaceMemberRequest(
            userId = trimmedUserId,
            displayName = trimmedDisplayName
        )
    )
}

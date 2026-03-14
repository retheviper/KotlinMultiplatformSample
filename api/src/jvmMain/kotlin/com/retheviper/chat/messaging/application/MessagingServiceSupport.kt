@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.retheviper.chat.messaging.application

import com.retheviper.chat.messaging.domain.LinkPreview
import com.retheviper.chat.messaging.domain.Message
import com.retheviper.chat.messaging.domain.MessageReaction
import com.retheviper.chat.messaging.domain.MessageReactionRepository
import com.retheviper.chat.messaging.domain.MessageReactionSummary
internal suspend fun hydrateMessages(
    messages: List<Message>,
    messageReactionRepository: MessageReactionRepository
): List<Message> {
    if (messages.isEmpty()) {
        return messages
    }

    val reactionsByMessageId = messageReactionRepository.listReactions(messages.map { it.id })
        .groupBy { it.messageId }

    return messages.map { message ->
        message.copy(reactions = reactionsByMessageId[message.id].toReactionSummaries())
    }
}

internal fun sanitizePreview(body: String, preview: LinkPreview?): LinkPreview? {
    val candidate = preview ?: return null
    val normalizedUrl = candidate.url.trim()
    if (normalizedUrl.isBlank() || !body.contains(normalizedUrl)) {
        return null
    }
    return candidate.copy(url = normalizedUrl)
}

internal fun List<MessageReaction>?.toReactionSummaries(): List<MessageReactionSummary> {
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

internal fun com.retheviper.chat.contract.LinkPreviewResponse.toDomain(): LinkPreview {
    return LinkPreview(
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        siteName = siteName
    )
}

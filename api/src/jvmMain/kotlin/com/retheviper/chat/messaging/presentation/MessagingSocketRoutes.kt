@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.retheviper.chat.messaging.presentation

import com.retheviper.chat.contract.ChatCommand
import com.retheviper.chat.contract.ChatCommandType
import com.retheviper.chat.contract.ChatEvent
import com.retheviper.chat.contract.ChatEventType
import com.retheviper.chat.contract.PostMessageRequest
import com.retheviper.chat.wiring.ApplicationDependencies
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlin.uuid.Uuid

internal fun Route.configureChatSocketRoutes(
    dependencies: ApplicationDependencies,
    sessions: ChannelSessions
) {
    webSocket("/ws/channels/{channelId}") {
        val channelId = call.channelId()
        sessions.join(channelId, this)
        try {
            try {
                while (true) {
                    val command = receiveDeserialized<ChatCommand>()
                    handleCommand(channelId, command, dependencies, sessions)
                }
            } catch (_: ClosedReceiveChannelException) {
            }
        } finally {
            sessions.leave(channelId, this)
        }
    }
}

private suspend fun DefaultWebSocketServerSession.handleCommand(
    channelId: Uuid,
    command: ChatCommand,
    dependencies: ApplicationDependencies,
    sessions: ChannelSessions
) {
    when (command.type) {
        ChatCommandType.LOAD_RECENT -> {
            val messages = dependencies.queryService
                .listChannelMessages(channelId, beforeMessageId = null, limit = command.limit ?: 50)
                .map { it.toResponse() }
            sendSerialized(ChatEvent(type = ChatEventType.SNAPSHOT, messages = messages))
        }

        ChatCommandType.POST_MESSAGE -> {
            val message = dependencies.commandService
                .postChannelMessage(
                    channelId = channelId,
                    request = PostMessageRequest(
                        authorMemberId = requireNotNull(command.authorMemberId),
                        body = requireNotNull(command.body),
                        linkPreview = command.linkPreview
                    )
                )
                .toResponse()
            sessions.broadcast(channelId, ChatEvent(type = ChatEventType.MESSAGE_POSTED, message = message))
        }

        ChatCommandType.REPLY_MESSAGE -> {
            val parentId = Uuid.parse(requireNotNull(command.parentMessageId))
            val reply = dependencies.commandService
                .replyToMessage(
                    messageId = parentId,
                    request = PostMessageRequest(
                        authorMemberId = requireNotNull(command.authorMemberId),
                        body = requireNotNull(command.body),
                        linkPreview = command.linkPreview
                    )
                )
                .toResponse()
            sessions.broadcast(channelId, ChatEvent(type = ChatEventType.REPLY_POSTED, message = reply))
        }

        ChatCommandType.TOGGLE_REACTION -> {
            val updatedMessage = dependencies.commandService
                .toggleReaction(
                    messageId = Uuid.parse(requireNotNull(command.messageId)),
                    memberId = Uuid.parse(requireNotNull(command.authorMemberId)),
                    emoji = requireNotNull(command.emoji)
                )
                .toResponse()
            sessions.broadcast(channelId, ChatEvent(type = ChatEventType.REACTION_UPDATED, message = updatedMessage))
        }
    }
}

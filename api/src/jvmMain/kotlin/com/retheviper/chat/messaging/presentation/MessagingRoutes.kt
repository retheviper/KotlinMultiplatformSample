@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.retheviper.chat.messaging.presentation

import com.retheviper.chat.contract.AddWorkspaceMemberRequest
import com.retheviper.chat.contract.ChannelResponse
import com.retheviper.chat.contract.ChatCommand
import com.retheviper.chat.contract.ChatCommandType
import com.retheviper.chat.contract.ChatEvent
import com.retheviper.chat.contract.ChatEventType
import com.retheviper.chat.contract.CreateChannelRequest
import com.retheviper.chat.contract.CreateWorkspaceRequest
import com.retheviper.chat.contract.MarkNotificationsReadRequest
import com.retheviper.chat.contract.MessagePageResponse
import com.retheviper.chat.contract.MessageResponse
import com.retheviper.chat.contract.MentionNotificationResponse
import com.retheviper.chat.contract.NotificationKind
import com.retheviper.chat.contract.ResolveLinkPreviewRequest
import com.retheviper.chat.contract.ResolveLinkPreviewResponse
import com.retheviper.chat.contract.ThreadResponse
import com.retheviper.chat.contract.ToggleReactionRequest
import com.retheviper.chat.contract.UpdateWorkspaceMemberRequest
import com.retheviper.chat.contract.WorkspaceMemberResponse
import com.retheviper.chat.contract.WorkspaceResponse
import com.retheviper.chat.messaging.domain.Channel
import com.retheviper.chat.messaging.domain.LinkPreview
import com.retheviper.chat.messaging.domain.Message
import com.retheviper.chat.messaging.domain.MessageReactionSummary
import com.retheviper.chat.messaging.domain.MentionNotification
import com.retheviper.chat.messaging.domain.Workspace
import com.retheviper.chat.messaging.domain.WorkspaceMember
import com.retheviper.chat.wiring.ApplicationDependencies
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlin.uuid.Uuid

fun Application.configureMessagingRouting(dependencies: ApplicationDependencies) {
    val sessions = ChannelSessions()
    routing {
        route("/api/v1") {
            get("/workspaces") {
                call.respond(dependencies.queryService.listWorkspaces().map { it.toResponse() })
            }

            post("/workspaces") {
                val request = call.receive<CreateWorkspaceRequest>()
                call.respond(HttpStatusCode.Created, dependencies.commandService.createWorkspace(request).toResponse())
            }

            get("/workspaces/by-slug/{slug}") {
                val slug = call.parameters["slug"] ?: error("slug is required")
                call.respond(dependencies.queryService.getWorkspaceBySlug(slug).toResponse())
            }

            get("/workspaces/{workspaceId}") {
                call.respond(dependencies.queryService.getWorkspace(call.workspaceId()).toResponse())
            }

            post("/workspaces/{workspaceId}/members") {
                val request = call.receive<AddWorkspaceMemberRequest>()
                call.respond(HttpStatusCode.Created, dependencies.commandService.addMember(call.workspaceId(), request).toResponse())
            }

            get("/workspaces/{workspaceId}/members") {
                call.respond(dependencies.queryService.listMembers(call.workspaceId()).map { it.toResponse() })
            }

            put("/members/{memberId}") {
                val request = call.receive<UpdateWorkspaceMemberRequest>()
                call.respond(dependencies.commandService.updateMember(call.memberId(), request).toResponse())
            }

            post("/workspaces/{workspaceId}/channels") {
                val request = call.receive<CreateChannelRequest>()
                call.respond(HttpStatusCode.Created, dependencies.commandService.createChannel(call.workspaceId(), request).toResponse())
            }

            get("/workspaces/{workspaceId}/channels") {
                call.respond(dependencies.queryService.listChannels(call.workspaceId()).map { it.toResponse() })
            }

            get("/channels/{channelId}/messages") {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val beforeMessageId = call.request.queryParameters["beforeMessageId"]?.let(Uuid::parse)
                call.respond(MessagePageResponse(dependencies.queryService.listChannelMessages(call.channelId(), beforeMessageId, limit).map { it.toResponse() }))
            }

            get("/messages/{messageId}/thread") {
                val (root, replies) = dependencies.queryService.getThread(call.messageId())
                call.respond(ThreadResponse(root = root.toResponse(), replies = replies.map { it.toResponse() }))
            }

            post("/messages/{messageId}/reactions/toggle") {
                val request = call.receive<ToggleReactionRequest>()
                val updated = dependencies.commandService.toggleReaction(
                    messageId = call.messageId(),
                    memberId = Uuid.parse(request.memberId),
                    emoji = request.emoji
                )
                call.respond(updated.toResponse())
            }

            get("/members/{memberId}/notifications") {
                val unreadOnly = call.request.queryParameters["unreadOnly"]?.toBooleanStrictOrNull() ?: true
                call.respond(
                    dependencies.queryService.listMemberNotifications(call.memberId(), unreadOnly).map { it.toResponse() }
                )
            }

            post("/members/{memberId}/notifications/read") {
                val request = call.receive<MarkNotificationsReadRequest>()
                dependencies.commandService.markNotificationsRead(
                    memberId = call.memberId(),
                    notificationIds = request.notificationIds.map(Uuid::parse)
                )
                call.respond(HttpStatusCode.OK)
            }

            post("/link-preview/resolve") {
                val request = call.receive<ResolveLinkPreviewRequest>()
                call.respond(
                    ResolveLinkPreviewResponse(
                        preview = dependencies.linkPreviewResolver.resolve(request.url)?.toResponse()
                    )
                )
            }

            get("/link-preview/image") {
                val url = call.request.queryParameters["url"] ?: error("url is required")
                val image = dependencies.linkPreviewResolver.fetchImage(url)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respondBytes(
                    bytes = image.bytes,
                    contentType = ContentType.parse(image.contentType)
                )
            }
        }

        webSocket("/ws/channels/{channelId}") {
            val channelId = call.channelId()
            sessions.join(channelId, this)
            try {
                try {
                    while (true) {
                        when (val command = receiveDeserialized<ChatCommand>()) {
                            is ChatCommand -> handleCommand(channelId, command, dependencies, sessions)
                        }
                    }
                } catch (_: ClosedReceiveChannelException) {
                }
            } finally {
                sessions.leave(channelId, this)
            }
        }
    }
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.handleCommand(
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
                    request = com.retheviper.chat.contract.PostMessageRequest(
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
                    request = com.retheviper.chat.contract.PostMessageRequest(
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

private class ChannelSessions {
    private val sessions = ConcurrentHashMap<Uuid, CopyOnWriteArraySet<io.ktor.server.websocket.DefaultWebSocketServerSession>>()

    fun join(channelId: Uuid, session: io.ktor.server.websocket.DefaultWebSocketServerSession) {
        sessions.computeIfAbsent(channelId) { CopyOnWriteArraySet() }.add(session)
    }

    fun leave(channelId: Uuid, session: io.ktor.server.websocket.DefaultWebSocketServerSession) {
        sessions[channelId]?.remove(session)
    }

    suspend fun broadcast(channelId: Uuid, event: ChatEvent) {
        sessions[channelId]?.forEach { session ->
            session.sendSerialized(event)
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.workspaceId(): Uuid =
    Uuid.parse(parameters["workspaceId"] ?: error("workspaceId is required"))

private fun io.ktor.server.application.ApplicationCall.channelId(): Uuid =
    Uuid.parse(parameters["channelId"] ?: error("channelId is required"))

private fun io.ktor.server.application.ApplicationCall.messageId(): Uuid =
    Uuid.parse(parameters["messageId"] ?: error("messageId is required"))

private fun io.ktor.server.application.ApplicationCall.memberId(): Uuid =
    Uuid.parse(parameters["memberId"] ?: error("memberId is required"))

private fun Workspace.toResponse(): WorkspaceResponse = WorkspaceResponse(
    id = id.toString(),
    slug = slug,
    name = name,
    ownerMemberId = ownerMemberId.toString(),
    createdAt = createdAt.toApiString()
)

private fun WorkspaceMember.toResponse(): WorkspaceMemberResponse = WorkspaceMemberResponse(
    id = id.toString(),
    workspaceId = workspaceId.toString(),
    userId = userId,
    displayName = displayName,
    role = role,
    joinedAt = joinedAt.toApiString()
)

private fun Channel.toResponse(): ChannelResponse = ChannelResponse(
    id = id.toString(),
    workspaceId = workspaceId.toString(),
    slug = slug,
    name = name,
    topic = topic,
    visibility = visibility,
    createdByMemberId = createdByMemberId.toString(),
    createdAt = createdAt.toApiString()
)

private fun Message.toResponse(): MessageResponse = MessageResponse(
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

private fun LinkPreview.toResponse(): com.retheviper.chat.contract.LinkPreviewResponse =
    com.retheviper.chat.contract.LinkPreviewResponse(
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        siteName = siteName
    )

private fun MessageReactionSummary.toResponse(): com.retheviper.chat.contract.MessageReactionResponse =
    com.retheviper.chat.contract.MessageReactionResponse(
        emoji = emoji,
        count = count,
        memberIds = memberIds.map(Uuid::toString)
    )

private fun MentionNotification.toResponse(): MentionNotificationResponse = MentionNotificationResponse(
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

private fun java.time.Instant.toApiString(): String {
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(atOffset(ZoneOffset.UTC))
}

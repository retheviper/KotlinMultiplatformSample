package com.retheviper.chat.client

import com.retheviper.chat.contract.AddWorkspaceMemberRequest
import com.retheviper.chat.contract.ApiErrorResponse
import com.retheviper.chat.contract.ChannelResponse
import com.retheviper.chat.contract.ChatCommand
import com.retheviper.chat.contract.ChatEvent
import com.retheviper.chat.contract.CreateChannelRequest
import com.retheviper.chat.contract.CreateWorkspaceRequest
import com.retheviper.chat.contract.MarkNotificationsReadRequest
import com.retheviper.chat.contract.MessageResponse
import com.retheviper.chat.contract.MessagePageResponse
import com.retheviper.chat.contract.MentionNotificationResponse
import com.retheviper.chat.contract.ResolveLinkPreviewRequest
import com.retheviper.chat.contract.ResolveLinkPreviewResponse
import com.retheviper.chat.contract.ThreadResponse
import com.retheviper.chat.contract.ToggleReactionRequest
import com.retheviper.chat.contract.UpdateWorkspaceMemberRequest
import com.retheviper.chat.contract.WorkspaceMemberResponse
import com.retheviper.chat.contract.WorkspaceResponse
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MessagingClient(
    private val baseUrl: String
) {
    private val client: HttpClient = platformHttpClient {
        install(ContentNegotiation) {
            json(JsonInstance)
        }
        install(WebSockets)
    }

    suspend fun listWorkspaces(): List<WorkspaceResponse> =
        requestDecoded {
            client.get(api("/api/v1/workspaces"))
        }

    suspend fun createWorkspace(request: CreateWorkspaceRequest): WorkspaceResponse =
        requestDecoded {
            client.post(api("/api/v1/workspaces")) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun getWorkspaceBySlug(slug: String): WorkspaceResponse =
        requestDecoded {
            client.get(api("/api/v1/workspaces/by-slug/${slug.trim()}"))
        }

    suspend fun addWorkspaceMember(workspaceId: String, request: AddWorkspaceMemberRequest): WorkspaceMemberResponse =
        requestDecoded {
            client.post(api("/api/v1/workspaces/$workspaceId/members")) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun listWorkspaceMembers(workspaceId: String): List<WorkspaceMemberResponse> =
        requestDecoded {
            client.get(api("/api/v1/workspaces/$workspaceId/members"))
        }

    suspend fun updateWorkspaceMember(memberId: String, request: UpdateWorkspaceMemberRequest): WorkspaceMemberResponse =
        requestDecoded {
            client.put(api("/api/v1/members/$memberId")) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun createChannel(workspaceId: String, request: CreateChannelRequest): ChannelResponse =
        requestDecoded {
            client.post(api("/api/v1/workspaces/$workspaceId/channels")) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun listWorkspaceChannels(workspaceId: String): List<ChannelResponse> =
        requestDecoded {
            client.get(api("/api/v1/workspaces/$workspaceId/channels"))
        }

    suspend fun listChannelMessages(
        channelId: String,
        limit: Int = 50,
        beforeMessageId: String? = null
    ): MessagePageResponse =
        requestDecoded {
            val beforeQuery = beforeMessageId?.let { "&beforeMessageId=$it" }.orEmpty()
            client.get(api("/api/v1/channels/$channelId/messages?limit=$limit$beforeQuery"))
        }

    suspend fun getThread(messageId: String): ThreadResponse =
        requestDecoded {
            client.get(api("/api/v1/messages/$messageId/thread"))
        }

    suspend fun resolveLinkPreview(url: String): ResolveLinkPreviewResponse =
        requestDecoded {
            client.post(api("/api/v1/link-preview/resolve")) {
                contentType(ContentType.Application.Json)
                setBody(ResolveLinkPreviewRequest(url))
            }
        }

    suspend fun toggleReaction(messageId: String, memberId: String, emoji: String): MessageResponse =
        requestDecoded {
            client.post(api("/api/v1/messages/$messageId/reactions/toggle")) {
                contentType(ContentType.Application.Json)
                setBody(ToggleReactionRequest(memberId = memberId, emoji = emoji))
            }
        }

    suspend fun listNotifications(memberId: String, unreadOnly: Boolean = true): List<MentionNotificationResponse> =
        requestDecoded {
            client.get(api("/api/v1/members/$memberId/notifications?unreadOnly=$unreadOnly"))
        }

    suspend fun markNotificationsRead(memberId: String, notificationIds: List<String>) {
        if (notificationIds.isEmpty()) return
        val response = client.post(api("/api/v1/members/$memberId/notifications/read")) {
            contentType(ContentType.Application.Json)
            setBody(MarkNotificationsReadRequest(notificationIds))
        }
        if (!response.status.isSuccess()) {
            throw decodeApiException(response.bodyAsText(), response.status.value)
        }
    }

    suspend fun openChat(channelId: String): DefaultClientWebSocketSession =
        client.webSocketSession(urlString = websocket("/ws/channels/$channelId"))

    suspend fun sendCommand(session: DefaultClientWebSocketSession, command: ChatCommand) {
        session.send(Frame.Text(JsonInstance.encodeToString(command)))
    }

    suspend fun receiveEvent(session: DefaultClientWebSocketSession): ChatEvent {
        val frame = session.incoming.receive() as? Frame.Text ?: error("Expected text websocket frame")
        return JsonInstance.decodeFromString(frame.readText())
    }

    suspend fun close() {
        client.close()
    }

    private suspend inline fun <reified T> requestDecoded(block: () -> io.ktor.client.statement.HttpResponse): T {
        val response = block()
        val payload = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw decodeApiException(payload, response.status.value)
        }
        return JsonInstance.decodeFromString(payload)
    }

    private fun decodeApiException(payload: String, statusCode: Int): IllegalStateException {
        val message = try {
            JsonInstance.decodeFromString<ApiErrorResponse>(payload).message
        } catch (_: Throwable) {
            payload.ifBlank { "Request failed with status $statusCode" }
        }
        return IllegalStateException(message)
    }

    private fun api(path: String): String = if (baseUrl.isBlank()) path else "$baseUrl$path"

    private fun websocket(path: String): String {
        if (baseUrl.isBlank()) {
            return path
        }

        val normalized = baseUrl.removeSuffix("/")
        val wsBase = when {
            normalized.startsWith("https://") -> normalized.replaceFirst("https://", "wss://")
            normalized.startsWith("http://") -> normalized.replaceFirst("http://", "ws://")
            else -> normalized
        }
        return "$wsBase$path"
    }

    private companion object {
        val JsonInstance = Json {
            ignoreUnknownKeys = true
            prettyPrint = false
            encodeDefaults = true
        }
    }
}

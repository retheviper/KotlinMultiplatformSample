package com.retheviper.chat.messaging

import com.retheviper.chat.module
import com.retheviper.chat.contract.AddWorkspaceMemberRequest
import com.retheviper.chat.contract.ChannelResponse
import com.retheviper.chat.contract.ChatCommand
import com.retheviper.chat.contract.ChatCommandType
import com.retheviper.chat.contract.ChatEvent
import com.retheviper.chat.contract.ChatEventType
import com.retheviper.chat.contract.ChannelVisibility
import com.retheviper.chat.contract.CreateChannelRequest
import com.retheviper.chat.contract.CreateWorkspaceRequest
import com.retheviper.chat.contract.MarkNotificationsReadRequest
import com.retheviper.chat.contract.LinkPreviewResponse
import com.retheviper.chat.contract.MessagePageResponse
import com.retheviper.chat.contract.MessageResponse
import com.retheviper.chat.contract.MentionNotificationResponse
import com.retheviper.chat.contract.NotificationKind
import com.retheviper.chat.contract.ThreadResponse
import com.retheviper.chat.contract.ToggleReactionRequest
import com.retheviper.chat.contract.UpdateWorkspaceMemberRequest
import com.retheviper.chat.contract.WorkspaceMemberResponse
import com.retheviper.chat.contract.WorkspaceResponse
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import kotlin.test.assertEquals

private val testJson = Json { ignoreUnknownKeys = false }

class ApiIntegrationTest {
    @BeforeEach
    fun resetDatabase() {
        DriverManager.getConnection(container.jdbcUrl, container.username, container.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("DROP SCHEMA public CASCADE")
                statement.execute("CREATE SCHEMA public")
            }
        }
    }

    @Test
    fun `workspace, channel, message, and thread flows work end to end`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "messaging.database.jdbcUrl" to container.jdbcUrl,
                "messaging.database.r2dbcUrl" to container.r2dbcUrl(),
                "messaging.database.username" to container.username,
                "messaging.database.password" to container.password
            )
        }
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(testJson)
            }
            install(WebSockets)
        }

        val workspace = client.post("/api/v1/workspaces") {
            contentType(ContentType.Application.Json)
            setBody(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "u-alice",
                    ownerDisplayName = "Alice"
                )
            )
        }.also { assertEquals(HttpStatusCode.Created, it.status) }.body<WorkspaceResponse>()

        val listedWorkspaces = client.get("/api/v1/workspaces")
            .also { assertEquals(HttpStatusCode.OK, it.status) }
            .body<List<WorkspaceResponse>>()

        val workspaceBySlug = client.get("/api/v1/workspaces/by-slug/acme")
            .also { assertEquals(HttpStatusCode.OK, it.status) }
            .body<WorkspaceResponse>()

        val bob = client.post("/api/v1/workspaces/${workspace.id}/members") {
            contentType(ContentType.Application.Json)
            setBody(
                AddWorkspaceMemberRequest(
                    userId = "u-bob",
                    displayName = "Bob"
                )
            )
        }.also { assertEquals(HttpStatusCode.Created, it.status) }.body<WorkspaceMemberResponse>()

        val channel = client.post("/api/v1/workspaces/${workspace.id}/channels") {
            contentType(ContentType.Application.Json)
            setBody(
                CreateChannelRequest(
                    slug = "design",
                    name = "design",
                    topic = "team updates",
                    visibility = ChannelVisibility.PUBLIC,
                    createdByMemberId = workspace.ownerMemberId
                )
            )
        }.also { assertEquals(HttpStatusCode.Created, it.status) }.body<ChannelResponse>()

        val socket = client.webSocketSession("/ws/channels/${channel.id}")

        socket.sendCommand(ChatCommand(type = ChatCommandType.LOAD_RECENT, limit = 20))
        val snapshot = socket.receiveEvent()
        assertEquals(ChatEventType.SNAPSHOT, snapshot.type)
        assertEquals(0, snapshot.messages.size)

        socket.sendCommand(
            ChatCommand(
                type = ChatCommandType.POST_MESSAGE,
                authorMemberId = workspace.ownerMemberId,
                body = "hello @u-bob https://example.com/article",
                linkPreview = LinkPreviewResponse(
                    url = "https://example.com/article",
                    title = "Example article",
                    description = "Preview description",
                    siteName = "Example"
                )
            )
        )
        val rootMessage = socket.receiveEvent().message!!

        socket.sendCommand(
            ChatCommand(
                type = ChatCommandType.REPLY_MESSAGE,
                authorMemberId = bob.id,
                body = "roger that",
                parentMessageId = rootMessage.id
            )
        )
        val reply = socket.receiveEvent().message!!

        socket.sendCommand(
            ChatCommand(
                type = ChatCommandType.TOGGLE_REACTION,
                authorMemberId = bob.id,
                messageId = rootMessage.id,
                emoji = "👍"
            )
        )
        val reactionEvent = socket.receiveEvent()
        socket.close()

        val reactedRoot = client.post("/api/v1/messages/${rootMessage.id}/reactions/toggle") {
            contentType(ContentType.Application.Json)
            setBody(ToggleReactionRequest(memberId = workspace.ownerMemberId, emoji = "👍"))
        }.also { assertEquals(HttpStatusCode.OK, it.status) }.body<MessageResponse>()

        val messages = client.get("/api/v1/channels/${channel.id}/messages?limit=20")
            .also { assertEquals(HttpStatusCode.OK, it.status) }
            .body<MessagePageResponse>()

        val thread = client.get("/api/v1/messages/${rootMessage.id}/thread")
            .also { assertEquals(HttpStatusCode.OK, it.status) }
            .body<ThreadResponse>()

        val channels = client.get("/api/v1/workspaces/${workspace.id}/channels")
            .also { assertEquals(HttpStatusCode.OK, it.status) }
            .body<List<ChannelResponse>>()

        val members = client.get("/api/v1/workspaces/${workspace.id}/members")
            .also { assertEquals(HttpStatusCode.OK, it.status) }
            .body<List<WorkspaceMemberResponse>>()

        val updatedBob = client.put("/api/v1/members/${bob.id}") {
            contentType(ContentType.Application.Json)
            setBody(UpdateWorkspaceMemberRequest(displayName = "Bobby"))
        }.also { assertEquals(HttpStatusCode.OK, it.status) }.body<WorkspaceMemberResponse>()

        val notifications = client.get("/api/v1/members/${bob.id}/notifications?unreadOnly=true")
            .also { assertEquals(HttpStatusCode.OK, it.status) }
            .body<List<MentionNotificationResponse>>()

        client.post("/api/v1/members/${bob.id}/notifications/read") {
            contentType(ContentType.Application.Json)
            setBody(MarkNotificationsReadRequest(notificationIds = notifications.map { it.id }))
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val notificationsAfterRead = client.get("/api/v1/members/${bob.id}/notifications?unreadOnly=true")
            .also { assertEquals(HttpStatusCode.OK, it.status) }
            .body<List<MentionNotificationResponse>>()

        assertEquals(workspace.id, workspaceBySlug.id)
        assertEquals(1, listedWorkspaces.size)
        assertEquals(workspace.id, listedWorkspaces.single().id)
        assertEquals(2, channels.size)
        assertEquals("general", channels.first().slug)
        assertEquals(channel.id, channels.last().id)
        assertEquals(2, members.size)
        assertEquals("Bobby", updatedBob.displayName)
        assertEquals(1, messages.messages.size)
        assertEquals(rootMessage.id, messages.messages.single().id)
        assertEquals("https://example.com/article", messages.messages.single().linkPreview?.url)
        assertEquals(1, messages.messages.single().threadReplyCount)
        assertEquals(ChatEventType.REACTION_UPDATED, reactionEvent.type)
        assertEquals(1, reactionEvent.message!!.reactions.single().count)
        assertEquals(2, reactedRoot.reactions.single().count)
        assertEquals("👍", reactedRoot.reactions.single().emoji)
        assertEquals(2, messages.messages.single().reactions.single().count)
        assertEquals(rootMessage.id, thread.root.id)
        assertEquals(1, thread.replies.size)
        assertEquals(reply.id, thread.replies.single().id)
        assertEquals(rootMessage.id, thread.replies.single().threadRootMessageId)
        assertEquals(1, notifications.size)
        assertEquals(NotificationKind.MENTION, notifications.single().kind)
        assertEquals(rootMessage.id, notifications.single().messageId)
        assertEquals(0, notificationsAfterRead.size)
    }

    @Test
    fun `thread participants receive notifications when others reply later`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "messaging.database.jdbcUrl" to container.jdbcUrl,
                "messaging.database.r2dbcUrl" to container.r2dbcUrl(),
                "messaging.database.username" to container.username,
                "messaging.database.password" to container.password
            )
        }
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(testJson)
            }
            install(WebSockets)
        }

        val workspace = client.post("/api/v1/workspaces") {
            contentType(ContentType.Application.Json)
            setBody(
                CreateWorkspaceRequest(
                    slug = "acme-thread",
                    name = "Acme Thread",
                    ownerUserId = "u-alice",
                    ownerDisplayName = "Alice"
                )
            )
        }.also { assertEquals(HttpStatusCode.Created, it.status) }.body<WorkspaceResponse>()

        val bob = client.post("/api/v1/workspaces/${workspace.id}/members") {
            contentType(ContentType.Application.Json)
            setBody(
                AddWorkspaceMemberRequest(
                    userId = "u-bob",
                    displayName = "Bob"
                )
            )
        }.also { assertEquals(HttpStatusCode.Created, it.status) }.body<WorkspaceMemberResponse>()

        val generalChannel = client.get("/api/v1/workspaces/${workspace.id}/channels")
            .also { assertEquals(HttpStatusCode.OK, it.status) }
            .body<List<ChannelResponse>>()
            .first { it.slug == "general" }

        val socket = client.webSocketSession("/ws/channels/${generalChannel.id}")

        socket.sendCommand(
            ChatCommand(
                type = ChatCommandType.POST_MESSAGE,
                authorMemberId = workspace.ownerMemberId,
                body = "Kickoff"
            )
        )
        val root = socket.receiveEvent().message!!

        socket.sendCommand(
            ChatCommand(
                type = ChatCommandType.REPLY_MESSAGE,
                authorMemberId = bob.id,
                body = "I'm in",
                parentMessageId = root.id
            )
        )
        socket.receiveEvent()

        socket.sendCommand(
            ChatCommand(
                type = ChatCommandType.REPLY_MESSAGE,
                authorMemberId = workspace.ownerMemberId,
                body = "Let's start",
                parentMessageId = root.id
            )
        )
        val followUp = socket.receiveEvent().message!!
        socket.close()

        val notifications = client.get("/api/v1/members/${bob.id}/notifications?unreadOnly=true")
            .also { assertEquals(HttpStatusCode.OK, it.status) }
            .body<List<MentionNotificationResponse>>()

        assertEquals(1, notifications.size)
        assertEquals(NotificationKind.THREAD_ACTIVITY, notifications.single().kind)
        assertEquals(root.id, notifications.single().threadRootMessageId)
        assertEquals(followUp.id, notifications.single().messageId)
    }

    @Test
    fun `mcp streamable http endpoint exposes messaging tools`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "messaging.database.jdbcUrl" to container.jdbcUrl,
                "messaging.database.r2dbcUrl" to container.r2dbcUrl(),
                "messaging.database.username" to container.username,
                "messaging.database.password" to container.password
            )
        }
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(testJson)
            }
        }

        val initializeResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2025-03-26",
                    "capabilities": {},
                    "clientInfo": {
                      "name": "api-integration-test",
                      "version": "1.0.0"
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val initializePayload = testJson.parseToJsonElement(initializeResponse.bodyAsText()).jsonObject
        val protocolVersion = initializePayload.resultField("protocolVersion")
        val sessionId = initializeResponse.headers["MCP-Session-Id"] ?: initializeResponse.headers["Mcp-Session-Id"]

        client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "method": "notifications/initialized"
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.Accepted, it.status) }

        val toolListResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 2,
                  "method": "tools/list",
                  "params": {}
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val toolNames = testJson.parseToJsonElement(toolListResponse.bodyAsText())
            .jsonObject
            .resultArray("tools")
            .map { it.jsonObject["name"]!!.jsonPrimitive.content }

        assertEquals(
            setOf(
                "get_health",
                "list_workspaces",
                "create_workspace",
                "get_workspace_by_slug",
                "list_workspace_channels",
                "create_channel",
                "list_members",
                "add_member",
                "update_member",
                "list_channel_messages",
                "get_thread",
                "post_message",
                "reply_message",
                "toggle_reaction",
                "list_notifications",
                "mark_notifications_read"
            ),
            toolNames.toSet()
        )

        val createWorkspaceResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 3,
                  "method": "tools/call",
                  "params": {
                    "name": "create_workspace",
                    "arguments": {
                      "slug": "acme",
                      "name": "Acme",
                      "ownerUserId": "u-alice",
                      "ownerDisplayName": "Alice"
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val createdWorkspace = testJson.decodeFromJsonElement<WorkspaceResponse>(
            extractToolPayload(testJson.parseToJsonElement(createWorkspaceResponse.bodyAsText()).jsonObject)
        )

        val listWorkspacesResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 4,
                  "method": "tools/call",
                  "params": {
                    "name": "list_workspaces",
                    "arguments": {}
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val listedWorkspaces = testJson.decodeFromJsonElement<List<WorkspaceResponse>>(
            extractToolPayload(testJson.parseToJsonElement(listWorkspacesResponse.bodyAsText()).jsonObject)
        )

        val createChannelResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 5,
                  "method": "tools/call",
                  "params": {
                    "name": "create_channel",
                    "arguments": {
                      "workspaceSlug": "acme",
                      "slug": "design",
                      "name": "design",
                      "topic": "team updates",
                      "createdByMemberId": "${createdWorkspace.ownerMemberId}"
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val createdChannel = testJson.decodeFromJsonElement<ChannelResponse>(
            extractToolPayload(testJson.parseToJsonElement(createChannelResponse.bodyAsText()).jsonObject)
        )

        val addMemberResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 6,
                  "method": "tools/call",
                  "params": {
                    "name": "add_member",
                    "arguments": {
                      "workspaceSlug": "acme",
                      "userId": "u-bob",
                      "displayName": "Bob"
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val bob = testJson.decodeFromJsonElement<WorkspaceMemberResponse>(
            extractToolPayload(testJson.parseToJsonElement(addMemberResponse.bodyAsText()).jsonObject)
        )

        val listMembersResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 7,
                  "method": "tools/call",
                  "params": {
                    "name": "list_members",
                    "arguments": {
                      "workspaceSlug": "acme"
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val listedMembers = testJson.decodeFromJsonElement<List<WorkspaceMemberResponse>>(
            extractToolPayload(testJson.parseToJsonElement(listMembersResponse.bodyAsText()).jsonObject)
        )

        val postMessageResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 8,
                  "method": "tools/call",
                  "params": {
                    "name": "post_message",
                    "arguments": {
                      "channelId": "${createdChannel.id}",
                      "authorMemberId": "${createdWorkspace.ownerMemberId}",
                      "body": "hello from mcp @u-bob"
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val rootMessage = testJson.decodeFromJsonElement<MessageResponse>(
            extractToolPayload(testJson.parseToJsonElement(postMessageResponse.bodyAsText()).jsonObject)
        )

        val replyMessageResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 9,
                  "method": "tools/call",
                  "params": {
                    "name": "reply_message",
                    "arguments": {
                      "messageId": "${rootMessage.id}",
                      "authorMemberId": "${bob.id}",
                      "body": "roger that"
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val replyMessage = testJson.decodeFromJsonElement<MessageResponse>(
            extractToolPayload(testJson.parseToJsonElement(replyMessageResponse.bodyAsText()).jsonObject)
        )

        val toggleReactionResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 10,
                  "method": "tools/call",
                  "params": {
                    "name": "toggle_reaction",
                    "arguments": {
                      "messageId": "${rootMessage.id}",
                      "memberId": "${bob.id}",
                      "emoji": "👍"
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val reactedMessage = testJson.decodeFromJsonElement<MessageResponse>(
            extractToolPayload(testJson.parseToJsonElement(toggleReactionResponse.bodyAsText()).jsonObject)
        )

        val updateMemberResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 11,
                  "method": "tools/call",
                  "params": {
                    "name": "update_member",
                    "arguments": {
                      "memberId": "${bob.id}",
                      "displayName": "Bobby"
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val updatedBob = testJson.decodeFromJsonElement<WorkspaceMemberResponse>(
            extractToolPayload(testJson.parseToJsonElement(updateMemberResponse.bodyAsText()).jsonObject)
        )

        val listChannelsResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 12,
                  "method": "tools/call",
                  "params": {
                    "name": "list_workspace_channels",
                    "arguments": {
                      "workspaceSlug": "acme"
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val listedChannels = testJson.decodeFromJsonElement<List<ChannelResponse>>(
            extractToolPayload(testJson.parseToJsonElement(listChannelsResponse.bodyAsText()).jsonObject)
        )

        val listMessagesResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 13,
                  "method": "tools/call",
                  "params": {
                    "name": "list_channel_messages",
                    "arguments": {
                      "channelId": "${createdChannel.id}",
                      "limit": 20
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val messagePage = testJson.decodeFromJsonElement<MessagePageResponse>(
            extractToolPayload(testJson.parseToJsonElement(listMessagesResponse.bodyAsText()).jsonObject)
        )

        val getThreadResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 14,
                  "method": "tools/call",
                  "params": {
                    "name": "get_thread",
                    "arguments": {
                      "messageId": "${replyMessage.id}"
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val thread = testJson.decodeFromJsonElement<ThreadResponse>(
            extractToolPayload(testJson.parseToJsonElement(getThreadResponse.bodyAsText()).jsonObject)
        )

        val listNotificationsResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 15,
                  "method": "tools/call",
                  "params": {
                    "name": "list_notifications",
                    "arguments": {
                      "memberId": "${bob.id}",
                      "unreadOnly": true
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val notifications = testJson.decodeFromJsonElement<List<MentionNotificationResponse>>(
            extractToolPayload(testJson.parseToJsonElement(listNotificationsResponse.bodyAsText()).jsonObject)
        )

        val markNotificationsReadResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 16,
                  "method": "tools/call",
                  "params": {
                    "name": "mark_notifications_read",
                    "arguments": {
                      "memberId": "${bob.id}",
                      "notificationIds": ["${notifications.single().id}"]
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val readAck = extractToolPayload(testJson.parseToJsonElement(markNotificationsReadResponse.bodyAsText()).jsonObject).jsonObject

        val notificationsAfterReadResponse = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            headers.append("Accept", "application/json, text/event-stream")
            headers.append("MCP-Protocol-Version", protocolVersion)
            if (sessionId != null) {
                headers.append("MCP-Session-Id", sessionId)
            }
            setBody(
                """
                {
                  "jsonrpc": "2.0",
                  "id": 17,
                  "method": "tools/call",
                  "params": {
                    "name": "list_notifications",
                    "arguments": {
                      "memberId": "${bob.id}",
                      "unreadOnly": true
                    }
                  }
                }
                """.trimIndent()
            )
        }.also { assertEquals(HttpStatusCode.OK, it.status) }

        val notificationsAfterRead = testJson.decodeFromJsonElement<List<MentionNotificationResponse>>(
            extractToolPayload(testJson.parseToJsonElement(notificationsAfterReadResponse.bodyAsText()).jsonObject)
        )

        assertEquals(1, listedWorkspaces.size)
        assertEquals("acme", listedWorkspaces.single().slug)
        assertEquals("acme", createdWorkspace.slug)
        assertEquals("design", createdChannel.slug)
        assertEquals(2, listedMembers.size)
        assertEquals("Bob", bob.displayName)
        assertEquals("Bobby", updatedBob.displayName)
        assertEquals(setOf("general", "design"), listedChannels.map { it.slug }.toSet())
        assertEquals("hello from mcp @u-bob", rootMessage.body)
        assertEquals(rootMessage.id, replyMessage.threadRootMessageId)
        assertEquals("👍", reactedMessage.reactions.single().emoji)
        assertEquals(1, reactedMessage.reactions.single().count)
        assertEquals(1, messagePage.messages.size)
        assertEquals(rootMessage.id, messagePage.messages.single().id)
        assertEquals(1, messagePage.messages.single().threadReplyCount)
        assertEquals(1, messagePage.messages.single().reactions.single().count)
        assertEquals(rootMessage.id, thread.root.id)
        assertEquals(replyMessage.id, thread.replies.single().id)
        assertEquals(1, notifications.size)
        assertEquals(NotificationKind.MENTION, notifications.single().kind)
        assertEquals("ok", readAck["status"]!!.jsonPrimitive.content)
        assertEquals(0, notificationsAfterRead.size)
    }

    @Test
    fun `openapi and swagger endpoints are exposed`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "messaging.database.jdbcUrl" to container.jdbcUrl,
                "messaging.database.r2dbcUrl" to container.r2dbcUrl(),
                "messaging.database.username" to container.username,
                "messaging.database.password" to container.password
            )
        }
        application {
            module()
        }

        val yaml = client.get("/openapi.yaml")
        val docs = client.get("/docs")

        assertEquals(HttpStatusCode.OK, yaml.status)
        assertEquals(HttpStatusCode.OK, docs.status)
        assertEquals(true, yaml.body<String>().contains("openapi: 3.1.0"))
    }

    @Test
    fun `root serves the compose web frontend`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "messaging.database.jdbcUrl" to container.jdbcUrl,
                "messaging.database.r2dbcUrl" to container.r2dbcUrl(),
                "messaging.database.username" to container.username,
                "messaging.database.password" to container.password
            )
        }
        application {
            module()
        }

        val index = client.get("/")

        assertEquals(HttpStatusCode.OK, index.status)
        assertEquals(true, index.body<String>().contains("shared.js"))
    }

    companion object {
        private val container = PostgreSQLContainer("postgres:17-alpine")

        @JvmStatic
        @BeforeAll
        fun startContainer() {
            container.start()
        }

        @JvmStatic
        @AfterAll
        fun stopContainer() {
            container.stop()
        }
    }
}

private fun PostgreSQLContainer<*>.r2dbcUrl(): String {
    return "r2dbc:postgresql://${host}:${getMappedPort(5432)}/${databaseName}"
}

private suspend fun DefaultClientWebSocketSession.sendCommand(command: ChatCommand) {
    send(Frame.Text(testJson.encodeToString(command)))
}

private suspend fun DefaultClientWebSocketSession.receiveEvent(): ChatEvent {
    val frame = incoming.receiveCatching().getOrNull() as? Frame.Text ?: error("expected text frame")
    return testJson.decodeFromString(frame.readText())
}

private fun JsonObject.resultField(fieldName: String): String =
    this["result"]!!.jsonObject[fieldName]!!.jsonPrimitive.content

private fun JsonObject.resultArray(fieldName: String) =
    this["result"]!!.jsonObject[fieldName]!!.jsonArray

private fun extractToolPayload(response: JsonObject): JsonElement =
    Json.parseToJsonElement(
        response["result"]!!
            .jsonObject["content"]!!
            .jsonArray
            .first()
            .jsonObject["text"]!!
            .jsonPrimitive
            .content
    )

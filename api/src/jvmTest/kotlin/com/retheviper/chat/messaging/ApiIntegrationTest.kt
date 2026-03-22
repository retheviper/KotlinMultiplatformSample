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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

private val testJson = Json { ignoreUnknownKeys = false }
private val container = PostgreSQLContainer("postgres:17-alpine")

class ApiIntegrationTest : FunSpec({
    beforeSpec {
        container.start()
    }

    afterSpec {
        container.stop()
    }

    beforeTest {
        DriverManager.getConnection(container.jdbcUrl, container.username, container.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("DROP SCHEMA public CASCADE")
                statement.execute("CREATE SCHEMA public")
            }
        }
    }

    test("workspace, channel, message, and thread flows work end to end") {
        testApplication {
        environment {
            config = testConfig()
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
        }.also { it.status shouldBe HttpStatusCode.Created }.body<WorkspaceResponse>()

        val listedWorkspaces = client.get("/api/v1/workspaces")
            .also { it.status shouldBe HttpStatusCode.OK }
            .body<List<WorkspaceResponse>>()

        val workspaceBySlug = client.get("/api/v1/workspaces/by-slug/acme")
            .also { it.status shouldBe HttpStatusCode.OK }
            .body<WorkspaceResponse>()

        val bob = client.post("/api/v1/workspaces/${workspace.id}/members") {
            contentType(ContentType.Application.Json)
            setBody(
                AddWorkspaceMemberRequest(
                    userId = "u-bob",
                    displayName = "Bob"
                )
            )
        }.also { it.status shouldBe HttpStatusCode.Created }.body<WorkspaceMemberResponse>()

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
        }.also { it.status shouldBe HttpStatusCode.Created }.body<ChannelResponse>()

        val socket = client.webSocketSession("/ws/channels/${channel.id}")

        socket.sendCommand(ChatCommand(type = ChatCommandType.LOAD_RECENT, limit = 20))
        val snapshot = socket.receiveEvent()
        snapshot.type shouldBe ChatEventType.SNAPSHOT
        snapshot.messages.size shouldBe 0

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
        }.also { it.status shouldBe HttpStatusCode.OK }.body<MessageResponse>()

        val messages = client.get("/api/v1/channels/${channel.id}/messages?limit=20")
            .also { it.status shouldBe HttpStatusCode.OK }
            .body<MessagePageResponse>()

        val thread = client.get("/api/v1/messages/${rootMessage.id}/thread")
            .also { it.status shouldBe HttpStatusCode.OK }
            .body<ThreadResponse>()

        val channels = client.get("/api/v1/workspaces/${workspace.id}/channels")
            .also { it.status shouldBe HttpStatusCode.OK }
            .body<List<ChannelResponse>>()

        val members = client.get("/api/v1/workspaces/${workspace.id}/members")
            .also { it.status shouldBe HttpStatusCode.OK }
            .body<List<WorkspaceMemberResponse>>()

        val updatedBob = client.put("/api/v1/members/${bob.id}") {
            contentType(ContentType.Application.Json)
            setBody(UpdateWorkspaceMemberRequest(displayName = "Bobby"))
        }.also { it.status shouldBe HttpStatusCode.OK }.body<WorkspaceMemberResponse>()

        val notifications = client.get("/api/v1/members/${bob.id}/notifications?unreadOnly=true")
            .also { it.status shouldBe HttpStatusCode.OK }
            .body<List<MentionNotificationResponse>>()

        client.post("/api/v1/members/${bob.id}/notifications/read") {
            contentType(ContentType.Application.Json)
            setBody(MarkNotificationsReadRequest(notificationIds = notifications.map { it.id }))
        }.also { it.status shouldBe HttpStatusCode.OK }

        val notificationsAfterRead = client.get("/api/v1/members/${bob.id}/notifications?unreadOnly=true")
            .also { it.status shouldBe HttpStatusCode.OK }
            .body<List<MentionNotificationResponse>>()

        workspaceBySlug.id shouldBe workspace.id
        listedWorkspaces.size shouldBe 1
        listedWorkspaces.single().id shouldBe workspace.id
        channels.size shouldBe 2
        channels.first().slug shouldBe "general"
        channels.last().id shouldBe channel.id
        members.size shouldBe 2
        updatedBob.displayName shouldBe "Bobby"
        messages.messages.size shouldBe 1
        messages.messages.single().id shouldBe rootMessage.id
        messages.messages.single().linkPreview?.url shouldBe "https://example.com/article"
        messages.messages.single().threadReplyCount shouldBe 1
        reactionEvent.type shouldBe ChatEventType.REACTION_UPDATED
        reactionEvent.message!!.reactions.single().count shouldBe 1
        reactedRoot.reactions.single().count shouldBe 2
        reactedRoot.reactions.single().emoji shouldBe "👍"
        messages.messages.single().reactions.single().count shouldBe 2
        thread.root.id shouldBe rootMessage.id
        thread.replies.size shouldBe 1
        thread.replies.single().id shouldBe reply.id
        thread.replies.single().threadRootMessageId shouldBe rootMessage.id
        notifications.size shouldBe 1
        notifications.single().kind shouldBe NotificationKind.MENTION
        notifications.single().messageId shouldBe rootMessage.id
        notificationsAfterRead.size shouldBe 0
        }
    }

    test("thread participants receive notifications when others reply later") {
        testApplication {
        environment {
            config = testConfig()
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
        }.also { it.status shouldBe HttpStatusCode.Created }.body<WorkspaceResponse>()

        val bob = client.post("/api/v1/workspaces/${workspace.id}/members") {
            contentType(ContentType.Application.Json)
            setBody(
                AddWorkspaceMemberRequest(
                    userId = "u-bob",
                    displayName = "Bob"
                )
            )
        }.also { it.status shouldBe HttpStatusCode.Created }.body<WorkspaceMemberResponse>()

        val generalChannel = client.get("/api/v1/workspaces/${workspace.id}/channels")
            .also { it.status shouldBe HttpStatusCode.OK }
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
            .also { it.status shouldBe HttpStatusCode.OK }
            .body<List<MentionNotificationResponse>>()

        notifications.size shouldBe 1
        notifications.single().kind shouldBe NotificationKind.THREAD_ACTIVITY
        notifications.single().threadRootMessageId shouldBe root.id
        notifications.single().messageId shouldBe followUp.id
        }
    }

    test("mcp streamable http endpoint exposes messaging tools") {
        testApplication {
        environment {
            config = testConfig()
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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.Accepted }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

        val toolNames = testJson.parseToJsonElement(toolListResponse.bodyAsText())
            .jsonObject
            .resultArray("tools")
            .map { it.jsonObject["name"]!!.jsonPrimitive.content }

        toolNames.toSet() shouldBe setOf(
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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

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
        }.also { it.status shouldBe HttpStatusCode.OK }

        val notificationsAfterRead = testJson.decodeFromJsonElement<List<MentionNotificationResponse>>(
            extractToolPayload(testJson.parseToJsonElement(notificationsAfterReadResponse.bodyAsText()).jsonObject)
        )

        listedWorkspaces.size shouldBe 1
        listedWorkspaces.single().slug shouldBe "acme"
        createdWorkspace.slug shouldBe "acme"
        createdChannel.slug shouldBe "design"
        listedMembers.size shouldBe 2
        bob.displayName shouldBe "Bob"
        updatedBob.displayName shouldBe "Bobby"
        listedChannels.map { it.slug }.toSet() shouldBe setOf("general", "design")
        rootMessage.body shouldBe "hello from mcp @u-bob"
        replyMessage.threadRootMessageId shouldBe rootMessage.id
        reactedMessage.reactions.single().emoji shouldBe "👍"
        reactedMessage.reactions.single().count shouldBe 1
        messagePage.messages.size shouldBe 1
        messagePage.messages.single().id shouldBe rootMessage.id
        messagePage.messages.single().threadReplyCount shouldBe 1
        messagePage.messages.single().reactions.single().count shouldBe 1
        thread.root.id shouldBe rootMessage.id
        thread.replies.single().id shouldBe replyMessage.id
        notifications.size shouldBe 1
        notifications.single().kind shouldBe NotificationKind.MENTION
        readAck["status"]!!.jsonPrimitive.content shouldBe "ok"
        notificationsAfterRead.size shouldBe 0
        }
    }

    test("openapi and swagger endpoints are exposed") {
        testApplication {
        environment {
            config = testConfig()
        }
        application {
            module()
        }

        val yaml = client.get("/openapi.yaml")
        val docs = client.get("/docs")

        yaml.status shouldBe HttpStatusCode.OK
        docs.status shouldBe HttpStatusCode.OK
        yaml.body<String>().contains("openapi: 3.1.0") shouldBe true
        }
    }

    test("root serves the compose web frontend") {
        testApplication {
        environment {
            config = testConfig()
        }
        application {
            module()
        }

        val index = client.get("/")

        index.status shouldBe HttpStatusCode.OK
        index.body<String>().contains("shared.js") shouldBe true
        }
    }
})

private fun testConfig() = MapApplicationConfig(
    "messaging.database.jdbcUrl" to container.jdbcUrl,
    "messaging.database.r2dbcUrl" to container.r2dbcUrl(),
    "messaging.database.username" to container.username,
    "messaging.database.password" to container.password,
    "messaging.bootstrap.sampleDataEnabled" to "false"
)

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

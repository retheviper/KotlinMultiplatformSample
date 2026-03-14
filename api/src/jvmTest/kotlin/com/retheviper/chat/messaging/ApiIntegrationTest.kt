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

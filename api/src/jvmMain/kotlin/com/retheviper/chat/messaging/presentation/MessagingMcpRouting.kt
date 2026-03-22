@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.retheviper.chat.messaging.presentation

import com.retheviper.chat.contract.AddWorkspaceMemberRequest
import com.retheviper.chat.contract.ChannelVisibility
import com.retheviper.chat.contract.CreateChannelRequest
import com.retheviper.chat.contract.CreateWorkspaceRequest
import com.retheviper.chat.contract.HealthResponse
import com.retheviper.chat.contract.MarkNotificationsReadRequest
import com.retheviper.chat.contract.MessagePageResponse
import com.retheviper.chat.contract.PostMessageRequest
import com.retheviper.chat.contract.ThreadResponse
import com.retheviper.chat.contract.ToggleReactionRequest
import com.retheviper.chat.contract.UpdateWorkspaceMemberRequest
import com.retheviper.chat.contract.WorkspaceMemberRole
import com.retheviper.chat.wiring.ApplicationDependencies
import io.ktor.server.application.Application
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.uuid.Uuid

fun Application.configureMessagingMcpRouting(dependencies: ApplicationDependencies) {
    val mcpServer = Server(
        serverInfo = Implementation(
            name = "messaging-api",
            version = "0.1.0-SNAPSHOT"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false)
            )
        )
    )

    mcpServer.addTool(
        name = "get_health",
        description = "Return the current API health payload."
    ) {
        toolResult(
            HealthResponse(
                status = "ok",
                service = "messaging-api",
                version = "v1"
            )
        )
    }

    mcpServer.addTool(
        name = "list_workspaces",
        description = "List all workspaces available in the messaging product."
    ) {
        toolResult(
            dependencies.queryService.listWorkspaces().map { it.toResponse() }
        )
    }

    mcpServer.addTool(
        name = "create_workspace",
        description = "Create a workspace and its default general channel.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("slug", "Workspace slug, for example acme.")
                stringProperty("name", "Workspace display name.")
                stringProperty("ownerUserId", "Owner user identifier.")
                stringProperty("ownerDisplayName", "Owner display name.")
            },
            required = listOf("slug", "name", "ownerUserId", "ownerDisplayName")
        )
    ) { request ->
        toolResult(
            dependencies.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = request.requireStringArgument("slug"),
                    name = request.requireStringArgument("name"),
                    ownerUserId = request.requireStringArgument("ownerUserId"),
                    ownerDisplayName = request.requireStringArgument("ownerDisplayName")
                )
            ).toResponse()
        )
    }

    mcpServer.addTool(
        name = "get_workspace_by_slug",
        description = "Look up one workspace by its slug.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("slug", "Workspace slug, for example acme.")
            },
            required = listOf("slug")
        )
    ) { request ->
        toolResult(
            dependencies.queryService.getWorkspaceBySlug(request.requireStringArgument("slug")).toResponse()
        )
    }

    mcpServer.addTool(
        name = "list_workspace_channels",
        description = "List channels for a workspace identified by slug.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("workspaceSlug", "Workspace slug whose channels should be listed.")
            },
            required = listOf("workspaceSlug")
        )
    ) { request ->
        val workspaceSlug = request.requireStringArgument("workspaceSlug")
        val workspace = dependencies.queryService.getWorkspaceBySlug(workspaceSlug)
        toolResult(
            dependencies.queryService.listChannels(workspace.id).map { it.toResponse() }
        )
    }

    mcpServer.addTool(
        name = "create_channel",
        description = "Create a channel inside a workspace identified by slug.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("workspaceSlug", "Workspace slug where the channel should be created.")
                stringProperty("slug", "Channel slug.")
                stringProperty("name", "Channel display name.")
                stringProperty("createdByMemberId", "Workspace member id that creates the channel.")
                stringProperty("topic", "Optional channel topic.")
                stringProperty("visibility", "PUBLIC or PRIVATE.")
            },
            required = listOf("workspaceSlug", "slug", "name", "createdByMemberId")
        )
    ) { request ->
        val workspace = dependencies.queryService.getWorkspaceBySlug(request.requireStringArgument("workspaceSlug"))
        val visibility = request.optionalStringArgument("visibility")
            ?.let(ChannelVisibility::valueOf)
            ?: ChannelVisibility.PUBLIC

        toolResult(
            dependencies.commandService.createChannel(
                workspaceId = workspace.id,
                request = CreateChannelRequest(
                    slug = request.requireStringArgument("slug"),
                    name = request.requireStringArgument("name"),
                    topic = request.optionalStringArgument("topic"),
                    visibility = visibility,
                    createdByMemberId = request.requireStringArgument("createdByMemberId")
                )
            ).toResponse()
        )
    }

    mcpServer.addTool(
        name = "list_members",
        description = "List members in a workspace identified by slug.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("workspaceSlug", "Workspace slug whose members should be listed.")
            },
            required = listOf("workspaceSlug")
        )
    ) { request ->
        val workspace = dependencies.queryService.getWorkspaceBySlug(request.requireStringArgument("workspaceSlug"))
        toolResult(
            dependencies.queryService.listMembers(workspace.id).map { it.toResponse() }
        )
    }

    mcpServer.addTool(
        name = "add_member",
        description = "Add a member to a workspace identified by slug.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("workspaceSlug", "Workspace slug where the member should be added.")
                stringProperty("userId", "External user identifier.")
                stringProperty("displayName", "Member display name.")
                stringProperty("role", "Optional role: OWNER, ADMIN, or MEMBER.")
            },
            required = listOf("workspaceSlug", "userId", "displayName")
        )
    ) { request ->
        val workspace = dependencies.queryService.getWorkspaceBySlug(request.requireStringArgument("workspaceSlug"))
        val role = request.optionalStringArgument("role")
            ?.let(WorkspaceMemberRole::valueOf)
            ?: WorkspaceMemberRole.MEMBER

        toolResult(
            dependencies.commandService.addMember(
                workspaceId = workspace.id,
                request = AddWorkspaceMemberRequest(
                    userId = request.requireStringArgument("userId"),
                    displayName = request.requireStringArgument("displayName"),
                    role = role
                )
            ).toResponse()
        )
    }

    mcpServer.addTool(
        name = "update_member",
        description = "Update one member display name.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("memberId", "Workspace member id.")
                stringProperty("displayName", "Updated display name.")
            },
            required = listOf("memberId", "displayName")
        )
    ) { request ->
        toolResult(
            dependencies.commandService.updateMember(
                memberId = request.requireUuidArgument("memberId"),
                request = UpdateWorkspaceMemberRequest(
                    displayName = request.requireStringArgument("displayName")
                )
            ).toResponse()
        )
    }

    mcpServer.addTool(
        name = "list_channel_messages",
        description = "List root messages in a channel.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("channelId", "Channel id.")
                stringProperty("beforeMessageId", "Optional message id cursor for pagination.")
                integerProperty("limit", "Optional page size between 1 and 100.")
            },
            required = listOf("channelId")
        )
    ) { request ->
        toolResult(
            MessagePageResponse(
                messages = dependencies.queryService.listChannelMessages(
                    channelId = request.requireUuidArgument("channelId"),
                    beforeMessageId = request.optionalUuidArgument("beforeMessageId"),
                    limit = request.optionalIntArgument("limit") ?: 50
                ).map { it.toResponse() }
            )
        )
    }

    mcpServer.addTool(
        name = "get_thread",
        description = "Get a thread by root message id or reply id.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("messageId", "Message id or reply id.")
            },
            required = listOf("messageId")
        )
    ) { request ->
        val (root, replies) = dependencies.queryService.getThread(request.requireUuidArgument("messageId"))
        toolResult(
            ThreadResponse(
                root = root.toResponse(),
                replies = replies.map { it.toResponse() }
            )
        )
    }

    mcpServer.addTool(
        name = "post_message",
        description = "Post a root message into a channel.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("channelId", "Channel id.")
                stringProperty("authorMemberId", "Author member id.")
                stringProperty("body", "Message body.")
            },
            required = listOf("channelId", "authorMemberId", "body")
        )
    ) { request ->
        toolResult(
            dependencies.commandService.postChannelMessage(
                channelId = request.requireUuidArgument("channelId"),
                request = PostMessageRequest(
                    authorMemberId = request.requireStringArgument("authorMemberId"),
                    body = request.requireStringArgument("body")
                )
            ).toResponse()
        )
    }

    mcpServer.addTool(
        name = "reply_message",
        description = "Reply to a message thread.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("messageId", "Root or reply message id to reply to.")
                stringProperty("authorMemberId", "Author member id.")
                stringProperty("body", "Reply body.")
            },
            required = listOf("messageId", "authorMemberId", "body")
        )
    ) { request ->
        toolResult(
            dependencies.commandService.replyToMessage(
                messageId = request.requireUuidArgument("messageId"),
                request = PostMessageRequest(
                    authorMemberId = request.requireStringArgument("authorMemberId"),
                    body = request.requireStringArgument("body")
                )
            ).toResponse()
        )
    }

    mcpServer.addTool(
        name = "toggle_reaction",
        description = "Toggle one reaction on a message.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("messageId", "Message id.")
                stringProperty("memberId", "Member id toggling the reaction.")
                stringProperty("emoji", "Emoji to toggle.")
            },
            required = listOf("messageId", "memberId", "emoji")
        )
    ) { request ->
        toolResult(
            dependencies.commandService.toggleReaction(
                messageId = request.requireUuidArgument("messageId"),
                memberId = request.requireUuidArgument("memberId"),
                emoji = ToggleReactionRequest(
                    memberId = request.requireStringArgument("memberId"),
                    emoji = request.requireStringArgument("emoji")
                ).emoji
            ).toResponse()
        )
    }

    mcpServer.addTool(
        name = "list_notifications",
        description = "List notifications for a member.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("memberId", "Workspace member id.")
                booleanProperty("unreadOnly", "Whether to return unread notifications only. Defaults to true.")
            },
            required = listOf("memberId")
        )
    ) { request ->
        toolResult(
            dependencies.queryService.listMemberNotifications(
                memberId = request.requireUuidArgument("memberId"),
                unreadOnly = request.optionalBooleanArgument("unreadOnly") ?: true
            ).map { it.toResponse() }
        )
    }

    mcpServer.addTool(
        name = "mark_notifications_read",
        description = "Mark notifications as read for a member.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                stringProperty("memberId", "Workspace member id.")
                stringArrayProperty("notificationIds", "Notification ids to mark as read.")
            },
            required = listOf("memberId", "notificationIds")
        )
    ) { request ->
        dependencies.commandService.markNotificationsRead(
            memberId = request.requireUuidArgument("memberId"),
            notificationIds = MarkNotificationsReadRequest(
                notificationIds = request.requireStringListArgument("notificationIds")
            ).notificationIds.map(Uuid::parse)
        )
        toolResult(
            McpMutationResponse(
                status = "ok",
                operation = "mark_notifications_read"
            )
        )
    }

    mcpStreamableHttp("/mcp") {
        mcpServer
    }
}

private inline fun <reified T> toolResult(payload: T): CallToolResult where T : @Serializable Any =
    CallToolResult(content = listOf(TextContent(Json.encodeToString(payload))))

@Serializable
private data class McpMutationResponse(
    val status: String,
    val operation: String
)

private fun JsonObjectBuilder.stringProperty(name: String, description: String) {
    put(
        name,
        buildJsonObject {
            put("type", "string")
            put("description", description)
        }
    )
}

private fun JsonObjectBuilder.integerProperty(name: String, description: String) {
    put(
        name,
        buildJsonObject {
            put("type", "integer")
            put("description", description)
        }
    )
}

private fun JsonObjectBuilder.booleanProperty(name: String, description: String) {
    put(
        name,
        buildJsonObject {
            put("type", "boolean")
            put("description", description)
        }
    )
}

private fun JsonObjectBuilder.stringArrayProperty(name: String, description: String) {
    put(
        name,
        buildJsonObject {
            put("type", "array")
            put("description", description)
            put(
                "items",
                buildJsonObject {
                    put("type", "string")
                }
            )
        }
    )
}

private fun CallToolRequest.requireStringArgument(name: String): String =
    arguments?.get(name)?.jsonPrimitive?.contentOrNull
        ?: throw IllegalArgumentException("$name is required")

private fun CallToolRequest.optionalStringArgument(name: String): String? =
    arguments?.get(name)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

private fun CallToolRequest.optionalIntArgument(name: String): Int? =
    arguments?.get(name)?.jsonPrimitive?.contentOrNull?.toIntOrNull()

private fun CallToolRequest.optionalBooleanArgument(name: String): Boolean? =
    arguments?.get(name)?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()

private fun CallToolRequest.requireUuidArgument(name: String): Uuid =
    Uuid.parse(requireStringArgument(name))

private fun CallToolRequest.optionalUuidArgument(name: String): Uuid? =
    optionalStringArgument(name)?.let(Uuid::parse)

private fun CallToolRequest.requireStringListArgument(name: String): List<String> =
    arguments?.get(name)?.let { argument ->
        argument.jsonArray.map { it.jsonPrimitive.content }
    } ?: throw IllegalArgumentException("$name is required")

@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.retheviper.chat.messaging.presentation

import com.retheviper.chat.contract.AddWorkspaceMemberRequest
import com.retheviper.chat.contract.CreateChannelRequest
import com.retheviper.chat.contract.CreateWorkspaceRequest
import com.retheviper.chat.contract.MarkNotificationsReadRequest
import com.retheviper.chat.contract.MessagePageResponse
import com.retheviper.chat.contract.ResolveLinkPreviewRequest
import com.retheviper.chat.contract.ResolveLinkPreviewResponse
import com.retheviper.chat.contract.ThreadResponse
import com.retheviper.chat.contract.ToggleReactionRequest
import com.retheviper.chat.contract.UpdateWorkspaceMemberRequest
import com.retheviper.chat.wiring.ApplicationDependencies
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.collect
import kotlin.uuid.Uuid

internal fun Route.configureWorkspaceRoutes(dependencies: ApplicationDependencies) {
    route("/workspaces") {
        get {
            call.respond(dependencies.queryService.listWorkspaces().map { it.toResponse() })
        }

        post {
            val request = call.receive<CreateWorkspaceRequest>()
            call.respond(HttpStatusCode.Created, dependencies.commandService.createWorkspace(request).toResponse())
        }

        route("/by-slug/{slug}") {
            get {
                val slug = call.parameters["slug"] ?: error("slug is required")
                call.respond(dependencies.queryService.getWorkspaceBySlug(slug).toResponse())
            }
        }

        route("/{workspaceId}") {
            get {
                call.respond(dependencies.queryService.getWorkspace(call.workspaceId()).toResponse())
            }

            route("/members") {
                post {
                    val request = call.receive<AddWorkspaceMemberRequest>()
                    call.respond(HttpStatusCode.Created, dependencies.commandService.addMember(call.workspaceId(), request).toResponse())
                }

                get {
                    call.respond(dependencies.queryService.listMembers(call.workspaceId()).map { it.toResponse() })
                }
            }

            route("/channels") {
                post {
                    val request = call.receive<CreateChannelRequest>()
                    call.respond(HttpStatusCode.Created, dependencies.commandService.createChannel(call.workspaceId(), request).toResponse())
                }

                get {
                    call.respond(dependencies.queryService.listChannels(call.workspaceId()).map { it.toResponse() })
                }
            }
        }
    }
}

internal fun Route.configureMemberRoutes(dependencies: ApplicationDependencies) {
    route("/members/{memberId}") {
        put {
            val request = call.receive<UpdateWorkspaceMemberRequest>()
            call.respond(dependencies.commandService.updateMember(call.memberId(), request).toResponse())
        }

        route("/notifications") {
            get {
                val unreadOnly = call.request.queryParameters["unreadOnly"]?.toBooleanStrictOrNull() ?: true
                call.respond(
                    dependencies.queryService.listMemberNotifications(call.memberId(), unreadOnly).map { it.toResponse() }
                )
            }

            route("/stream") {
                get {
                    val memberId = call.memberId()
                    dependencies.queryService.listMemberNotifications(memberId, unreadOnly = false)
                    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                        write("data: connected\n\n")
                        flush()

                        dependencies.notificationEventBus.stream(memberId).collect {
                            write("data: refresh\n\n")
                            flush()
                        }
                    }
                }
            }

            route("/read") {
                post {
                    val request = call.receive<MarkNotificationsReadRequest>()
                    dependencies.commandService.markNotificationsRead(
                        memberId = call.memberId(),
                        notificationIds = request.notificationIds.map(Uuid::parse)
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

internal fun Route.configureChannelRoutes(dependencies: ApplicationDependencies) {
    route("/channels/{channelId}") {
        route("/messages") {
            get {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val beforeMessageId = call.request.queryParameters["beforeMessageId"]?.let(Uuid::parse)
                call.respond(
                    MessagePageResponse(
                        dependencies.queryService.listChannelMessages(call.channelId(), beforeMessageId, limit).map { it.toResponse() }
                    )
                )
            }
        }
    }
}

internal fun Route.configureMessageRoutes(dependencies: ApplicationDependencies) {
    route("/messages/{messageId}") {
        route("/thread") {
            get {
                val (root, replies) = dependencies.queryService.getThread(call.messageId())
                call.respond(ThreadResponse(root = root.toResponse(), replies = replies.map { it.toResponse() }))
            }
        }

        route("/reactions") {
            route("/toggle") {
                post {
                    val request = call.receive<ToggleReactionRequest>()
                    val updated = dependencies.commandService.toggleReaction(
                        messageId = call.messageId(),
                        memberId = Uuid.parse(request.memberId),
                        emoji = request.emoji
                    )
                    call.respond(updated.toResponse())
                }
            }
        }
    }
}

internal fun Route.configureLinkPreviewRoutes(dependencies: ApplicationDependencies) {
    route("/link-preview") {
        route("/resolve") {
            post {
                val request = call.receive<ResolveLinkPreviewRequest>()
                call.respond(
                    ResolveLinkPreviewResponse(
                        preview = dependencies.linkPreviewResolver.resolve(request.url)?.toResponse()
                    )
                )
            }
        }

        route("/image") {
            get {
                val url = call.request.queryParameters["url"] ?: error("url is required")
                val image = dependencies.linkPreviewResolver.fetchImage(url)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respondBytes(
                    bytes = image.bytes,
                    contentType = ContentType.parse(image.contentType)
                )
            }
        }
    }
}

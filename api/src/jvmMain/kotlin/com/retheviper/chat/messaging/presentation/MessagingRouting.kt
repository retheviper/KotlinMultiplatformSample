@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.retheviper.chat.messaging.presentation

import com.retheviper.chat.wiring.ApplicationDependencies
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.sendSerialized
import kotlin.uuid.Uuid

fun Application.configureMessagingRouting(dependencies: ApplicationDependencies) {
    val sessions = ChannelSessions()
    routing {
        route("/api/v1") {
            configureWorkspaceRoutes(dependencies)
            configureMemberRoutes(dependencies)
            configureChannelRoutes(dependencies)
            configureMessageRoutes(dependencies)
            configureLinkPreviewRoutes(dependencies)
        }
        configureChatSocketRoutes(dependencies, sessions)
    }
    configureMessagingMcpRouting(dependencies)
}

internal class ChannelSessions {
    private val sessions =
        java.util.concurrent.ConcurrentHashMap<Uuid, java.util.concurrent.CopyOnWriteArraySet<io.ktor.server.websocket.DefaultWebSocketServerSession>>()

    fun join(channelId: Uuid, session: io.ktor.server.websocket.DefaultWebSocketServerSession) {
        sessions.computeIfAbsent(channelId) { java.util.concurrent.CopyOnWriteArraySet() }.add(session)
    }

    fun leave(channelId: Uuid, session: io.ktor.server.websocket.DefaultWebSocketServerSession) {
        sessions[channelId]?.remove(session)
    }

    suspend fun broadcast(channelId: Uuid, event: com.retheviper.chat.contract.ChatEvent) {
        sessions[channelId]?.forEach { session ->
            session.sendSerialized(event)
        }
    }
}

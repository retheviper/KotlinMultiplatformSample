package com.retheviper.bbs.message.presentation.route

import com.retheviper.bbs.common.domain.model.AuthUser
import com.retheviper.bbs.common.exception.AuthenticationException
import com.retheviper.bbs.common.extension.authUser
import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.extension.getIdFromPathParameter
import com.retheviper.bbs.common.extension.getQueryParameter
import com.retheviper.bbs.common.value.MessageGroupId
import com.retheviper.bbs.constant.MESSAGE
import com.retheviper.bbs.message.domain.model.Connection
import com.retheviper.bbs.message.domain.model.Message
import com.retheviper.bbs.message.domain.model.MessageGroup
import com.retheviper.bbs.message.domain.service.MessageService
import com.retheviper.bbs.model.request.CreateMessageGroupRequest
import com.retheviper.bbs.model.response.ListMessagesResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.util.collections.ConcurrentMap
import io.ktor.util.collections.ConcurrentSet
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import org.koin.ktor.ext.inject
import java.time.LocalDateTime

fun Route.routeMessage() {

    // management message group's connections
    val messageGroupConnections = ConcurrentMap<MessageGroupId, MutableSet<Connection>>()
    val service by inject<MessageService>()

    route(MESSAGE) {
        authenticate("auth-jwt") {
            get("/latest") {
                val userId = call.authUser?.id ?: throw AuthenticationException("Authentication failed.")
                val messages = service.findLatestMessages(userId)
                call.respond(messages.map { ListMessagesResponse.from(it) })
            }

            route("/group") {
                get("/{messageGroupId}") {
                    val messageGroupId = call.getIdFromPathParameter<MessageGroupId>()
                    val lastUpdatedTime = call.request.getQueryParameter<LocalDateTime>("lastUpdatedTime")
                    val userId = call.authUser?.id ?: throw AuthenticationException("Authentication failed.")
                    val messages = service.findGroupMessages(messageGroupId, userId, lastUpdatedTime)
                    call.respond(messages.map { ListMessagesResponse.from(it) })
                }

                post {
                    val request = call.receive<CreateMessageGroupRequest>()
                    val ownerId = call.authUser?.id ?: throw AuthenticationException("Authentication failed.")
                    service.createGroup(MessageGroup.from(request, ownerId))
                    call.respond(HttpStatusCode.Created)
                }

                webSocket("/{messageGroupId}") {
                    val messageGroupId = call.getIdFromPathParameter<MessageGroupId>()
                    val messageGroup = service.findGroup(messageGroupId)
                    val userInfo = call.authUser ?: throw AuthenticationException("Authentication failed.")

                    handleWebsocket(messageGroup, userInfo, messageGroupConnections, messageGroupId, service)
                }
            }

            // TODO kick user from message group (only owner)
            // TODO quit message group
        }
    }
}

private suspend fun DefaultWebSocketServerSession.handleWebsocket(
    messageGroup: MessageGroup,
    userInfo: AuthUser,
    messageGroupConnections: ConcurrentMap<MessageGroupId, MutableSet<Connection>>,
    messageGroupId: MessageGroupId,
    service: MessageService
) {
    if (messageGroup.members.contains(userInfo.id).not()) {
        close()
        return
    }

    val connection = Connection(this, userInfo.id, userInfo.username)
    val connections = messageGroupConnections.getOrPut(messageGroupId) { ConcurrentSet() }
    connections.add(connection)

    send(Frame.Text("Connected"))

    try {
        for (frame in incoming) {
            frame as? Frame.Text ?: continue

            val message = Message(
                messageGroupId = messageGroupId,
                userId = userInfo.id,
                username = userInfo.username,
                content = frame.readText()
            ).let {
                service.createMessage(it)
            }

            connections.forEach {
                it.session.send(Frame.Text("[${userInfo.username}]:\n${message.content}"))
            }
        }
    } catch (e: Exception) {
        connections.remove(connection)
        connections.forEach {
            it.session.send(Frame.Text("User ${connection.username} left because of error"))
        }
    } finally {
        connections.remove(connection)
        connections.forEach {
            it.session.send(Frame.Text("User ${connection.username} left"))
        }
        close()
        if (connections.isEmpty()) {
            messageGroupConnections.remove(messageGroupId)
        }
    }
}
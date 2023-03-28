package com.retheviper.bbs.message.route

import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.extension.getIdFromPathParameter
import com.retheviper.bbs.common.extension.getQueryParameter
import com.retheviper.bbs.common.extension.getUserInfoFromToken
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
                val userId = call.getUserInfoFromToken().first
                val messages = service.findLatestMessages(userId)
                call.respond(messages.map { ListMessagesResponse.from(it) })
            }

            get("/group/{messageGroupId}") {
                val messageGroupId = call.getIdFromPathParameter<MessageGroupId>()
                val lastUpdatedTime = call.request.getQueryParameter<LocalDateTime>("lastUpdatedTime")
                val userId = call.getUserInfoFromToken().first
                val messages = service.findGroupMessages(messageGroupId, userId, lastUpdatedTime)
                call.respond(messages.map { ListMessagesResponse.from(it) })
            }

            post("/group") {
                val request = call.receive<CreateMessageGroupRequest>()
                val ownerId = call.getUserInfoFromToken().first
                service.createGroup(MessageGroup.from(request, ownerId))
                call.respond(HttpStatusCode.Created)
            }

            webSocket("/group/{messageGroupId}") {
                val messageGroupId = call.getIdFromPathParameter<MessageGroupId>()
                val messageGroup = service.findGroup(messageGroupId)
                val userInfo = call.getUserInfoFromToken()

                if (messageGroup.members.contains(userInfo.first).not()) {
                    close()
                    return@webSocket
                }

                val connection = Connection(this, userInfo.first, userInfo.second)
                val connections = messageGroupConnections.getOrPut(messageGroupId) { ConcurrentSet() }
                connections.add(connection)

                send(Frame.Text("Connected"))

                try {
                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue

                        val message = Message(
                            messageGroupId = messageGroupId,
                            userId = userInfo.first,
                            username = userInfo.second,
                            content = frame.readText()
                        ).let {
                            service.createMessage(it)
                        }

                        connections.forEach {
                            it.session.send(Frame.Text("[${userInfo.second}]:\n${message.content}"))
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
                    messageGroupConnections.remove(messageGroupId)
                }
            }

            // TODO kick user from message group (only owner)
            // TODO quit message group
        }
    }
}
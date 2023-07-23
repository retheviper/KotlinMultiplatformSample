package com.retheviper.bbs.user.presentation.route

import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.extension.getIdFromPathParameter
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.constant.USER
import com.retheviper.bbs.model.request.CreateUserRequest
import com.retheviper.bbs.model.request.UpdateUserRequest
import com.retheviper.bbs.model.response.UserResponse
import com.retheviper.bbs.user.domain.model.User
import com.retheviper.bbs.user.domain.usecase.UserUseCase
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

fun Route.routeUser() {

    val usecase by inject<UserUseCase>()

    route(USER) {
        get("/{userId}") {
            val id = call.getIdFromPathParameter<UserId>()

            val response = transaction { usecase.find(id) }
                .let { UserResponse.from(it) }

            call.respond(response)
        }

        post {
            val request = call.receive<CreateUserRequest>()

            val response = transaction { usecase.create(User.from(request)) }
                .let { UserResponse.from(it) }

            call.application.log.info("User created: ${response.username}")
            call.respond(response)
        }

        authenticate("auth-jwt") {
            route("/{userId}") {
                put {
                    val id = call.getIdFromPathParameter<UserId>()
                    val request = call.receive<UpdateUserRequest>()

                    val response = transaction { usecase.update(User.from(id, request)) }
                        .let { UserResponse.from(it) }

                    call.application.log.info("User updated: ${response.username}")
                    call.respond(response)
                }

                delete {
                    val id = call.getIdFromPathParameter<UserId>()

                    transaction { usecase.delete(id) }

                    call.application.log.info("User deleted: $id")
                    call.respond("User deleted: $id")
                }
            }
        }
    }
}
package com.retheviper.bbs.user.presentation.route

import com.retheviper.bbs.common.extension.getIdFromPathParameter
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.constant.USER
import com.retheviper.bbs.model.request.CreateUserRequest
import com.retheviper.bbs.user.presentation.controller.UserController
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

fun Route.routeUser() {

    val controller by inject<UserController>()

    route(USER) {
        get("/{userId}") {
            val id = call.getIdFromPathParameter<UserId>()

            val response = transaction { controller.find(id) }
            call.respond(response)
        }

        post {
            val request = call.receive<CreateUserRequest>()

            val response = transaction { controller.create(request) }
            call.application.log.info("User created: ${response.username}")
            call.respond(response)
        }
    }
}
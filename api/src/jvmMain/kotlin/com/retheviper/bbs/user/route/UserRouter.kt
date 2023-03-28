package com.retheviper.bbs.user.route

import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.extension.getIdFromPathParameter
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.constant.USER
import com.retheviper.bbs.model.request.CreateUserRequest
import com.retheviper.bbs.model.response.GetUserResponse
import com.retheviper.bbs.user.domain.model.User
import com.retheviper.bbs.user.domain.service.UserService
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.routeUser() {

    val service by inject<UserService>()

    route(USER) {
        get("/{userId}") {
            val id = call.getIdFromPathParameter<UserId>()

            val user = service.find(id)

            call.respond(GetUserResponse.from(user))
        }

        post {
            val request = call.receive<CreateUserRequest>()

            val user = service.create(User.from(request))

            user?.let {
                call.application.log.info("User created: ${user.username}")
                call.respond(GetUserResponse.from(it))
            }
        }
    }
}
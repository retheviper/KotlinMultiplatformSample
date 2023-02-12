package com.retheviper.bbs.user.web.route

import com.retheviper.bbs.user.domain.model.UserDto
import com.retheviper.bbs.user.domain.service.UserService
import com.retheviper.bbs.user.web.model.request.CreateUserRequest
import com.retheviper.bbs.user.web.model.response.GetUserResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Routing.user() {

    val service by inject<UserService>()

    route("/user") {
        get("/{id}") {
            val id = call.parameters["id"]?.toInt()

            if (id == null) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = "Invalid id"
                )
                return@get
            }

            val user = service.getUser(id)
            user?.let {
                call.respond(GetUserResponse.from(it))
            } ?: run {
                call.respond(
                    status = HttpStatusCode.NotFound,
                    message = "User not found"
                )
            }
        }

        post {
            val request = call.receive<CreateUserRequest>()

            val user = try {
                service.createUser(UserDto.from(request))
            } catch (e: IllegalArgumentException) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = e.message.toString()
                )
                return@post
            } catch (e: Exception) {
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = "Internal server error"
                )
                return@post
            }

            user?.let {
                call.respond(GetUserResponse.from(it))
            }
        }
    }
}
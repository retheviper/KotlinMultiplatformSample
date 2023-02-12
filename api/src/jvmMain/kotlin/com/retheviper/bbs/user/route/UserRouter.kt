package com.retheviper.bbs.user.route

import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.extension.default
import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.constant.ErrorCode
import com.retheviper.bbs.constant.USER
import com.retheviper.bbs.model.request.CreateUserRequest
import com.retheviper.bbs.model.response.ExceptionResponse
import com.retheviper.bbs.model.response.GetUserResponse
import com.retheviper.bbs.user.domain.model.UserDto
import com.retheviper.bbs.user.domain.service.UserService
import io.ktor.http.HttpStatusCode
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
        get("/{id}") {
            val id = call.parameters["id"]?.toInt()

            if (id == null) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ExceptionResponse(
                        code = ErrorCode.INVALID_PARAMETER.value,
                        message = "Invalid ID"
                    )
                )
                return@get
            }

            val user = try {
                service.getUser(id)
            } catch (e: BadRequestException) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ExceptionResponse.from(e)
                )
                call.application.log.error("${e.message}: $id")
                return@get
            } catch (e: Exception) {
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ExceptionResponse.default()
                )
                call.application.log.error("Internal server error: ${e.message}")
                return@get
            }

            call.respond(GetUserResponse.from(user))
        }

        post {
            val request = call.receive<CreateUserRequest>()

            val user = try {
                service.createUser(UserDto.from(request))
            } catch (e: BadRequestException) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ExceptionResponse.from(e)
                )
                call.application.log.error("${e.message}: ${request.username}")
                return@post
            } catch (e: Exception) {
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ExceptionResponse.default()
                )
                call.application.log.error("Internal server error: ${e.message}")
                return@post
            }

            user?.let {
                call.application.log.info("User created: ${user.username}")
                call.respond(GetUserResponse.from(it))
            }
        }
    }
}
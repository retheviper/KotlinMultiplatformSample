package com.retheviper.bbs.user.web

import com.retheviper.bbs.user.domain.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Routing.user() {

    val service by inject<UserService>()

    get("/user/{id}") {
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
            call.respond(it)
        } ?: run {
            call.respond(
                status = HttpStatusCode.NotFound,
                message = "User not found"
            )
        }
    }
}
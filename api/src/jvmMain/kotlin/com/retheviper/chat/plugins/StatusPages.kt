package com.retheviper.chat.plugins

import com.retheviper.chat.contract.ApiErrorResponse
import com.retheviper.chat.messaging.domain.DomainValidationException
import com.retheviper.chat.messaging.domain.NotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<DomainValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(code = "VALIDATION_ERROR", message = cause.message ?: "validation error"))
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ApiErrorResponse(code = "NOT_FOUND", message = cause.message ?: "not found"))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(code = "BAD_REQUEST", message = cause.message ?: "bad request"))
        }
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ApiErrorResponse(code = "INTERNAL_ERROR", message = cause.message ?: "unexpected error"))
        }
    }
}

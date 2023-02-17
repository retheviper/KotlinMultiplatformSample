package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.constant.ErrorCode
import com.retheviper.bbs.model.response.ExceptionResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureExceptionHandling() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ExceptionResponse(
                    code = ErrorCode.INVALID_PARAMETER.value,
                    message = cause.reasons.first()
                )
            )
        }

        exception<BadRequestException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ExceptionResponse(
                    code = ErrorCode.INVALID_PARAMETER.value,
                    message = cause.message.toString()
                )
            )
        }
    }
}
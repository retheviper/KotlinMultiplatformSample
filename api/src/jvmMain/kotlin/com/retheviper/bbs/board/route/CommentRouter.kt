package com.retheviper.bbs.board.route

import com.retheviper.bbs.board.domain.service.CommentService
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.extension.getPaginationProperties
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.constant.COMMENT
import com.retheviper.bbs.model.response.ListCommentResponse
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.routeComment() {
    route(COMMENT) {
        val service by inject<CommentService>()
        get {
            val authorId = call.request.queryParameters["authorId"]?.toInt()?.let { UserId(it) } ?: run {
                throw BadRequestException("Author ID is required.")
            }

            val paginationProperties = call.getPaginationProperties()

            val dtos = service.findAll(
                authorId = authorId,
                paginationProperties = paginationProperties
            )

            call.respond(
                ListCommentResponse.from(
                    paginationProperties = paginationProperties,
                    dtos = dtos
                )
            )

            call.application.log.info("Comment list returned with page: ${paginationProperties.page}, size: ${paginationProperties.size}, limit: ${paginationProperties.limit}.")
        }
    }
}
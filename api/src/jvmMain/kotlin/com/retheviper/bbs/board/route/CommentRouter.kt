package com.retheviper.bbs.board.route

import com.retheviper.bbs.board.domain.service.CommentService
import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.extension.respondBadRequest
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
            val authorId = call.request.queryParameters["authorId"]?.toInt()

            if (authorId == null) {
                call.respondBadRequest("Invalid authorId.")
                return@get
            }

            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toInt() ?: 10
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 100

            val dtos = service.findAll(
                authorId = authorId,
                page = page,
                pageSize = pageSize,
                limit = limit
            )

            call.respond(
                ListCommentResponse.from(
                    page = page,
                    pageSize = pageSize,
                    limit = limit,
                    dtos = dtos
                )
            )

            call.application.log.info("Comment list returned with page: $page, pageSize: $pageSize, limit: $limit.")
        }
    }
}
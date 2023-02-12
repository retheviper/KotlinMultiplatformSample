package com.retheviper.bbs.board.route

import com.retheviper.bbs.board.domain.service.BoardService
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.extension.respondBadRequest
import com.retheviper.bbs.constant.BOARD
import com.retheviper.bbs.model.response.GetBoardResponse
import com.retheviper.bbs.model.response.ListBoardResponse
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.routeBoard() {

    val service by inject<BoardService>()

    route(BOARD) {
        get {
            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toInt() ?: 10
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 100

            val dtos = service.findAll(
                page = page,
                pageSize = pageSize,
                limit = limit
            )

            call.respond(
                ListBoardResponse.from(
                    page = page,
                    pageSize = pageSize,
                    limit = limit,
                    dtos = dtos
                )
            )
            call.application.log.info("Board list returned with page: $page, pageSize: $pageSize, limit: $limit.")
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toInt()

            if (id == null) {
                call.respondBadRequest("Invalid ID")
                return@get
            }

            val dto = try {
                service.find(id)
            } catch (e: BadRequestException) {
                call.respondBadRequest(e)
                call.application.log.error("${e.message}: $id")
                return@get
            }

            call.respond(GetBoardResponse.from(dto))
            call.application.log.info("Board returned with id: $id.")
        }
    }
}
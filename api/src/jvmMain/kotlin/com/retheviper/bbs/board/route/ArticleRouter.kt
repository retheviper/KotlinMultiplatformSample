package com.retheviper.bbs.board.route

import com.retheviper.bbs.board.domain.service.ArticleService
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.extension.respondBadRequest
import com.retheviper.bbs.constant.ARTICLE
import com.retheviper.bbs.model.response.GetArticleResponse
import com.retheviper.bbs.model.response.ListArticleResponse
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.routeArticle() {
    route(ARTICLE) {
        val service by inject<ArticleService>()
        get {
            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toInt() ?: 10
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 100

            if (page < 1 || pageSize < 1 || limit < 1) {
                call.respondBadRequest("Invalid page, pageSize or limit.")
                return@get
            }

            if (pageSize > limit) {
                call.respondBadRequest("pageSize must be less than or equal to limit.")
                return@get
            }

            if (page > limit / pageSize) {
                call.respondBadRequest("page must be less than or equal to limit / pageSize.")
                return@get
            }

            val authorId = call.request.queryParameters["authorId"]?.toInt()

            val dtos = if (authorId == null) {
                service.findAll(
                    page = page,
                    pageSize = pageSize,
                    limit = limit
                )
            } else {
                service.findAll(
                    authorId = authorId,
                    page = page,
                    pageSize = pageSize,
                    limit = limit
                )
            }

            call.respond(
                ListArticleResponse.from(
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

            call.respond(GetArticleResponse.from(dto))
            call.application.log.info("Board returned with id: $id.")
        }
    }
}
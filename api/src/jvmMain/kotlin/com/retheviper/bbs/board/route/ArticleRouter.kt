package com.retheviper.bbs.board.route

import com.retheviper.bbs.board.domain.service.ArticleService
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.extension.getIdFromParameter
import com.retheviper.bbs.common.extension.getPaginationProperties
import com.retheviper.bbs.common.extension.respondBadRequest
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.UserId
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
            val (page, pageSize, limit) = try {
                call.getPaginationProperties()
            } catch (e: BadRequestException) {
                call.respondBadRequest(e)
                call.application.log.error(e.message)
                return@get
            }

            val authorId = call.request.queryParameters["authorId"]?.toInt()?.let { UserId(it) }

            call.application.log.info("authorId: $authorId, page: $page, pageSize: $pageSize, limit: $limit.")

            val dtos = service.findAll(
                authorId = authorId,
                page = page,
                pageSize = pageSize,
                limit = limit
            )

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
            val id = try {
                call.getIdFromParameter()
            } catch (e: BadRequestException) {
                call.respondBadRequest(e)
                call.application.log.error(e.message)
                return@get
            }.let {
                ArticleId(it)
            }

            val article = try {
                service.find(id)
            } catch (e: BadRequestException) {
                call.respondBadRequest(e)
                call.application.log.error("${e.message}: $id")
                return@get
            }

            call.respond(GetArticleResponse.from(article))
            call.application.log.info("Board returned with id: $id.")
        }
    }
}
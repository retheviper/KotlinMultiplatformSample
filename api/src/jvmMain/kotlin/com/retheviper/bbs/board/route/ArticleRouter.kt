package com.retheviper.bbs.board.route

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.service.ArticleService
import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.extension.getIdFromParameter
import com.retheviper.bbs.common.extension.getPaginationProperties
import com.retheviper.bbs.common.extension.getUserInfoFromToken
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.constant.ARTICLE
import com.retheviper.bbs.model.request.CreateArticleRequest
import com.retheviper.bbs.model.response.GetArticleResponse
import com.retheviper.bbs.model.response.ListArticleResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.routeArticle() {
    route("/{boardId}$ARTICLE") {
        val service by inject<ArticleService>()

        get {
            val boardId = call.getIdFromParameter<BoardId>()
            val paginationProperties = call.getPaginationProperties()

            val authorId = call.request.queryParameters["authorId"]?.toInt()?.let { UserId(it) }

            call.application.log.info("authorId: $authorId, page: ${paginationProperties.page}, size: ${paginationProperties.size}, limit: ${paginationProperties.limit}.")

            val dtos = service.findAll(
                boardId = boardId,
                authorId = authorId,
                paginationProperties = paginationProperties
            )

            call.respond(
                ListArticleResponse.from(
                    paginationProperties = paginationProperties, dtos = dtos
                )
            )
            call.application.log.info("Board list returned with page: ${paginationProperties.page}, size: ${paginationProperties.size}, limit: ${paginationProperties.limit}.")
        }

        get("/{articleId}") {
            val articleId = call.getIdFromParameter<ArticleId>()

            val principal = call.principal<JWTPrincipal>()
            val username = principal?.payload?.getClaim("username")?.asString()

            val article = service.find(
                id = articleId,
                username = username
            )

            call.respond(GetArticleResponse.from(article))
            call.application.log.info("Board returned with id: $articleId.")
        }

        authenticate("auth-jwt") {
            post {
                val boardId = call.getIdFromParameter<BoardId>()
                val authorId = call.getUserInfoFromToken().first
                val request = call.receive<CreateArticleRequest>()

                val id = service.create(Article.from(boardId, authorId, request))

                call.respond(HttpStatusCode.Created)
                call.application.log.info("Board created with id: ${id.value}.")
            }
        }
    }
}
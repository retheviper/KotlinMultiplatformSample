package com.retheviper.bbs.board.presentation.route

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.usecase.ArticleUseCase
import com.retheviper.bbs.common.exception.AuthenticationException
import com.retheviper.bbs.common.extension.authUser
import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.extension.getIdFromPathParameter
import com.retheviper.bbs.common.extension.getPaginationProperties
import com.retheviper.bbs.common.extension.getSubPaginationProperties
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.constant.ARTICLE
import com.retheviper.bbs.model.request.CreateArticleRequest
import com.retheviper.bbs.model.request.UpdateArticleRequest
import com.retheviper.bbs.model.response.ArticleResponse
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
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

fun Route.routeArticle() {
    route(ARTICLE) {

        val usecase by inject<ArticleUseCase>()

        get {
            val boardId = call.getIdFromPathParameter<BoardId>()
            val paginationProperties = call.getPaginationProperties()
            val authorId = call.request.queryParameters["authorId"]?.toInt()?.let { UserId(it) }

            call.application.log.info("authorId: $authorId, page: ${paginationProperties.page}, size: ${paginationProperties.size}, limit: ${paginationProperties.limit}.")

            val response = transaction {
                usecase.findBy(
                    boardId = boardId,
                    authorId = authorId,
                    paginationProperties = paginationProperties
                )
            }.let {
                ListArticleResponse.from(paginationProperties, it)
            }

            call.respond(response)
            call.application.log.info("Board list returned with page: ${paginationProperties.page}, size: ${paginationProperties.size}, limit: ${paginationProperties.limit}.")
        }

        authenticate("auth-jwt") {
            post {
                val boardId = call.getIdFromPathParameter<BoardId>()
                val authorId = call.authUser?.id ?: throw AuthenticationException("User id not found.")
                val request = call.receive<CreateArticleRequest>()

                val article = transaction {
                    usecase.create(Article.from(boardId, authorId, request))
                }.let { ArticleResponse.from(it) }

                call.respond(HttpStatusCode.Created)
                call.application.log.info("Board created with id: ${article.id}.")
            }
        }

        route("/{articleId}") {

            routeComment()

            get {
                val articleId = call.getIdFromPathParameter<ArticleId>()
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()?.let { UserId(it) }
                val subPaginationProperties = call.getSubPaginationProperties()

                call.application.log.info("articleId: $articleId, page: ${subPaginationProperties.page}, size: ${subPaginationProperties.size}, limit: ${subPaginationProperties.limit}.")

                val response = transaction {
                    usecase.find(
                        id = articleId,
                        userId = userId,
                        subPaginationProperties = subPaginationProperties
                    )
                }.let { ArticleResponse.from(it) }

                call.respond(response)
                call.application.log.info("Board returned with id: $articleId.")
            }

            authenticate("auth-jwt") {
                put {
                    val articleId = call.getIdFromPathParameter<ArticleId>()
                    val authorId = call.authUser?.id ?: throw AuthenticationException("User id not found.")
                    val request = call.receive<UpdateArticleRequest>()

                    transaction {
                        usecase.update(Article.from(articleId, authorId, request))
                    }.let { ArticleResponse.from(it) }

                    call.respond(HttpStatusCode.OK)
                    call.application.log.info("Board updated with id: ${articleId.value}.")
                }

                delete {
                    val articleId = call.getIdFromPathParameter<ArticleId>()
                    val password =
                        call.parameters["password"] ?: throw IllegalArgumentException("Password is required.")

                    transaction {
                        usecase.delete(
                            id = articleId,
                            password = password
                        )
                    }

                    call.respond(HttpStatusCode.OK)
                    call.application.log.info("Board deleted with id: ${articleId.value}.")
                }
            }
        }
    }
}
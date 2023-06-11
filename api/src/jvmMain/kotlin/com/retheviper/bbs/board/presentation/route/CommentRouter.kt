package com.retheviper.bbs.board.presentation.route

import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.domain.usecase.CommentUseCase
import com.retheviper.bbs.common.exception.AuthenticationException
import com.retheviper.bbs.common.extension.authUser
import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.extension.getIdFromPathParameter
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CommentId
import com.retheviper.bbs.constant.COMMENT
import com.retheviper.bbs.model.request.CreateCommentRequest
import com.retheviper.bbs.model.request.UpdateCommentRequest
import com.retheviper.bbs.model.response.CommentResponse
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

fun Route.routeComment() {
    route(COMMENT) {
        val usecase by inject<CommentUseCase>()

        authenticate("auth-jwt") {
            post {
                val articleId = call.getIdFromPathParameter<ArticleId>()
                val request = call.receive<CreateCommentRequest>()

                val response = transaction {
                    usecase.create(Comment.from(articleId, request))
                }.let {
                    CommentResponse.from(it)
                }

                call.respond(response)
                call.application.log.info("Comment returned with id: ${response.id}.")
            }
        }

        route("/{commentId}") {
            authenticate("auth-jwt") {
                put {
                    val articleId = call.getIdFromPathParameter<ArticleId>()
                    val commentId = call.getIdFromPathParameter<CommentId>()
                    val userId = call.authUser?.id ?: throw AuthenticationException("User id not found.")
                    val request = call.receive<UpdateCommentRequest>()

                    val response = transaction {
                        usecase.update(Comment.from(articleId, commentId, userId, request))
                    }.let {
                        CommentResponse.from(it)
                    }

                    call.respond(response)
                    call.application.log.info("Comment returned with id: ${response.id}.")
                }
            }
        }
    }
}
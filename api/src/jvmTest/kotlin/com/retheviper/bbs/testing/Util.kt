package com.retheviper.bbs.testing

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.infrastructure.repository.ArticleRepository
import com.retheviper.bbs.board.infrastructure.repository.CommentRepository
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.user.domain.model.User
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

fun Application.insertTestData() {
    val userRepository by inject<UserRepository>()
    val articleRepository by inject<ArticleRepository>()
    val commentRepository by inject<CommentRepository>()

    transaction {
        userRepository.create(
            User(
                username = "testuser",
                password = "1234",
                name = "test user",
                mail = "test_user@test.com"
            )
        )

        repeat(100) { articleId ->
            articleRepository.create(
                Article(
                    title = "test title $articleId",
                    content = "test content $articleId",
                    password = "1234",
                    authorId = UserId(1)
                )
            )
            repeat(articleId) {
                if ((articleId % 2) == 0) {
                    commentRepository.create(
                        Comment(
                            articleId = ArticleId(articleId),
                            content = "test comment $it",
                            password = "1234",
                            authorId = UserId(1)
                        )
                    )
                }
            }
        }
    }
}
package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.infrastructure.ArticleRepository
import com.retheviper.bbs.board.infrastructure.CommentRepository
import com.retheviper.bbs.common.infrastructure.table.Articles
import com.retheviper.bbs.common.infrastructure.table.Comments
import com.retheviper.bbs.common.infrastructure.table.Users
import com.retheviper.bbs.user.domain.model.User
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

fun Application.configurePersistent() {

    Database.connect(
        url = "jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver"
    )

    val userRepository by inject<UserRepository>()
    val articleRepository by inject<ArticleRepository>()
    val commentRepository by inject<CommentRepository>()

    transaction {
        SchemaUtils.create(Users, Articles, Comments)

        userRepository.create(
            User(
                username = "test_user",
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
                    authorId = 1
                )
            )
            repeat(articleId) {
                commentRepository.create(
                    Comment(
                        boardId = articleId,
                        content = "test comment $it",
                        password = "1234",
                        authorId = 1
                    )
                )
            }
        }
    }
}
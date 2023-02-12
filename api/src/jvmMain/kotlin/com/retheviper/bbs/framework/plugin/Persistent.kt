package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.board.domain.model.Board
import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.infrastructure.BoardRepository
import com.retheviper.bbs.board.infrastructure.CommentRepository
import com.retheviper.bbs.common.infrastructure.table.Boards
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
    val boardRepository by inject<BoardRepository>()
    val commentRepository by inject<CommentRepository>()

    transaction {
        SchemaUtils.create(Users, Boards, Comments)

        userRepository.create(
            User(
                username = "test_user",
                password = "1234",
                name = "test user",
                mail = "test_user@test.com"
            )
        )

        repeat(100) { boardId ->
            boardRepository.create(
                Board(
                    title = "test title $boardId",
                    content = "test content $boardId",
                    password = "1234",
                    authorId = 1
                )
            )
            repeat(boardId) {
                commentRepository.create(
                    Comment(
                        boardId = boardId,
                        content = "test comment $it",
                        password = "1234",
                        authorId = 1
                    )
                )
            }
        }
    }
}
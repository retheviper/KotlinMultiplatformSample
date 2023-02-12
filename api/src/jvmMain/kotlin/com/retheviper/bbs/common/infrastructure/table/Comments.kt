package com.retheviper.bbs.common.infrastructure.table

object Comments : Audit() {
    val content = text("content")
    val password = varchar("password", 255)
    val authorId = reference("author_id", Users)
    val boardId = reference("board_id", Boards)
}
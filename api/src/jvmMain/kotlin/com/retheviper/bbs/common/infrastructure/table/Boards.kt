package com.retheviper.bbs.common.infrastructure.table

object Boards : Audit() {
    val title = varchar("title", 255)
    val content = text("content")
    val password = varchar("password", 255)
    val authorId = reference("author_id", Users)
}
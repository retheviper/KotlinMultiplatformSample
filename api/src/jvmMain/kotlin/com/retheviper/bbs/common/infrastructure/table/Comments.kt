package com.retheviper.bbs.common.infrastructure.table

import org.jetbrains.exposed.sql.ReferenceOption

object Comments : Audit() {
    val content = text("content")
    val password = varchar("password", 255)
    val authorId = reference("author_id", Users)
    val articleId = reference("article_id", Articles, onDelete = ReferenceOption.CASCADE)
}
package com.retheviper.bbs.common.infrastructure.table

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table.Dual.reference
import org.jetbrains.exposed.sql.Table.Dual.uinteger

object Articles : Audit() {
    val title = varchar("title", 255)
    val content = text("content")
    val password = varchar("password", 255)
    val authorId = reference("author_id", Users)
    val categoryId = reference("category_id", Categories).nullable()
    val viewCount = uinteger("view_count").default(0u)
    val likeCount = uinteger("like_count").default(0u)
    val dislikeCount = uinteger("dislike_count").default(0u)
    val boardId = reference("board_id", Boards, onDelete = ReferenceOption.CASCADE)
}
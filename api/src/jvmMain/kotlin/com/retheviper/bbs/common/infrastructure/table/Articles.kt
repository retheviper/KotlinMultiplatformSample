package com.retheviper.bbs.common.infrastructure.table

object Articles : Audit() {
    val title = varchar("title", 255)
    val content = text("content")
    val password = varchar("password", 255)
    val authorId = reference("author_id", Users)
    val categoryId = reference("category_id", Categories)
    val viewCount = integer("view_count").default(0)
    val likeCount = integer("like_count").default(0)
    val dislikeCount = integer("dislike_count").default(0)
}
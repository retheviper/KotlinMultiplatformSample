package com.retheviper.bbs.board.infrastructure

import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.common.infrastructure.table.Comments
import org.jetbrains.exposed.sql.insert
import java.time.LocalDateTime

class CommentRepository {

    fun create(comment: Comment) {
        Comments.insert {
            it[content] = comment.content
            it[password] = comment.password
            it[authorId] = comment.authorId
            it[boardId] = comment.boardId
            it[createdBy] = comment.authorName ?: ""
            it[createdDate] = LocalDateTime.now()
            it[lastModifiedBy] = comment.authorName ?: ""
            it[lastModifiedDate] = LocalDateTime.now()
            it[deleted] = false
        }
    }
}
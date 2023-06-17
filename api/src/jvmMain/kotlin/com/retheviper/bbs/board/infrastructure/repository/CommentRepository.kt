package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.infrastructure.model.CommentRecord
import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.extension.updateAuditInfos
import com.retheviper.bbs.common.extension.withPagination
import com.retheviper.bbs.common.infrastructure.table.Comments
import com.retheviper.bbs.common.infrastructure.table.Users
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CommentId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.model.common.PaginationProperties
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class CommentRepository {

    fun findBy(articleIds: List<ArticleId>): List<CommentRecord> {
        return Comments.leftJoin(Users, { authorId }, { Users.id })
            .slice(Comments.columns + Users.name)
            .select { (Comments.articleId inList articleIds.map { it.value }) and (Comments.deleted eq false) }
            .map { it.toRecord() }
    }

    fun findBy(articleId: ArticleId, paginationProperties: PaginationProperties? = null): List<CommentRecord> {
        return Comments.leftJoin(Users, { authorId }, { Users.id })
            .slice(Comments.columns + Users.name)
            .select { (Comments.articleId eq articleId.value) and (Comments.deleted eq false) }
            .apply { paginationProperties?.let { withPagination(it) } }
            .map { it.toRecord() }
    }

    fun findBy(authorId: UserId, paginationProperties: PaginationProperties): List<CommentRecord> {
        return Comments.select { (Comments.authorId eq authorId.value) and (Comments.deleted eq false) }
            .withPagination(paginationProperties)
            .map { it.toRecord() }
    }

    fun find(id: CommentId): CommentRecord? {
        return Comments.select { (Comments.id eq id.value) and (Comments.deleted eq false) }
            .map { it.toRecord() }
            .firstOrNull()
    }

    fun create(comment: Comment): CommentId {
        val id = Comments.insertAndGetId {
            it[content] = comment.content
            it[password] = comment.password.toHashedString()
            it[authorId] = comment.authorId.value
            it[articleId] = comment.articleId.value
            insertAuditInfos(it, comment.authorName ?: "")
        }

        return CommentId(id.value)
    }

    fun update(comment: Comment) {
        Comments.update({ Comments.id eq comment.id?.value }) {
            it[content] = comment.content
            updateAuditInfos(it, comment.authorName ?: "")
        }
    }

    fun delete(id: CommentId) {
        Comments.update({ Comments.id eq id.value }) {
            it[deleted] = true
        }
    }

    private fun ResultRow.toRecord(): CommentRecord {
        return CommentRecord(
            articleId = ArticleId(this[Comments.articleId].value),
            id = CommentId(this[Comments.id].value),
            content = this[Comments.content],
            password = this[Comments.password],
            authorId = UserId(this[Comments.authorId].value),
            authorName = this[Users.name]
        )
    }
}
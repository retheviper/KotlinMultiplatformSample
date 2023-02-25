package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.extension.updateAuditInfos
import com.retheviper.bbs.common.infrastructure.table.Articles
import com.retheviper.bbs.common.infrastructure.table.Articles.authorId
import com.retheviper.bbs.common.infrastructure.table.Users
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CategoryId
import com.retheviper.bbs.common.value.UserId
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class ArticleRepository {

    fun count(authorId: UserId?): Long {
        return Articles
            .slice(Articles.id)
            .select(selectOperator(authorId))
            .count()
    }

    fun findAll(authorId: UserId?, page: Int, pageSize: Int, limit: Int): List<ArticleRecord> {
        return Articles
            .leftJoin(Users, { Articles.authorId }, { Users.id })
            .slice(Articles.columns + Users.name)
            .select(selectOperator(authorId))
            .limit(limit)
            .limit(pageSize, ((page - 1) * pageSize).toLong())
            .orderBy(Articles.id, SortOrder.ASC)
            .map { it.toRecord() }
    }

    fun find(id: ArticleId): ArticleRecord? {
        return Articles
            .leftJoin(Users, { authorId }, { Users.id })
            .slice(Articles.columns + Users.name)
            .select { (Articles.id eq id.value) and (Articles.deleted eq false) }
            .map { it.toRecord() }
            .firstOrNull()
    }

    fun findAuthorName(authorId: UserId): String {
        return Users
            .slice(Users.name)
            .select { Users.id eq authorId.value }
            .map { it[Users.name] }
            .first()
    }

    fun create(article: Article): ArticleId {
        val id = Articles.insertAndGetId {
            it[title] = article.title
            it[content] = article.content
            it[password] = article.password
            it[authorId] = article.authorId.value
            it[categoryId] = article.category?.id?.value ?: 0
            insertAuditInfos(it, article.authorName ?: "")
        }.value
        return ArticleId(id)
    }

    fun update(article: Article) {
        Articles.update({ Articles.id eq article.id?.value }) {
            it[title] = article.title
            it[content] = article.content
            it[password] = article.password
            it[categoryId] = article.category?.id?.value ?: 0
            updateAuditInfos(it, article.authorName ?: "")
        }
    }

    fun delete(id: ArticleId) {
        Articles.update({ Articles.id eq id.value }) {
            it[deleted] = true
        }
    }


    private fun selectOperator(authorId: UserId?) = if (authorId != null) {
        (Articles.authorId eq authorId.value) and (Articles.deleted eq false)
    } else {
        Articles.deleted eq false
    }

    private fun ResultRow.toRecord() = ArticleRecord(
        id = ArticleId(this[Articles.id].value),
        title = this[Articles.title],
        content = this[Articles.content],
        password = this[Articles.password],
        authorId = UserId(this[authorId].value),
        authorName = this[Users.name],
        categoryId =  CategoryId(this[Articles.categoryId].value),
        likeCount = this[Articles.likeCount],
        dislikeCount = this[Articles.dislikeCount],
        viewCount = this[Articles.viewCount],
        createdDate = this[Articles.createdDate],
        updatedDate = this[Articles.lastModifiedDate],
    )
}
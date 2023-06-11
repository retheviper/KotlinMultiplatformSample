package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.extension.updateAuditInfos
import com.retheviper.bbs.common.extension.withPagination
import com.retheviper.bbs.common.infrastructure.table.Articles
import com.retheviper.bbs.common.infrastructure.table.Articles.authorId
import com.retheviper.bbs.common.infrastructure.table.Articles.categoryId
import com.retheviper.bbs.common.infrastructure.table.Categories
import com.retheviper.bbs.common.infrastructure.table.Users
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.CategoryId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.model.common.PaginationProperties
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class ArticleRepository {

    fun exists(id: ArticleId): Boolean {
        return Articles.select { Articles.id eq id.value }.count() > 0
    }

    fun findBy(boardId: BoardId, authorId: UserId?, paginationProperties: PaginationProperties): List<ArticleRecord> {
        return Articles.leftJoin(Users, { Articles.authorId }, { Users.id })
            .leftJoin(Categories, { categoryId }, { Categories.id })
            .slice(Articles.columns + Users.name + Categories.name)
            .select(selectOperator(boardId = boardId, authorId = authorId))
            .withPagination(paginationProperties)
            .orderBy(Articles.id, SortOrder.ASC)
            .map { it.toRecord() }
    }

    fun find(id: ArticleId, forUpdate: Boolean): ArticleRecord? {
        return Articles.leftJoin(Users, { authorId }, { Users.id })
            .leftJoin(Categories, { categoryId }, { Categories.id })
            .slice(Articles.columns + Users.name + Categories.name)
            .select { (Articles.id eq id.value) and (Articles.deleted eq false) }
            .apply { if (forUpdate) forUpdate() }
            .firstOrNull()
            .let { it?.toRecord() }
    }

    fun create(article: Article): ArticleId {
        return Articles.insertAndGetId {
            it[title] = article.title ?: ""
            it[content] = article.content ?: ""
            it[password] = article.password.toHashedString()
            it[authorId] = article.authorId.value
            if (article.category?.id != null) {
                it[categoryId] = article.category.id.value
            }
            it[boardId] = requireNotNull(article.boardId).value
            insertAuditInfos(it, article.authorName ?: "")
        }.let {
            ArticleId(it.value)
        }
    }

    fun update(article: Article) {
        Articles.update({ Articles.id eq requireNotNull(article.id).value }) {
            if (article.title != null) {
                it[title] = article.title
            }
            if (article.content != null) {
                it[content] = article.content
            }
            if (article.category?.id != null) {
                it[categoryId] = article.category.id.value
            }
            it[likeCount] = article.likeCount
            it[dislikeCount] = article.dislikeCount
            it[viewCount] = article.viewCount
            updateAuditInfos(it, article.authorName ?: "")
        }
    }

    fun updateViewCount(id: ArticleId) {
        Articles.update({ Articles.id eq id.value }) {
            it[viewCount] = viewCount + 1u
        }
    }

    fun updateLikeCount(id: ArticleId) {
        Articles.update({ Articles.id eq id.value }) {
            it[likeCount] = likeCount + 1u
        }
    }

    fun updateDislikeCount(id: ArticleId) {
        Articles.update({ Articles.id eq id.value }) {
            it[dislikeCount] = dislikeCount + 1u
        }
    }

    fun delete(id: ArticleId) {
        Articles.update({ Articles.id eq id.value }) {
            it[deleted] = true
        }
    }


    private fun selectOperator(boardId: BoardId? = null, authorId: UserId? = null): Op<Boolean> {
        return when {
            boardId != null && authorId != null -> {
                (Articles.boardId eq boardId.value) and (Articles.authorId eq authorId.value) and (Articles.deleted eq false)
            }

            boardId != null -> {
                (Articles.boardId eq boardId.value) and (Articles.deleted eq false)
            }

            authorId != null -> {
                (Articles.authorId eq authorId.value) and (Articles.deleted eq false)
            }

            else -> {
                Articles.deleted eq false
            }
        }
    }

    private fun ResultRow.toRecord() = ArticleRecord(
        boardId = BoardId(this[Articles.boardId].value),
        id = ArticleId(this[Articles.id].value),
        title = this[Articles.title],
        content = this[Articles.content],
        password = this[Articles.password],
        authorId = UserId(this[authorId].value),
        authorName = this[Users.name],
        categoryId = this[categoryId]?.let { CategoryId(it.value) },
        categoryName = this[Categories.name],
        likeCount = this[Articles.likeCount],
        dislikeCount = this[Articles.dislikeCount],
        viewCount = this[Articles.viewCount],
        createdDate = this[Articles.createdDate],
        updatedDate = this[Articles.lastModifiedDate],
    )
}
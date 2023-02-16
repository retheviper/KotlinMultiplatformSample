package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.extension.updateAuditInfos
import com.retheviper.bbs.common.infrastructure.table.Articles
import com.retheviper.bbs.common.infrastructure.table.Articles.authorId
import com.retheviper.bbs.common.infrastructure.table.Users
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class ArticleRepository {

    fun count(authorId: Int?): Long {
        val op = if (authorId != null) {
            (Articles.authorId eq authorId) and (Articles.deleted eq false)
        } else {
            Articles.deleted eq false
        }

        return Articles
            .select(op)
            .count()
    }

    fun findAll(authorId: Int?, page: Int, pageSize: Int, limit: Int): List<ArticleRecord> {
        val op = if (authorId != null) {
            (Articles.authorId eq authorId) and (Articles.deleted eq false)
        } else {
            Articles.deleted eq false
        }

        return Articles
            .leftJoin(Users, { Articles.authorId }, { Users.id })
            .slice(Articles.columns + Users.name)
            .select(op)
            .limit(limit)
            .limit(pageSize, ((page - 1) * pageSize).toLong())
            .orderBy(Articles.id, SortOrder.ASC)
            .map {
                ArticleRecord(
                    id = it[Articles.id].value,
                    title = it[Articles.title],
                    content = it[Articles.content],
                    password = it[Articles.password],
                    authorId = it[Articles.authorId].value,
                    authorName = it[Users.name]
                )
            }
    }

    fun find(id: Int): ArticleRecord? {
        return Articles
            .leftJoin(Users, { authorId }, { Users.id })
            .slice(Articles.columns + Users.name)
            .select { (Articles.id eq id) and (Articles.deleted eq false) }
            .map {
                ArticleRecord(
                    id = it[Articles.id].value,
                    title = it[Articles.title],
                    content = it[Articles.content],
                    password = it[Articles.password],
                    authorId = it[authorId].value,
                    authorName = it[Users.name]
                )
            }.firstOrNull()
    }

    fun create(article: Article) {
        Articles.insert {
            it[title] = article.title
            it[content] = article.content
            it[password] = article.password
            it[authorId] = article.authorId
            insertAuditInfos(it, article.authorName ?: "")
        }
    }

    fun update(article: Article) {
        Articles.update({ Articles.id eq article.id }) {
            it[title] = article.title
            it[content] = article.content
            it[password] = article.password
            updateAuditInfos(it, article.authorName ?: "")
        }
    }

    fun delete(id: Int) {
        Articles.update({ Articles.id eq id }) {
            it[deleted] = true
        }
    }
}
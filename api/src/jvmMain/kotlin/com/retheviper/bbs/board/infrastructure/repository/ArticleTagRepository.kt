package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.infrastructure.model.ArticleTagRecord
import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.infrastructure.table.ArticleTags
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.TagId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

class ArticleTagRepository {

    fun find(articleIds: List<ArticleId>): List<ArticleTagRecord> {
        return ArticleTags.select { (ArticleTags.articleId inList articleIds.map { it.value }) and (ArticleTags.deleted eq false) }
            .map { ArticleTagRecord(
                articleId = ArticleId(it[ArticleTags.articleId].value),
                tagId = TagId(it[ArticleTags.tagId].value))
            }
    }

    fun create(articleId: ArticleId, tagId: TagId, createdBy: String) {
        ArticleTags.insert {
            it[ArticleTags.articleId] = articleId.value
            it[ArticleTags.tagId] = tagId.value
            insertAuditInfos(it, createdBy)
        }
    }

    fun delete(articleId: ArticleId) {
        ArticleTags.deleteWhere { ArticleTags.articleId eq articleId.value }
    }
}
package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.infrastructure.table.ArticleTags
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.TagId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert

class ArticleTagRepository {

    fun create(articleId: ArticleId, tagId: TagId, createdBy: String) {
        ArticleTags.insert {
            it[ArticleTags.articleId] = articleId.value
            it[ArticleTags.tagId] = tagId.value
            insertAuditInfos(it, createdBy)
        }
    }

    fun batchCreate(articleId: ArticleId, tagIds: List<TagId>, createdBy: String) {
        ArticleTags.batchInsert(tagIds) {
            this[ArticleTags.articleId] = articleId.value
            this[ArticleTags.tagId] = it.value
            ArticleTags.insertAuditInfos(this, createdBy)
        }
    }

    fun deleteAll(articleId: ArticleId) {
        ArticleTags.deleteWhere { ArticleTags.articleId eq articleId.value }
    }

    fun delete(ids: List<TagId>) {
        ArticleTags.deleteWhere { tagId inList ids.map { it.value } }
    }

    fun batchDelete(articleId: ArticleId, tagIds: List<TagId>) {
        ArticleTags.deleteWhere { (ArticleTags.articleId eq articleId.value) and (ArticleTags.tagId inList tagIds.map { it.value }) }
    }
}
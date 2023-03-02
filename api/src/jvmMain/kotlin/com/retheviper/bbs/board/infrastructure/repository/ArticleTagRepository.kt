package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.infrastructure.table.ArticleTags
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.TagId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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

    fun delete(articleId: ArticleId) {
        ArticleTags.deleteWhere { ArticleTags.articleId eq articleId.value }
    }
}
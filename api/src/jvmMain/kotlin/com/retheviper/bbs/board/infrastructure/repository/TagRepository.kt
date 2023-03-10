package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.domain.model.Tag
import com.retheviper.bbs.board.infrastructure.model.TagRecord
import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.infrastructure.table.ArticleTags
import com.retheviper.bbs.common.infrastructure.table.Tags
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.TagId
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select

class TagRepository {

    fun findAll(articleIds: List<ArticleId>): List<TagRecord> {
        return ArticleTags.leftJoin(Tags)
            .select { (ArticleTags.articleId inList articleIds.map { it.value }) and (ArticleTags.deleted eq false) }
            .map { it.toRecord() }
    }

    fun find(name: String): TagRecord? {
        return Tags.leftJoin(ArticleTags)
            .select { (Tags.name eq name) and (Tags.deleted eq false) }
            .map { it.toRecord() }
            .firstOrNull()
    }

    fun find(id: TagId): TagRecord? {
        return Tags.leftJoin(ArticleTags)
            .select { (Tags.id eq id.value) and (Tags.deleted eq false) }
            .firstOrNull()
            .let { it?.toRecord() }
    }

    fun create(tag: Tag): TagId {
        val id = Tags.insertAndGetId {
            it[name] = tag.name
            it[description] = tag.description
            insertAuditInfos(it, requireNotNull(tag.createdBy))
        }.value

        return TagId(id)
    }


    private fun ResultRow.toRecord() = TagRecord(
        articleId = ArticleId(this[ArticleTags.articleId].value),
        id = TagId(this[Tags.id].value),
        name = this[Tags.name],
        description = this[Tags.description],
        createdBy = this[Tags.createdBy]
    )
}
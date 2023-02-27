package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Tag
import com.retheviper.bbs.board.infrastructure.repository.ArticleTagRepository
import com.retheviper.bbs.board.infrastructure.repository.TagRepository
import com.retheviper.bbs.common.value.ArticleId
import org.jetbrains.exposed.sql.transactions.transaction

class TagService(private val tagRepository: TagRepository, private val articleTagRepository: ArticleTagRepository) {

    fun findAll(articleIds: List<ArticleId>): List<Tag> {
        return transaction {
            val tags = tagRepository.findAll(articleIds)

            tags.map { Tag.from(it) }
        }
    }

    fun link(articleId: ArticleId, tag: Tag) {
        transaction {
            val exist = tagRepository.find(tag.name)

            if (exist != null) {
                articleTagRepository.create(articleId, exist.id, requireNotNull(tag.createdBy))
            } else {
                val id = tagRepository.create(tag)
                articleTagRepository.create(articleId, id, checkNotNull(tag.createdBy))
            }
        }
    }

    fun unlink(articleId: ArticleId) {
        transaction {
            articleTagRepository.delete(articleId)
        }
    }
}
package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.infrastructure.ArticleRepository
import com.retheviper.bbs.common.exception.BoardNotFoundException
import org.jetbrains.exposed.sql.transactions.transaction

class ArticleService(private val repository: ArticleRepository) {

    fun count(authorId: Int?): Long {
        return transaction {
            repository.count(authorId)
        }
    }

    fun findAll(authorId: Int?, page: Int, pageSize: Int, limit: Int): List<Article> {
        return transaction {
            repository.findAll(
                authorId = authorId,
                page = page,
                pageSize = pageSize,
                limit = limit
            )
        }
    }

    fun find(id: Int): Article {
        return transaction {
            repository.find(id)
        } ?: throw BoardNotFoundException("Board not found with id: $id.")
    }

    fun create(article: Article) {
        transaction {
            repository.create(article)
        }
    }
}
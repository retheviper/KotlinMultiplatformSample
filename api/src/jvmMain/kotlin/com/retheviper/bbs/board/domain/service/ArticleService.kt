package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.infrastructure.ArticleRepository
import com.retheviper.bbs.common.exception.ArticleNotFoundException
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.exception.PasswordNotMatchException
import com.retheviper.bbs.constant.ErrorCode
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
        } ?: throw ArticleNotFoundException("Article not found with id: $id.")
    }

    fun create(article: Article) {
        transaction {
            repository.create(article) // TODO Encrypt
        }
    }

    fun update(article: Article) {
        transaction {
            val id = article.id ?: throw BadRequestException("Article id is null.")
            val exist =
                repository.find(id) ?: throw ArticleNotFoundException("Article not found with id: ${article.id}.")

            if (exist.password != article.password) { // TODO Encrypt
                throw PasswordNotMatchException(
                    "Article not found with id: ${article.id}.",
                    ErrorCode.ARTICLE_PASSWORD_NOT_MATCH
                )
            }

            repository.update(article)
        }
    }

    fun delete(id: Int, password: String) {
        transaction {
            val exist = repository.find(id) ?: throw ArticleNotFoundException("Article not found with id: $id.")

            if (exist.password != password) { // TODO Encrypt
                throw PasswordNotMatchException("Article not found with id: $id", ErrorCode.ARTICLE_PASSWORD_NOT_MATCH)
            }

            repository.delete(id)
        }
    }
}
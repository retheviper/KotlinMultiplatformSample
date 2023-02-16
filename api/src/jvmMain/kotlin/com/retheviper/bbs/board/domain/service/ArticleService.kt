package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.infrastructure.repository.ArticleRepository
import com.retheviper.bbs.common.exception.ArticleNotFoundException
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.exception.PasswordNotMatchException
import com.retheviper.bbs.common.extension.notMatchesWith
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.constant.ErrorCode
import org.jetbrains.exposed.sql.transactions.transaction

class ArticleService(private val commentService: CommentService, private val repository: ArticleRepository) {

    fun count(authorId: Int?): Long {
        return transaction {
            repository.count(authorId)
        }
    }

    fun findAll(authorId: Int?, page: Int, pageSize: Int, limit: Int): List<Article> {
        return transaction {
            val articles = repository.findAll(
                authorId = authorId, page = page, pageSize = pageSize, limit = limit
            )

            val comments = commentService.findAll(articles.map { it.id })
                .groupBy { it.articleId }

            articles.map {
                Article.from(
                    articleRecord = it,
                    comments = comments[it.id]
                )
            }
        }
    }

    @Throws(BadRequestException::class)
    fun find(id: Int): Article {
        return transaction {
            repository.find(id)?.let {
                Article.from(
                    articleRecord = it,
                    comments = commentService.findAll(it.id)
                )
            } ?: throw ArticleNotFoundException("Article not found with id: $id.")
        }
    }

    fun create(article: Article) {
        transaction {
            repository.create(article.copy(password = article.password.toHashedString()))
        }
    }

    @Throws(BadRequestException::class)
    fun update(article: Article) {
        val id = article.id ?: throw BadRequestException("Article id is null.")

        transaction {
            val exist =
                repository.find(id) ?: throw ArticleNotFoundException("Article not found with id: ${article.id}.")

            if (article.password notMatchesWith exist.password) {
                throw PasswordNotMatchException(
                    "Article's password not match with id: ${article.id}.", ErrorCode.ARTICLE_PASSWORD_NOT_MATCH
                )
            }

            repository.update(article)
        }
    }

    @Throws(BadRequestException::class)
    fun delete(id: Int, password: String) {
        transaction {
            val exist = repository.find(id) ?: throw ArticleNotFoundException("Article not found with id: $id.")

            if (password notMatchesWith exist.password) {
                throw PasswordNotMatchException(
                    "Article's password not match with id: ${id}.", ErrorCode.ARTICLE_PASSWORD_NOT_MATCH
                )
            }

            repository.delete(id)
        }
    }
}
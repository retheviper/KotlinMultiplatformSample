package com.retheviper.bbs.board.domain.usecase

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.common.domain.usecase.HasSensitiveWordService
import com.retheviper.bbs.board.domain.service.ArticleService
import com.retheviper.bbs.common.exception.ArticleAuthorNotMatchException
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.exception.PasswordNotMatchException
import com.retheviper.bbs.common.extension.notMatchesWith
import com.retheviper.bbs.common.domain.service.SensitiveWordService
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.constant.ErrorCode
import com.retheviper.bbs.model.common.PaginationProperties

class ArticleUseCase(
    private val articleService: ArticleService,
    override val sensitiveWordService: SensitiveWordService
) : HasSensitiveWordService {

    fun findBy(boardId: BoardId, authorId: UserId?, paginationProperties: PaginationProperties): List<Article> {
        return articleService.findBy(boardId, authorId, paginationProperties)
    }

    @Throws(BadRequestException::class)
    fun find(id: ArticleId, userId: UserId?, subPaginationProperties: PaginationProperties): Article {
        val article = articleService.find(id, subPaginationProperties)

        return if (article.authorId != userId) {
            articleService.updateViewCount(id)
            article.copy(viewCount = article.viewCount + 1u)
        } else {
            article
        }
    }

    @Throws(BadRequestException::class)
    fun create(article: Article): Article {
        article.title ?: throw BadRequestException("Article title is null.")
        checkSensitiveWords(article.title)

        article.content ?: throw BadRequestException("Article content is null.")
        checkSensitiveWords(article.content)

        if (article.tags.isNotEmpty()) {
            checkSensitiveWords(article.tags.map { it.name })
        }

        return articleService.create(article)
    }

    @Throws(BadRequestException::class)
    fun update(article: Article): Article {
        val id = article.id ?: throw BadRequestException("Article id is null.")

        val exist = articleService.findForUpdate(id)

        if (article.authorId != exist.authorId) {
            throw ArticleAuthorNotMatchException("Article's author id is not match with id: ${article.id}.")
        }

        if (article.password notMatchesWith exist.password) {
            throw PasswordNotMatchException(
                "Article's password not match with id: ${article.id}.", ErrorCode.ARTICLE_PASSWORD_NOT_MATCH
            )
        }

        article.title?.let {
            checkSensitiveWords(it)
        }

        article.content?.let {
            checkSensitiveWords(it)
        }

        if (article.tags.isNotEmpty()) {
            checkSensitiveWords(article.tags.map { it.name })
        }

        return articleService.update(article)
    }

    @Throws(BadRequestException::class)
    fun delete(id: ArticleId, password: String) {
        val exist = articleService.find(id)

        if (password notMatchesWith exist.password) {
            throw PasswordNotMatchException(
                "Article's password not match with id: ${id}.", ErrorCode.ARTICLE_PASSWORD_NOT_MATCH
            )
        }

        articleService.delete(id)
    }
}
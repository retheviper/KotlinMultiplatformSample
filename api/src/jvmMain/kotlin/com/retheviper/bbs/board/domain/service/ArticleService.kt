package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.model.Category
import com.retheviper.bbs.board.infrastructure.repository.ArticleRepository
import com.retheviper.bbs.common.exception.ArticleAuthorNotMatchException
import com.retheviper.bbs.common.exception.ArticleNotFoundException
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.exception.PasswordNotMatchException
import com.retheviper.bbs.common.extension.notMatchesWith
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.constant.ErrorCode
import com.retheviper.bbs.model.common.PaginationProperties
import org.jetbrains.exposed.sql.transactions.transaction

class ArticleService(
    private val sensitiveWordService: SensitiveWordService,
    private val categoryService: CategoryService,
    private val tagService: TagService,
    private val commentService: CommentService,
    private val repository: ArticleRepository
) {

    fun count(authorId: UserId?): Long {
        return transaction {
            repository.count(authorId)
        }
    }

    fun findAll(boardId: BoardId, authorId: UserId?, paginationProperties: PaginationProperties): List<Article> {
        return transaction {
            val articles = repository.findAll(
                boardId = boardId,
                authorId = authorId,
                paginationProperties = paginationProperties
            )

            val tags = tagService.findAll(articles.map { it.id })
                .groupBy { it.articleId }

            val comments = commentService.findAll(articles.map { it.id })
                .groupBy { it.articleId }

            articles.map {
                Article.from(
                    articleRecord = it,
                    category = Category(
                        id = it.categoryId,
                        name = it.categoryName
                    ),
                    tags = tags[it.id] ?: emptyList(),
                    comments = comments[it.id]
                )
            }
        }
    }

    @Throws(BadRequestException::class)
    fun find(id: ArticleId, username: String? = null): Article {
        return transaction {
            var article = repository.find(id = id, forUpdate = true)?.let {
                Article.from(
                    articleRecord = it,
                    category = Category(
                        id = it.categoryId,
                        name = it.categoryName
                    ),
                    tags = tagService.findAll(listOf(it.id)),
                    comments = commentService.findAll(it.id)
                )
            } ?: throw ArticleNotFoundException("Article not found with id: $id.")

            if (article.authorName != username) {
                article = article.updateViewCount()
                repository.update(article)
            }

            article
        }
    }

    fun create(article: Article): ArticleId {
        var newArticle = article.checkSensitiveWord().trim()
        return transaction {
            if (article.category != null) {
                val category = categoryService.find(article.category.name)
                newArticle = article.copy(category = category)
            }

            newArticle = newArticle.copy(
                password = article.password.toHashedString(),
                authorName = repository.findAuthorName(article.authorId)
            )

            val articleId = repository.create(newArticle)

            article.tags.forEach {
                tagService.link(articleId, it.copy(createdBy = newArticle.authorName))
            }

            articleId
        }
    }

    @Throws(BadRequestException::class)
    fun update(article: Article) {
        val id = article.id ?: throw BadRequestException("Article id is null.")
        var updatedArticle = article.checkSensitiveWord().trim()

        transaction {
            val exist =
                repository.find(id = id, forUpdate = true) ?: throw ArticleNotFoundException("Article not found with id: ${article.id}.")

            if (article.authorId != exist.authorId) {
                throw ArticleAuthorNotMatchException("Article's author id is not match with id: ${article.id}.")
            }

            if (article.password notMatchesWith exist.password) {
                throw PasswordNotMatchException(
                    "Article's password not match with id: ${article.id}.", ErrorCode.ARTICLE_PASSWORD_NOT_MATCH
                )
            }

            updatedArticle = updatedArticle.copy(
                authorName = exist.authorName,
                password = article.password.toHashedString()
            )

            if (article.category != null) {
                val category = categoryService.find(article.category.name)
                updatedArticle = article.copy(category = category)
            }

            if (article.tags.isNotEmpty()) {
                article.tags.forEach {
                    tagService.link(id, it.copy(createdBy = updatedArticle.authorName))
                }
            }

            repository.update(updatedArticle)
        }
    }

    @Throws(BadRequestException::class)
    fun delete(id: ArticleId, password: String) {
        transaction {
            val exist = repository.find(id) ?: throw ArticleNotFoundException("Article not found with id: $id.")

            if (password notMatchesWith exist.password) {
                throw PasswordNotMatchException(
                    "Article's password not match with id: ${id}.", ErrorCode.ARTICLE_PASSWORD_NOT_MATCH
                )
            }

            tagService.unlink(id)
            repository.delete(id)
        }
    }

    @Throws(BadRequestException::class)
    private fun Article.checkSensitiveWord(): Article {
        val title = sensitiveWordService.find(title)

        if (title.isNotEmpty()) {
            throw BadRequestException("Article's title contains sensitive word: ${title.joinToString()}.")
        }

        val content  = sensitiveWordService.find(content)

        if (content.isNotEmpty()) {
            throw BadRequestException("Article's content contains sensitive word: ${content.joinToString()}.")
        }

        return this
    }

    private fun Article.trim(): Article {
        return this.copy(
            title = this.title.trim(),
            content = this.content.trim(),
            password = this.password.trim()
        )
    }
}
package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.board.infrastructure.repository.ArticleRepository
import com.retheviper.bbs.board.infrastructure.repository.ArticleTagRepository
import com.retheviper.bbs.board.infrastructure.repository.CategoryRepository
import com.retheviper.bbs.board.infrastructure.repository.CommentRepository
import com.retheviper.bbs.board.infrastructure.repository.TagRepository
import com.retheviper.bbs.common.exception.ArticleAlreadyExistsException
import com.retheviper.bbs.common.exception.ArticleNotFoundException
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.model.common.PaginationProperties

class ArticleService(
    private val articleRepository: ArticleRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val articleTagRepository: ArticleTagRepository,
    private val commentRepository: CommentRepository
) {
    fun findBy(boardId: BoardId, authorId: UserId?, paginationProperties: PaginationProperties): List<Article> {
        val articles = articleRepository.findBy(
            boardId = boardId,
            authorId = authorId,
            paginationProperties = paginationProperties
        )

        val articleIds = articles.map { it.id }

        val tags = tagRepository.findBy(articleIds)
            .groupBy { it.articleId }

        val comments = commentRepository.findBy(articleIds)
            .groupBy { it.articleId }

        return articles.map {
            Article.from(
                article = it,
                tags = tags[it.id] ?: emptyList(),
                comments = comments[it.id] ?: emptyList()
            )
        }
    }

    @Throws(ArticleNotFoundException::class)
    fun find(id: ArticleId, subPaginationProperties: PaginationProperties? = null): Article {
        val article = articleRepository.find(id = id, forUpdate = false)
            ?: throw ArticleNotFoundException("Article not found with id: $id.")

        val tags = tagRepository.findBy(article.id)

        val comments = commentRepository.findBy(article.id, subPaginationProperties)

        return Article.from(
            article = article,
            tags = tags,
            comments = comments
        )
    }

    @Throws(ArticleNotFoundException::class)
    fun findForUpdate(id: ArticleId): ArticleRecord {
        return articleRepository.find(id = id, forUpdate = true)
            ?: throw ArticleNotFoundException("Article not found with id: $id.")
    }

    @Throws(ArticleAlreadyExistsException::class)
    fun create(article: Article): Article {
        var newArticle = article

        checkIsValidCategory(article)

        val articleId = articleRepository.create(article)
        newArticle = newArticle.copy(id = articleId)

        if (newArticle.tags.isNotEmpty()) {
            linkTags(newArticle)
        }

        return newArticle
    }

    @Throws(BadRequestException::class)
    fun update(article: Article): Article {
        article.id ?: throw BadRequestException("Article id is null.")

        checkIsValidCategory(article)

        if (article.tags.isNotEmpty()) {
            linkTags(article)
        }

        articleRepository.update(article)
        return article
    }

    fun updateViewCount(id: ArticleId) {
        articleRepository.updateViewCount(id)
    }

    @Throws(BadRequestException::class)
    fun delete(id: ArticleId) {
        articleTagRepository.deleteAll(id)
        articleRepository.delete(id)
    }

    private fun checkIsValidCategory(article: Article) {
        if (article.category?.id == null) {
            return
        }

        val category = categoryRepository.find(article.category.id)
            ?: throw BadRequestException("Category not found with id: ${article.category.id}.")

        if (category.boardId != null && category.boardId != article.boardId) {
            throw BadRequestException("Category id: ${article.category.id} is not available for board id: ${article.boardId}.")
        }
    }

    private fun linkTags(article: Article) {
        article.id ?: throw BadRequestException("Article id is null.")

        val existingTags = tagRepository.findBy(article.tags.map { it.name })
        val newTags = article.tags.filter { tag -> existingTags.none { it.name == tag.name } }

        if (newTags.isNotEmpty()) {
            val createdTags = tagRepository.batchCreate(newTags)
            articleTagRepository.batchCreate(article.id, createdTags.map { it.id }, createdTags.first().createdBy)
        }

        val removedTags = existingTags.filter { tag -> article.tags.none { it.name == tag.name } }
        if (removedTags.isNotEmpty()) {
            articleTagRepository.batchDelete(article.id, removedTags.map { it.id })
        }
    }
}
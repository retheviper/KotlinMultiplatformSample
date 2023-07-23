package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.infrastructure.model.request.CreateArticleRequest

class ArticleRepository {

    private val endpoint = "/articles"

    fun getArticle() {
    }

    fun getArticles() {
    }

    fun createArticle() {

        CreateArticleRequest(
            title = "title",
            content = "content",
            password = "password",
            categoryName = "categoryName",
            tagNames = listOf("tagNames")
        )


    }

    fun updateArticle() {
    }
}
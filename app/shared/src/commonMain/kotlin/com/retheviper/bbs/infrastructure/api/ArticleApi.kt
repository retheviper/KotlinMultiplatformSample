package com.retheviper.bbs.infrastructure.api

import com.retheviper.bbs.infrastructure.client.getHttpClient
import com.retheviper.bbs.model.common.PaginationProperties
import com.retheviper.bbs.model.response.ArticleResponse
import com.retheviper.bbs.model.response.ListArticleResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

object ArticleApi {

    private val endpoint = "${UserApi.basePath}/board/{boardId}/article"

    suspend fun getArticle(boardId: Int, articleId: Int): ArticleResponse {
        return getHttpClient().use {
            it.get("${endpoint.replace("{boardId}", boardId.toString())}/$articleId")
        }.body()
    }

    suspend fun getArticles(boardId: Int, paginationProperties: PaginationProperties): ListArticleResponse {
        return getHttpClient().use {
            it.get(endpoint.replace("{boardId}", boardId.toString())) {
                parameter("page", paginationProperties.page)
                parameter("size", paginationProperties.size)
                parameter("limit", paginationProperties.limit)
            }
        }.body()
    }
}

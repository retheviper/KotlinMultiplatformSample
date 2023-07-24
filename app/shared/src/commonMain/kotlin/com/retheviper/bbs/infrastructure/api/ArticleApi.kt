package com.retheviper.bbs.infrastructure.api

import com.retheviper.bbs.infrastructure.client.getHttpClient
import com.retheviper.bbs.infrastructure.model.common.PaginationProperties
import com.retheviper.bbs.infrastructure.model.response.ArticleResponse
import com.retheviper.bbs.infrastructure.model.response.ListArticleResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.parameters
import io.ktor.utils.io.core.use

object ArticleApi {

    private val endpoint = "${UserApi.basePath}/board/{boardId}/article"

    suspend fun getArticle(boardId: Int, articleId: Int): ArticleResponse {
        return getHttpClient().use {
            it.get("${endpoint.replace("{boardId}", boardId.toString())}/$articleId")
        }.body()
    }

    suspend fun getArticles(boardId: Int, paginationProperties: PaginationProperties): ListArticleResponse {
        return getHttpClient().use {
            parameters {
                append("page", paginationProperties.page.toString())
                append("size", paginationProperties.size.toString())
                append("limit", paginationProperties.limit.toString())
            }
            it.get(endpoint.replace("{boardId}", boardId.toString()))
        }.body()
    }
}
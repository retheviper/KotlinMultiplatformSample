package com.retheviper.chat.messaging.application

import com.retheviper.chat.messaging.domain.LinkPreview
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import java.net.URI
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

data class LinkPreviewImagePayload(
    val bytes: ByteArray,
    val contentType: String
)

class LinkPreviewResolver(
    private val client: HttpClient = HttpClient {
        install(HttpRedirect)
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {
    suspend fun resolve(url: String): LinkPreview? {
        val normalizedUrl = url.trim()
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            return null
        }

        val uri = runCatching { URI(normalizedUrl) }.getOrNull()
        val host = uri?.host
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }

        val response = runCatching {
            client.get(normalizedUrl) {
                header(HttpHeaders.UserAgent, "ChatPreviewBot/1.0")
            }
        }.getOrNull()

        val contentType = response?.headers?.get(HttpHeaders.ContentType)
        if (response != null && response.status.isSuccess() && contentType?.startsWith("image/") == true) {
            return LinkPreview(
                url = normalizedUrl,
                title = imageTitle(uri),
                imageUrl = normalizedUrl,
                siteName = host
            )
        }

        val html = runCatching { response?.body<String>() }.getOrNull()

        if (html == null) {
            return host?.let {
                LinkPreview(
                    url = normalizedUrl,
                    title = it,
                    siteName = it
                )
            }
        }

        val title = findMetaContent(html, "property", "og:title")
            ?: findMetaContent(html, "name", "twitter:title")
            ?: findTitle(html)
        val description = findMetaContent(html, "property", "og:description")
            ?: findMetaContent(html, "name", "description")
            ?: findMetaContent(html, "name", "twitter:description")
        val imageUrl = findMetaContent(html, "property", "og:image")
            ?: findMetaContent(html, "name", "twitter:image")
        val siteName = findMetaContent(html, "property", "og:site_name")

        return LinkPreview(
            url = normalizedUrl,
            title = title ?: host,
            description = description,
            imageUrl = imageUrl?.let { resolveUrl(normalizedUrl, it) },
            siteName = siteName ?: host
        )
    }

    suspend fun fetchImage(url: String): LinkPreviewImagePayload? {
        val normalizedUrl = url.trim()
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            return null
        }

        val response = runCatching {
            client.get(normalizedUrl) {
                header(HttpHeaders.UserAgent, "ChatPreviewBot/1.0")
            }
        }.getOrNull() ?: return null

        return response.toImagePayload()
    }

    private fun findMetaContent(html: String, attribute: String, value: String): String? {
        val regex = Regex(
            """<meta[^>]*$attribute\s*=\s*["']${Regex.escape(value)}["'][^>]*content\s*=\s*["']([^"']+)["'][^>]*>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val reverseRegex = Regex(
            """<meta[^>]*content\s*=\s*["']([^"']+)["'][^>]*$attribute\s*=\s*["']${Regex.escape(value)}["'][^>]*>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return (regex.find(html)?.groupValues?.getOrNull(1)
            ?: reverseRegex.find(html)?.groupValues?.getOrNull(1))
            ?.decodeHtml()
            ?.takeIf { it.isNotBlank() }
    }

    private fun findTitle(html: String): String? {
        val titleMatch = Regex(
            """<title[^>]*>(.*?)</title>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html)?.groupValues?.getOrNull(1)
        return titleMatch?.decodeHtml()?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun String.decodeHtml(): String {
        return replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .trim()
    }

    private fun resolveUrl(pageUrl: String, targetUrl: String): String {
        return runCatching {
            URI(pageUrl).resolve(targetUrl).toString()
        }.getOrDefault(targetUrl)
    }

    private suspend fun HttpResponse.toImagePayload(): LinkPreviewImagePayload? {
        if (!status.isSuccess()) {
            return null
        }
        val contentType = headers[HttpHeaders.ContentType]
            ?.takeIf { it.startsWith("image/") }
            ?: return null
        return LinkPreviewImagePayload(
            bytes = body(),
            contentType = contentType
        )
    }

    private fun imageTitle(uri: URI?): String {
        val path = uri?.path.orEmpty()
        val fileName = path.substringAfterLast('/').substringBefore('?').substringBefore('#')
        return fileName.takeIf { it.isNotBlank() } ?: "Image"
    }
}

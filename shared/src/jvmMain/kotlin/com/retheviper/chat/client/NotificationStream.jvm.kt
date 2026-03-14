package com.retheviper.chat.client

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private class JvmNotificationStreamHandle(
    private val scope: CoroutineScope,
    private val disconnect: () -> Unit
) : NotificationStreamHandle {
    override fun close() {
        disconnect()
        scope.cancel()
    }
}

actual object NotificationStreamClient {
    actual fun connect(
        baseUrl: String,
        memberId: String,
        onNotificationSignal: () -> Unit,
        onFailure: (Throwable) -> Unit
    ): NotificationStreamHandle? {
        val normalizedBaseUrl = baseUrl.removeSuffix("/")
        if (normalizedBaseUrl.isBlank()) return null

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var connection: HttpURLConnection? = null
        scope.launch {
            try {
                connection = URL("$normalizedBaseUrl/api/v1/members/$memberId/notifications/stream")
                    .openConnection() as HttpURLConnection
                connection!!.requestMethod = "GET"
                connection!!.setRequestProperty("Accept", "text/event-stream")
                connection!!.connectTimeout = 10_000
                connection!!.readTimeout = 0
                connection!!.connect()

                val statusCode = connection!!.responseCode
                if (statusCode !in 200..299) {
                    throw IllegalStateException("Notification stream request failed with status $statusCode")
                }

                var initialized = false
                connection!!.inputStream.bufferedReader().use { reader ->
                    while (scope.isActive) {
                        val line = reader.readLine() ?: break
                        if (!line.startsWith("data:")) continue
                        val payload = line.removePrefix("data:").trim()
                        if (!initialized && payload == "connected") {
                            initialized = true
                            continue
                        }
                        initialized = true
                        onNotificationSignal()
                    }
                }
            } catch (_: CancellationException) {
                // Ignore cancellation while disposing the stream.
            } catch (throwable: Throwable) {
                if (scope.isActive) {
                    onFailure(throwable)
                }
            } finally {
                connection?.disconnect()
                connection = null
            }
        }
        return JvmNotificationStreamHandle(scope) {
            connection?.disconnect()
        }
    }
}

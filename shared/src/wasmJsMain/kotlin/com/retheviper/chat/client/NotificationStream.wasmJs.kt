package com.retheviper.chat.client

import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event

private external class EventSource(url: String) {
    fun close()
    var onmessage: ((MessageEvent) -> Unit)?
    var onerror: ((Event) -> Unit)?
}

private class WasmNotificationStreamHandle(
    private val source: EventSource
) : NotificationStreamHandle {
    override fun close() {
        source.close()
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
        val source = EventSource("$normalizedBaseUrl/api/v1/members/$memberId/notifications/stream")
        var initialized = false
        source.onmessage = {
            if (initialized) {
                onNotificationSignal()
            } else {
                initialized = true
            }
        }
        source.onerror = { event ->
            onFailure(IllegalStateException("Notification stream error: $event"))
        }
        return WasmNotificationStreamHandle(source)
    }
}

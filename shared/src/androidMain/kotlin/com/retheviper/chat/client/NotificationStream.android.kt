package com.retheviper.chat.client

actual object NotificationStreamClient {
    actual fun connect(
        baseUrl: String,
        memberId: String,
        onNotificationSignal: () -> Unit,
        onFailure: (Throwable) -> Unit
    ): NotificationStreamHandle? = null
}

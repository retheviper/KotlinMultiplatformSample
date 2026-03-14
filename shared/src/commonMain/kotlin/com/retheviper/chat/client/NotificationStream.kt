package com.retheviper.chat.client

interface NotificationStreamHandle {
    fun close()
}

expect object NotificationStreamClient {
    fun connect(
        baseUrl: String,
        memberId: String,
        onNotificationSignal: () -> Unit,
        onFailure: (Throwable) -> Unit = {}
    ): NotificationStreamHandle?
}

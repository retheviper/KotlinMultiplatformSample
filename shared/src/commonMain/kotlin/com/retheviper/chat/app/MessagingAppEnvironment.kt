package com.retheviper.chat.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.retheviper.chat.client.NotificationStreamClient
import com.retheviper.chat.client.NotificationStreamHandle
import com.retheviper.chat.client.PlatformClientConfig
import io.ktor.http.encodeURLParameter

fun interface NotificationStreamConnector {
    fun connect(
        baseUrl: String,
        memberId: String,
        onNotificationSignal: () -> Unit,
        onFailure: (Throwable) -> Unit
    ): NotificationStreamHandle?
}

data class MessagingAppEnvironment(
    val baseUrl: String,
    val platformName: String,
    val notificationStreamConnector: NotificationStreamConnector
) {
    fun previewImageUrl(imageUrl: String): String {
        return "$baseUrl/api/v1/link-preview/image?url=${imageUrl.encodeURLParameter()}"
    }
}

@Composable
fun rememberMessagingAppEnvironment(): MessagingAppEnvironment {
    return remember {
        MessagingAppEnvironment(
            baseUrl = PlatformClientConfig.baseUrl,
            platformName = PlatformClientConfig.platformName,
            notificationStreamConnector = NotificationStreamConnector { baseUrl, memberId, onNotificationSignal, onFailure ->
                NotificationStreamClient.connect(
                    baseUrl = baseUrl,
                    memberId = memberId,
                    onNotificationSignal = onNotificationSignal,
                    onFailure = onFailure
                )
            }
        )
    }
}

package com.retheviper.chat.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.retheviper.chat.app.AppNotificationEvent

internal interface AndroidNotificationPublisher {
    fun showNotification(event: AppNotificationEvent)
}

internal class AndroidAppBridge(
    private val context: Context
) : AndroidNotificationPublisher {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        ensureNotificationChannel()
    }

    override fun showNotification(event: AppNotificationEvent) {
        if (!canPostNotifications()) return

        notificationManager.notify(
            event.id.hashCode(),
            NotificationCompat.Builder(context, CHAT_UPDATES_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(event.title)
                .setContentText(event.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(event.body))
                .setAutoCancel(true)
                .setContentIntent(createContentIntent())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        )
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHAT_UPDATES_CHANNEL_ID,
            "Chat updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "New mentions and thread updates"
        }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHAT_UPDATES_CHANNEL_ID = "chat_updates"
    }
}

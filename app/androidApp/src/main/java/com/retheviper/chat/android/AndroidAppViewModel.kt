package com.retheviper.chat.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.retheviper.chat.app.AppNotificationEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class AndroidAppUiState(
    val windowTitle: String = ANDROID_APP_DEFAULT_WINDOW_TITLE
)

internal fun interface NotificationPermissionDecider {
    fun shouldRequestPermission(): Boolean
}

internal class AndroidNotificationPermissionDecider(
    private val context: Context
) : NotificationPermissionDecider {
    override fun shouldRequestPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
    }
}

internal class AndroidAppViewModel(
    private val notificationPublisher: AndroidNotificationPublisher,
    private val notificationPermissionDecider: NotificationPermissionDecider
) : ViewModel() {
    private val _uiState = MutableStateFlow(AndroidAppUiState())
    val uiState: StateFlow<AndroidAppUiState> = _uiState.asStateFlow()

    fun onWindowTitleChanged(windowTitle: String) {
        _uiState.update { current ->
            if (current.windowTitle == windowTitle) current else current.copy(windowTitle = windowTitle)
        }
    }

    fun onNotificationEvent(event: AppNotificationEvent) {
        notificationPublisher.showNotification(event)
    }

    fun shouldRequestNotificationPermission(): Boolean = notificationPermissionDecider.shouldRequestPermission()

    internal class Factory(
        private val notificationPublisher: AndroidNotificationPublisher,
        private val notificationPermissionDecider: NotificationPermissionDecider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AndroidAppViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return AndroidAppViewModel(
                notificationPublisher = notificationPublisher,
                notificationPermissionDecider = notificationPermissionDecider
            ) as T
        }
    }

}

private const val ANDROID_APP_DEFAULT_WINDOW_TITLE = "Chat Android"

package com.retheviper.chat.android

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelProvider
import com.retheviper.chat.app.MessagingApp

class MainActivity : ComponentActivity() {
    private val appBridge by lazy(LazyThreadSafetyMode.NONE) {
        AndroidAppBridge(applicationContext)
    }

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(
            this,
            AndroidAppViewModel.Factory(
                notificationPublisher = appBridge,
                notificationPermissionDecider = AndroidNotificationPermissionDecider(applicationContext)
            )
        )[AndroidAppViewModel::class.java]
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.windowTitle) {
                title = uiState.windowTitle
            }

            MessagingApp(
                onNotificationEvent = viewModel::onNotificationEvent,
                onWindowTitleChange = viewModel::onWindowTitleChanged
            )
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (viewModel.shouldRequestNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

package com.retheviper.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import com.retheviper.chat.app.MessagingApp
import java.awt.AWTException
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics2D
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.Taskbar
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.util.prefs.Preferences
import javax.swing.SwingUtilities
import kotlinx.coroutines.flow.distinctUntilChanged

internal object ComposeDesktopShellRunner : DesktopShellRunner {
    override fun run() = application {
        ComposeDesktopShellApp(onExitApplication = ::exitApplication)
    }
}

@Composable
internal fun ComposeDesktopShellApp(onExitApplication: () -> Unit) {
    val preferences = remember { Preferences.userRoot().node(PreferencesNode) }
    val windowState = rememberDesktopWindowState(preferences)
    val desktopBridge = remember { DesktopBridge() }

    var windowTitle by rememberSaveable { mutableStateOf("Chat Desktop") }
    var unreadNotificationCount by rememberSaveable { mutableStateOf(0) }
    var quitRequested by remember { mutableStateOf(false) }
    var awtWindow by remember { mutableStateOf<java.awt.Window?>(null) }

    fun requestQuit() {
        persistWindowState(preferences, windowState)
        desktopBridge.dispose()
        quitRequested = true
    }

    if (quitRequested) {
        onExitApplication()
        return
    }

    LaunchedEffect(windowState) {
        snapshotFlow {
            DesktopWindowSnapshot(
                width = windowState.size.width.value,
                height = windowState.size.height.value,
                x = (windowState.position as? androidx.compose.ui.window.WindowPosition.Absolute)?.x?.value,
                y = (windowState.position as? androidx.compose.ui.window.WindowPosition.Absolute)?.y?.value,
                maximized = windowState.placement == WindowPlacement.Maximized
            )
        }.distinctUntilChanged().collect {
            persistWindowState(preferences, windowState)
        }
    }

    LaunchedEffect(unreadNotificationCount, awtWindow, windowTitle) {
        desktopBridge.updateBadge(windowTitle, unreadNotificationCount)
        desktopBridge.ensureTray(
            title = windowTitle,
            unreadCount = unreadNotificationCount,
            onOpen = {
                SwingUtilities.invokeLater {
                    awtWindow?.isVisible = true
                    awtWindow?.toFront()
                    awtWindow?.requestFocus()
                }
            },
            onHide = {
                SwingUtilities.invokeLater { awtWindow?.isVisible = false }
            },
            onQuit = {
                SwingUtilities.invokeLater { requestQuit() }
            }
        )
    }

    Window(onCloseRequest = { requestQuit() }, title = windowTitle, state = windowState) {
        awtWindow = window
        window.minimumSize = Dimension(1180, 760)

        LaunchedEffect(Unit) {
            if (!window.isVisible) window.isVisible = true
            window.toFront()
            window.requestFocus()
        }

        MenuBar {
            Menu("App") {
                Item(
                    text = "Hide Window",
                    enabled = desktopBridge.isTraySupported,
                    shortcut = menuShortcut(Key.M),
                    onClick = { window.isVisible = false }
                )
                Item(
                    text = "Show Window",
                    shortcut = menuShortcut(Key.O),
                    onClick = {
                        window.isVisible = true
                        window.toFront()
                        window.requestFocus()
                    }
                )
                Item(text = "Quit", shortcut = menuShortcut(Key.Q), onClick = { requestQuit() })
            }
        }

        DisposableEffect(Unit) {
            onDispose { persistWindowState(preferences, windowState) }
        }

        MessagingApp(
            onUnreadNotificationCountChange = { unreadNotificationCount = it },
            onNotificationEvent = { event ->
                desktopBridge.showNotification(event = event, window = awtWindow, unreadCount = unreadNotificationCount)
            },
            onWindowTitleChange = { title -> windowTitle = title }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            persistWindowState(preferences, windowState)
            desktopBridge.dispose()
        }
    }
}

internal class DesktopBridge {
    private var trayIcon: TrayIcon? = null

    val isTraySupported: Boolean = SystemTray.isSupported()
    private val isTaskbarSupported: Boolean = Taskbar.isTaskbarSupported()

    fun ensureTray(
        title: String,
        unreadCount: Int,
        onOpen: () -> Unit,
        onHide: () -> Unit,
        onQuit: () -> Unit
    ) {
        if (!isTraySupported) return

        val existing = trayIcon
        if (existing != null) {
            existing.image = createAppIcon(unreadCount)
            existing.toolTip = tooltip(title, unreadCount)
            return
        }

        val popup = PopupMenu().apply {
            add(java.awt.MenuItem("Open").apply { addActionListener { onOpen() } })
            add(java.awt.MenuItem("Hide").apply { addActionListener { onHide() } })
            addSeparator()
            add(java.awt.MenuItem("Quit").apply { addActionListener { onQuit() } })
        }

        val icon = TrayIcon(createAppIcon(unreadCount), tooltip(title, unreadCount), popup).apply {
            isImageAutoSize = true
            addActionListener { onOpen() }
        }

        try {
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
        } catch (_: AWTException) {
        }
    }

    fun updateBadge(title: String, unreadCount: Int) {
        trayIcon?.image = createAppIcon(unreadCount)
        trayIcon?.toolTip = tooltip(title, unreadCount)
        if (!isTaskbarSupported) return
        runCatching {
            Taskbar.getTaskbar().setIconBadge(unreadCount.takeIf { it > 0 }?.coerceAtMost(99)?.toString().orEmpty())
        }
    }

    fun showNotification(event: com.retheviper.chat.app.AppNotificationEvent, window: java.awt.Window?, unreadCount: Int) {
        updateBadge("Chat Desktop", unreadCount)
        trayIcon?.displayMessage(event.title, event.body, TrayIcon.MessageType.INFO)
        if (isTaskbarSupported) {
            runCatching {
                val taskbar = Taskbar.getTaskbar()
                window?.let { taskbar.requestWindowUserAttention(it) } ?: taskbar.requestUserAttention(true, true)
            }
        }
    }

    fun dispose() {
        trayIcon?.let { runCatching { SystemTray.getSystemTray().remove(it) } }
        trayIcon = null
        if (isTaskbarSupported) runCatching { Taskbar.getTaskbar().setIconBadge("") }
    }

    private fun tooltip(title: String, unreadCount: Int): String {
        return if (unreadCount > 0) "$title ($unreadCount unread)" else title
    }
}

private fun createAppIcon(unreadCount: Int): BufferedImage {
    val size = 64
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    graphics.color = Color(60, 33, 75)
    graphics.fillRoundRect(0, 0, size, size, 18, 18)

    graphics.color = Color(246, 244, 250)
    graphics.font = Font("SansSerif", Font.BOLD, 34)
    val label = "C"
    val metrics = graphics.fontMetrics
    val textX = (size - metrics.stringWidth(label)) / 2
    val textY = ((size - metrics.height) / 2) + metrics.ascent
    graphics.drawString(label, textX, textY)

    if (unreadCount > 0) {
        val badgeText = unreadCount.coerceAtMost(99).toString()
        graphics.color = Color(124, 92, 255)
        graphics.fillOval(size - 28, 4, 24, 24)
        graphics.color = Color.WHITE
        graphics.font = Font("SansSerif", Font.BOLD, if (badgeText.length > 1) 11 else 13)
        val badgeMetrics = graphics.fontMetrics
        val badgeX = size - 16 - (badgeMetrics.stringWidth(badgeText) / 2)
        val badgeY = 20
        graphics.drawString(badgeText, badgeX, badgeY)
    }

    graphics.dispose()
    return image
}

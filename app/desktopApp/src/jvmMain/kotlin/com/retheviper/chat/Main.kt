package com.retheviper.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.retheviper.chat.app.AppNotificationEvent
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
import java.io.File
import java.nio.file.Path
import java.util.prefs.Preferences
import javax.swing.SwingUtilities
import kotlinx.coroutines.flow.distinctUntilChanged

private const val PreferencesNode = "com.retheviper.chat.desktop"
private const val PrefWindowWidth = "window.width"
private const val PrefWindowHeight = "window.height"
private const val PrefWindowX = "window.x"
private const val PrefWindowY = "window.y"
private const val PrefWindowMaximized = "window.maximized"

private enum class DesktopShellMode {
    AUTO,
    COMPOSE,
    MAC_NATIVE,
    CHOOSER
}

private interface DesktopShellRunner {
    fun run()
}

fun main() {
    val shellMode = selectedShellMode()
    val runner = when (resolveShellMode(shellMode)) {
        DesktopShellMode.COMPOSE -> ComposeDesktopShellRunner
        DesktopShellMode.MAC_NATIVE -> MacNativeDesktopShellRunner
        DesktopShellMode.CHOOSER -> ChooserDesktopShellRunner
        DesktopShellMode.AUTO -> ComposeDesktopShellRunner
    }
    runner.run()
}

private object ChooserDesktopShellRunner : DesktopShellRunner {
    override fun run() = application {
        var selectedMode by remember { mutableStateOf<DesktopShellMode?>(null) }

        when (selectedMode) {
            null -> DialogWindow(
                onCloseRequest = ::exitApplication,
                title = "Choose Shell",
                resizable = false
            ) {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.background(ComposeColor(0xFF17131F)),
                        color = ComposeColor(0xFF17131F)
                    ) {
                        Column(
                            modifier = Modifier
                                .width(560.dp)
                                .padding(28.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = "Choose desktop shell",
                                style = MaterialTheme.typography.h5,
                                color = ComposeColor(0xFFF6F2FF)
                            )
                            Text(
                                text = "Pick the shell to launch for this session.",
                                style = MaterialTheme.typography.body2,
                                color = ComposeColor(0xFFB7ADCC)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                DesktopShellChoiceCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Compose Desktop",
                                    description = "Full multiplatform shell with tray integration and shared UI.",
                                    accent = ComposeColor(0xFF8C62FF),
                                    actionLabel = "Open Compose",
                                    onClick = {
                                        selectedMode = DesktopShellMode.COMPOSE
                                    }
                                )
                                DesktopShellChoiceCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Mac Native",
                                    description = "SwiftUI shell for macOS with native windowing and notifications.",
                                    accent = ComposeColor(0xFF45C2A7),
                                    actionLabel = "Open Native",
                                    onClick = {
                                        val launched = MacNativeDesktopShellRunner.launch(background = true)
                                        if (launched != null) {
                                            exitApplication()
                                        } else {
                                            selectedMode = DesktopShellMode.COMPOSE
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            DesktopShellMode.COMPOSE -> ComposeDesktopShellApp(onExitApplication = ::exitApplication)
            else -> Unit
        }
    }
}

@Composable
private fun DesktopShellChoiceCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    accent: ComposeColor,
    actionLabel: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = 220.dp),
        color = ComposeColor(0xFF231C30),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.width(44.dp),
                color = accent.copy(alpha = 0.18f),
                elevation = 0.dp
            ) {
                Text(
                    text = "•",
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = accent,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                color = ComposeColor(0xFFF6F2FF),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.body2,
                color = ComposeColor(0xFFB7ADCC)
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClick
            ) {
                Text(actionLabel)
            }
        }
    }
}

private object ComposeDesktopShellRunner : DesktopShellRunner {
    override fun run() = application {
        ComposeDesktopShellApp(onExitApplication = ::exitApplication)
    }
}

@Composable
private fun ComposeDesktopShellApp(onExitApplication: () -> Unit) {
    val preferences = remember { Preferences.userRoot().node(PreferencesNode) }
    val windowState = rememberDesktopWindowState(preferences)
    val desktopBridge = remember { DesktopBridge() }

    var isWindowVisible by rememberSaveable { mutableStateOf(true) }
    var windowTitle by rememberSaveable { mutableStateOf("Chat Desktop") }
    var unreadNotificationCount by rememberSaveable { mutableStateOf(0) }
    var openWindowRequest by remember { mutableStateOf(0L) }
    var quitRequested by remember { mutableStateOf(false) }
    var awtWindow by remember { mutableStateOf<java.awt.Window?>(null) }

    fun showWindow() {
        isWindowVisible = true
        openWindowRequest += 1
    }

    fun hideWindow() {
        isWindowVisible = false
    }

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
                x = (windowState.position as? WindowPosition.Absolute)?.x?.value,
                y = (windowState.position as? WindowPosition.Absolute)?.y?.value,
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
                SwingUtilities.invokeLater { showWindow() }
            },
            onHide = {
                SwingUtilities.invokeLater { hideWindow() }
            },
            onQuit = {
                SwingUtilities.invokeLater { requestQuit() }
            }
        )
    }

    if (isWindowVisible) {
        Window(
            onCloseRequest = {
                if (desktopBridge.isTraySupported) {
                    hideWindow()
                } else {
                    requestQuit()
                }
            },
            title = windowTitle,
            state = windowState
        ) {
            awtWindow = window
            window.minimumSize = Dimension(1180, 760)

            LaunchedEffect(openWindowRequest) {
                window.isVisible = true
                window.toFront()
                window.requestFocus()
            }

            MenuBar {
                Menu("App") {
                    Item(
                        text = "Hide Window",
                        enabled = desktopBridge.isTraySupported,
                        shortcut = menuShortcut(Key.M),
                        onClick = { hideWindow() }
                    )
                    Item(
                        text = "Show Window",
                        shortcut = menuShortcut(Key.O),
                        onClick = { showWindow() }
                    )
                    Item(
                        text = "Quit",
                        shortcut = menuShortcut(Key.Q),
                        onClick = { requestQuit() }
                    )
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    persistWindowState(preferences, windowState)
                }
            }

            MessagingApp(
                onUnreadNotificationCountChange = { unreadNotificationCount = it },
                onNotificationEvent = { event ->
                    desktopBridge.showNotification(
                        event = event,
                        window = awtWindow,
                        unreadCount = unreadNotificationCount
                    )
                },
                onWindowTitleChange = { title -> windowTitle = title }
            )
        }
    } else {
        LaunchedEffect(openWindowRequest) {
            desktopBridge.ensureTray(
                title = windowTitle,
                unreadCount = unreadNotificationCount,
                onOpen = {
                    SwingUtilities.invokeLater { showWindow() }
                },
                onHide = {
                    SwingUtilities.invokeLater { hideWindow() }
                },
                onQuit = {
                    SwingUtilities.invokeLater { requestQuit() }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            persistWindowState(preferences, windowState)
            desktopBridge.dispose()
        }
    }
}

private object MacNativeDesktopShellRunner : DesktopShellRunner {
    override fun run() {
        val process = launch(background = false)
        if (process == null) {
            ComposeDesktopShellRunner.run()
            return
        }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            System.err.println("SwiftUI shell exited with code $exitCode. Falling back to Compose Desktop.")
            ComposeDesktopShellRunner.run()
        }
    }

    fun launch(background: Boolean): Process? {
        val packageManifest = locateMacNativePackage()
        if (packageManifest == null) {
            System.err.println("Unable to locate app/macosApp/Package.swift.")
            return null
        }

        return runCatching {
            ProcessBuilder(
                "swift",
                "run",
                "--package-path",
                packageManifest.parent.toString(),
                "ChatMacNative"
            ).apply {
                directory(packageManifest.parent.toFile())
                inheritIO()
                environment()["MESSAGING_BASE_URL"] =
                    System.getProperty("messaging.baseUrl")
                        ?: System.getenv("MESSAGING_BASE_URL")
                        ?: "http://localhost:8080"
            }.start()
        }.getOrElse { error ->
            System.err.println("Failed to launch SwiftUI shell: ${error.message}.")
            if (!background) {
                System.err.println("Falling back to Compose Desktop.")
            }
            null
        }
    }
}

private class DesktopBridge {
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
        if (!isTraySupported) {
            return
        }

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

        if (!isTaskbarSupported) {
            return
        }

        runCatching {
            val taskbar = Taskbar.getTaskbar()
            taskbar.setIconBadge(
                unreadCount.takeIf { it > 0 }?.coerceAtMost(99)?.toString().orEmpty()
            )
        }
    }

    fun showNotification(
        event: AppNotificationEvent,
        window: java.awt.Window?,
        unreadCount: Int
    ) {
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
        trayIcon?.let {
            runCatching { SystemTray.getSystemTray().remove(it) }
        }
        trayIcon = null
        if (isTaskbarSupported) {
            runCatching { Taskbar.getTaskbar().setIconBadge("") }
        }
    }

    private fun tooltip(title: String, unreadCount: Int): String {
        return if (unreadCount > 0) "$title ($unreadCount unread)" else title
    }
}

@Composable
private fun rememberDesktopWindowState(preferences: Preferences): WindowState {
    val width = preferences.getFloat(PrefWindowWidth, 1440f).dp
    val height = preferences.getFloat(PrefWindowHeight, 960f).dp
    val x = preferences.getFloat(PrefWindowX, Float.NaN)
    val y = preferences.getFloat(PrefWindowY, Float.NaN)
    val maximized = preferences.getBoolean(PrefWindowMaximized, false)

    return rememberWindowState(
        placement = if (maximized) WindowPlacement.Maximized else WindowPlacement.Floating,
        position = if (x.isNaN() || y.isNaN()) {
            WindowPosition.Aligned(Alignment.Center)
        } else {
            WindowPosition.Absolute(x.dp, y.dp)
        },
        width = width,
        height = height
    )
}

private fun persistWindowState(preferences: Preferences, windowState: WindowState) {
    preferences.putFloat(PrefWindowWidth, windowState.size.width.value)
    preferences.putFloat(PrefWindowHeight, windowState.size.height.value)
    val position = windowState.position
    if (position is WindowPosition.Absolute) {
        preferences.putFloat(PrefWindowX, position.x.value)
        preferences.putFloat(PrefWindowY, position.y.value)
    }
    preferences.putBoolean(PrefWindowMaximized, windowState.placement == WindowPlacement.Maximized)
}

private fun menuShortcut(key: Key): KeyShortcut {
    val isMac = isMacOs()
    return if (isMac) KeyShortcut(key, meta = true) else KeyShortcut(key, ctrl = true)
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

private fun selectedShellMode(): DesktopShellMode {
    val raw = System.getProperty("chat.desktop.shell")
        ?: System.getenv("CHAT_DESKTOP_SHELL")
        ?: "auto"
    return when (raw.lowercase()) {
        "compose" -> DesktopShellMode.COMPOSE
        "mac-native", "native", "swiftui" -> DesktopShellMode.MAC_NATIVE
        "chooser", "choose", "select" -> DesktopShellMode.CHOOSER
        else -> DesktopShellMode.AUTO
    }
}

private fun resolveShellMode(mode: DesktopShellMode): DesktopShellMode {
    return when (mode) {
        DesktopShellMode.AUTO -> if (isMacOs()) DesktopShellMode.CHOOSER else DesktopShellMode.COMPOSE
        DesktopShellMode.COMPOSE -> DesktopShellMode.COMPOSE
        DesktopShellMode.MAC_NATIVE -> if (isMacOs()) DesktopShellMode.MAC_NATIVE else DesktopShellMode.COMPOSE
        DesktopShellMode.CHOOSER -> if (isMacOs()) DesktopShellMode.CHOOSER else DesktopShellMode.COMPOSE
    }
}

private fun isMacOs(): Boolean =
    System.getProperty("os.name").contains("Mac", ignoreCase = true)

private fun locateMacNativePackage(): Path? {
    val cwd = File(System.getProperty("user.dir")).absoluteFile
    var current: File? = cwd
    while (current != null) {
        val candidate = current.toPath().resolve("app").resolve("macosApp").resolve("Package.swift")
        if (candidate.toFile().exists()) {
            return candidate
        }
        current = current.parentFile
    }
    return null
}

private data class DesktopWindowSnapshot(
    val width: Float,
    val height: Float,
    val x: Float?,
    val y: Float?,
    val maximized: Boolean
)

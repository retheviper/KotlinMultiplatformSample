package com.retheviper.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import java.io.File
import java.nio.file.Path
import java.util.prefs.Preferences

internal const val PreferencesNode = "com.retheviper.chat.desktop"
internal const val PrefWindowWidth = "window.width"
internal const val PrefWindowHeight = "window.height"
internal const val PrefWindowX = "window.x"
internal const val PrefWindowY = "window.y"
internal const val PrefWindowMaximized = "window.maximized"

internal fun selectedShellMode(): DesktopShellMode {
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

internal fun resolveShellMode(mode: DesktopShellMode): DesktopShellMode {
    return when (mode) {
        DesktopShellMode.AUTO -> if (isMacOs()) DesktopShellMode.CHOOSER else DesktopShellMode.COMPOSE
        DesktopShellMode.COMPOSE -> DesktopShellMode.COMPOSE
        DesktopShellMode.MAC_NATIVE -> if (isMacOs()) DesktopShellMode.MAC_NATIVE else DesktopShellMode.COMPOSE
        DesktopShellMode.CHOOSER -> if (isMacOs()) DesktopShellMode.CHOOSER else DesktopShellMode.COMPOSE
    }
}

internal fun isMacOs(): Boolean =
    System.getProperty("os.name").contains("Mac", ignoreCase = true)

internal fun locateMacNativePackage(): Path? {
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

@Composable
internal fun rememberDesktopWindowState(preferences: Preferences): WindowState {
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

internal fun persistWindowState(preferences: Preferences, windowState: WindowState) {
    preferences.putFloat(PrefWindowWidth, windowState.size.width.value)
    preferences.putFloat(PrefWindowHeight, windowState.size.height.value)
    val position = windowState.position
    if (position is WindowPosition.Absolute) {
        preferences.putFloat(PrefWindowX, position.x.value)
        preferences.putFloat(PrefWindowY, position.y.value)
    }
    preferences.putBoolean(PrefWindowMaximized, windowState.placement == WindowPlacement.Maximized)
}

internal fun menuShortcut(key: Key): KeyShortcut {
    return if (isMacOs()) KeyShortcut(key, meta = true) else KeyShortcut(key, ctrl = true)
}

internal data class DesktopWindowSnapshot(
    val width: Float,
    val height: Float,
    val x: Float?,
    val y: Float?,
    val maximized: Boolean
)

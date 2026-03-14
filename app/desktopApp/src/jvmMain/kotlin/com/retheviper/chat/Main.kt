package com.retheviper.chat

internal enum class DesktopShellMode {
    AUTO,
    COMPOSE,
    MAC_NATIVE,
    CHOOSER
}

internal interface DesktopShellRunner {
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

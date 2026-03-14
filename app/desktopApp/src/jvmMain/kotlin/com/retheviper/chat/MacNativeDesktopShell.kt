package com.retheviper.chat

internal object MacNativeDesktopShellRunner : DesktopShellRunner {
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

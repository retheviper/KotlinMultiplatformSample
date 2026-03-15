package codex.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.io.File

private const val IOS_APP_SCHEME = "iosApp"
private const val IOS_APP_BUNDLE_ID = "orgIdentifier.iosApp"
private const val IOS_APP_NAME = "KMPs.app"

abstract class BaseCommandTask : DefaultTask() {
    @get:InputDirectory
    abstract val rootDir: DirectoryProperty

    protected fun runCommand(vararg command: String, environment: Map<String, String> = emptyMap()) {
        val processBuilder = ProcessBuilder(*command)
            .directory(rootDir.asFile.get())
            .inheritIO()
        if (environment.isNotEmpty()) {
            processBuilder.environment().putAll(environment)
        }
        val exitCode = processBuilder.start().waitFor()
        if (exitCode != 0) {
            throw GradleException("Command failed (${command.joinToString(" ")}), exit code $exitCode.")
        }
    }

    protected fun captureCommand(vararg command: String): String {
        val error = ByteArrayOutputStream()
        val output = ByteArrayOutputStream()
        val process = ProcessBuilder(*command)
            .directory(rootDir.asFile.get())
            .start()
        process.inputStream.copyTo(output)
        process.errorStream.copyTo(error)
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException(
                "Command failed (${command.joinToString(" ")}), exit code $exitCode: ${error.toString().trim()}"
            )
        }
        return output.toString().trim()
    }

    protected fun optionalCommand(vararg command: String): String? =
        runCatching { captureCommand(*command) }.getOrNull()
}

abstract class ListAppleSimulatorsTask : BaseCommandTask() {
    @TaskAction
    fun list() {
        logger.lifecycle(captureCommand("xcrun", "simctl", "list", "devices", "available"))
    }
}

abstract class ListAndroidAvdsTask : BaseCommandTask() {
    @get:Input
    abstract val androidSdkDir: Property<String>

    @TaskAction
    fun list() {
        logger.lifecycle(captureCommand(resolveAndroidTool("emulator/emulator", "emulator"), "-list-avds"))
    }

    private fun resolveAndroidTool(relativePath: String, fallbackCommand: String): String {
        val sdkDir = androidSdkDir.orNull?.takeIf(String::isNotBlank)?.let(::File) ?: return fallbackCommand
        val candidate = sdkDir.resolve(relativePath)
        return if (candidate.exists()) candidate.absolutePath else fallbackCommand
    }
}

abstract class RunAppleSimulatorTask : BaseCommandTask() {
    @get:Input
    abstract val xcodeProjectPath: Property<String>

    @get:OutputDirectory
    abstract val derivedDataRoot: DirectoryProperty

    @get:Optional
    @get:Input
    abstract val simulatorName: Property<String>

    @get:Input
    abstract val simulatorPropertyName: Property<String>

    @get:Input
    abstract val simulatorLabel: Property<String>

    @get:Optional
    @get:Input
    abstract val baseUrl: Property<String>

    @TaskAction
    fun runSimulator() {
        val selectedSimulator = simulatorName.orNull?.takeIf(String::isNotBlank)
            ?: throw GradleException(
                "Missing -P${simulatorPropertyName.get()}. Example: ./gradlew ${path} -P${simulatorPropertyName.get()}=\"${simulatorLabel.get()}\""
            )
        val derivedDataDir = derivedDataRoot.asFile.get().resolve(
            "${name}/${selectedSimulator.replace(Regex("[^A-Za-z0-9._-]"), "_")}"
        )
        val appBundle = derivedDataDir.resolve("Build/Products/Debug-iphonesimulator/$IOS_APP_NAME")

        runCommand(
            "xcodebuild",
            "-project", xcodeProjectPath.get(),
            "-scheme", IOS_APP_SCHEME,
            "-sdk", "iphonesimulator",
            "-destination", "platform=iOS Simulator,name=$selectedSimulator",
            "-derivedDataPath", derivedDataDir.absolutePath,
            "build"
        )
        optionalCommand("xcrun", "simctl", "boot", selectedSimulator)
        runCommand("xcrun", "simctl", "bootstatus", selectedSimulator, "-b")

        if (!appBundle.exists()) {
            throw GradleException("Built app bundle not found at ${appBundle.absolutePath}.")
        }

        runCommand("xcrun", "simctl", "install", selectedSimulator, appBundle.absolutePath)
        val launchEnvironment = baseUrl.orNull?.takeIf(String::isNotBlank)
            ?.let { mapOf("SIMCTL_CHILD_MESSAGING_BASE_URL" to it) }
            ?: emptyMap()
        runCommand("xcrun", "simctl", "launch", selectedSimulator, IOS_APP_BUNDLE_ID, environment = launchEnvironment)
    }
}

abstract class AndroidRunTask : BaseCommandTask() {
    @get:Input
    abstract val androidSdkDir: Property<String>

    @get:InputDirectory
    abstract val apkOutputDir: DirectoryProperty

    @get:Optional
    @get:Input
    abstract val avdName: Property<String>

    @get:Optional
    @get:Input
    abstract val deviceSerial: Property<String>

    @TaskAction
    fun runAndroid() {
        val adbPath = resolveAndroidTool("platform-tools/adb", "adb")
        var serial = deviceSerial.orNull?.takeIf(String::isNotBlank) ?: findRunningEmulatorSerial(adbPath)

        if (serial == null) {
            val avd = avdName.orNull?.takeIf(String::isNotBlank)
                ?: throw GradleException(
                    "No running Android emulator found. Start one first or pass -PandroidAvd=<avd-name>."
                )
            val emulatorPath = resolveAndroidTool("emulator/emulator", "emulator")
            val before = androidConnectedDevices(adbPath).toSet()
            ProcessBuilder(emulatorPath, "@$avd")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()

            val deadline = System.currentTimeMillis() + 120_000L
            while (serial == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(2_000L)
                serial = androidConnectedDevices(adbPath)
                    .firstOrNull { it.startsWith("emulator-") && it !in before }
            }
            if (serial == null) {
                throw GradleException("Timed out waiting for Android emulator AVD '$avd' to connect.")
            }
        }

        waitForAndroidDeviceBoot(adbPath, serial)
        val apk = findAndroidDebugApk()
        runCommand(adbPath, "-s", serial, "install", "-r", apk.absolutePath)
        runCommand(
            adbPath,
            "-s", serial,
            "shell",
            "am",
            "start",
            "-n",
            "com.retheviper.chat.android/com.retheviper.chat.android.MainActivity"
        )
    }

    private fun resolveAndroidTool(relativePath: String, fallbackCommand: String): String {
        val sdkDir = androidSdkDir.orNull?.takeIf(String::isNotBlank)?.let(::File) ?: return fallbackCommand
        val candidate = sdkDir.resolve(relativePath)
        return if (candidate.exists()) candidate.absolutePath else fallbackCommand
    }

    private fun androidConnectedDevices(adbPath: String): List<String> =
        optionalCommand(adbPath, "devices")
            ?.lineSequence()
            ?.drop(1)
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.mapNotNull { line ->
                val parts = line.split(Regex("\\s+"))
                val serial = parts.firstOrNull() ?: return@mapNotNull null
                val state = parts.getOrNull(1) ?: return@mapNotNull null
                serial.takeIf { state == "device" }
            }
            ?.toList()
            .orEmpty()

    private fun findRunningEmulatorSerial(adbPath: String): String? =
        androidConnectedDevices(adbPath).singleOrNull { it.startsWith("emulator-") }

    private fun waitForAndroidDeviceBoot(adbPath: String, serial: String, timeoutMillis: Long = 240_000L) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMillis) {
            val bootCompleted = optionalCommand(adbPath, "-s", serial, "shell", "getprop", "sys.boot_completed")
            if (bootCompleted == "1") {
                return
            }
            Thread.sleep(2_000L)
        }
        throw GradleException("Timed out waiting for Android emulator $serial to finish booting.")
    }

    private fun findAndroidDebugApk(): File =
        apkOutputDir.asFile.get().listFiles()
            ?.filter { it.extension == "apk" }
            ?.maxByOrNull(File::lastModified)
            ?: throw GradleException("Could not find a debug APK under ${apkOutputDir.asFile.get().absolutePath}.")
}

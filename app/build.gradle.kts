import codex.tasks.AndroidRunTask
import codex.tasks.ListAndroidAvdsTask
import codex.tasks.ListAppleSimulatorsTask
import codex.tasks.RunAppleSimulatorTask
import java.io.File
import java.util.Properties

description = "Application shell modules for Android and Desktop."

fun localPropertiesSdkDir(): File? {
    val localProperties = rootProject.file("local.properties")
    if (!localProperties.exists()) {
        return null
    }
    val properties = Properties()
    localProperties.inputStream().use(properties::load)
    return properties.getProperty("sdk.dir")
        ?.takeIf(String::isNotBlank)
        ?.let(::File)
}

val androidSdkDirPath = sequenceOf(
    providers.environmentVariable("ANDROID_SDK_ROOT").orNull,
    providers.environmentVariable("ANDROID_HOME").orNull,
    localPropertiesSdkDir()?.absolutePath
)
    .filterNotNull()
    .firstOrNull { File(it).exists() }
    ?: ""

tasks.register<ListAppleSimulatorsTask>("listAppleSimulators") {
    group = "application"
    description = "Lists available Apple simulators for app/iosApp."
    rootDir.set(rootProject.layout.projectDirectory)
}

tasks.register<ListAndroidAvdsTask>("listAndroidAvds") {
    group = "application"
    description = "Lists available Android virtual devices."
    rootDir.set(rootProject.layout.projectDirectory)
    androidSdkDir.set(androidSdkDirPath)
}

tasks.register<RunAppleSimulatorTask>("runIosSimulator") {
    group = "application"
    description = "Builds app/iosApp, installs it into the named iPhone simulator, and launches it."
    rootDir.set(rootProject.layout.projectDirectory)
    xcodeProjectPath.set(rootProject.layout.projectDirectory.file("app/iosApp/iosApp.xcodeproj").asFile.absolutePath)
    derivedDataRoot.set(layout.buildDirectory.dir("xcode"))
    simulatorName.convention(providers.gradleProperty("iosSimulator"))
    simulatorPropertyName.set("iosSimulator")
    simulatorLabel.set("iPhone 16")
    baseUrl.convention(
        providers.gradleProperty("messaging.baseUrl")
            .orElse(providers.systemProperty("messaging.baseUrl"))
            .orElse(providers.environmentVariable("MESSAGING_BASE_URL"))
    )
}

tasks.register<RunAppleSimulatorTask>("runIpadSimulator") {
    group = "application"
    description = "Builds app/iosApp, installs it into the named iPad simulator, and launches it."
    rootDir.set(rootProject.layout.projectDirectory)
    xcodeProjectPath.set(rootProject.layout.projectDirectory.file("app/iosApp/iosApp.xcodeproj").asFile.absolutePath)
    derivedDataRoot.set(layout.buildDirectory.dir("xcode"))
    simulatorName.convention(providers.gradleProperty("ipadSimulator"))
    simulatorPropertyName.set("ipadSimulator")
    simulatorLabel.set("iPad Pro 13-inch (M4)")
    baseUrl.convention(
        providers.gradleProperty("messaging.baseUrl")
            .orElse(providers.systemProperty("messaging.baseUrl"))
            .orElse(providers.environmentVariable("MESSAGING_BASE_URL"))
    )
}

tasks.register<AndroidRunTask>("runAndroidEmulator") {
    group = "application"
    description = "Assembles, installs, and launches the Android app on a running emulator or a named AVD."
    dependsOn(":app:androidApp:assembleDebug")
    rootDir.set(rootProject.layout.projectDirectory)
    androidSdkDir.set(androidSdkDirPath)
    apkOutputDir.set(rootProject.layout.projectDirectory.dir("app/androidApp/build/outputs/apk/debug"))
    avdName.convention(providers.gradleProperty("androidAvd"))
    deviceSerial.convention(providers.gradleProperty("androidDeviceSerial"))
}

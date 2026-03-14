import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.JavaExec

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src/jvmMain/kotlin"))
        resources.setSrcDirs(listOf("src/jvmMain/resources"))
    }
    test {
        java.setSrcDirs(listOf("src/jvmTest/kotlin"))
        resources.setSrcDirs(listOf("src/jvmTest/resources"))
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.koin.core)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.instancio.junit)
    testImplementation(libs.instancio.core)
}

compose.desktop {
    application {
        mainClass = "com.retheviper.chat.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "chat-desktop"
            packageVersion = "1.0.0"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaExec>().configureEach {
    System.getProperty("chat.desktop.shell")?.let { systemProperty("chat.desktop.shell", it) }
    System.getProperty("messaging.baseUrl")?.let { systemProperty("messaging.baseUrl", it) }
    System.getenv("CHAT_DESKTOP_SHELL")?.let { environment("CHAT_DESKTOP_SHELL", it) }
    System.getenv("MESSAGING_BASE_URL")?.let { environment("MESSAGING_BASE_URL", it) }
}

kotlin {
    jvmToolchain(17)
}

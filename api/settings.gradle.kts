rootProject.name = "bbs-server"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("multiplatform").version(extra["kotlinVersion"] as String)
        kotlin("plugin.serialization").version(extra["kotlinVersion"] as String)
        id("org.jetbrains.compose").version(extra["composeVersion"] as String)
        id("io.ktor.plugin").version(extra["ktorVersion"] as String)
    }
}
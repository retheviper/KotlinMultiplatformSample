import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization") version "1.7.20"
}

group = "com.retheviper.bbs"
version = "1.0-SNAPSHOT"

val ktorVersion: String by project
val koinVersion: String by project
val kotestVersion: String by project

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.21")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-apache:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.insert-koin:koin-ktor:$koinVersion")
                implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
                implementation("io.mockk:mockk:1.13.4")
                implementation("org.instancio:instancio-junit:2.10.0")
                implementation("org.instancio:instancio-core:2.10.0")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.retheviper.bbs.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "bbs-app-desktop"
            packageVersion = "1.0.0"
        }
    }
}

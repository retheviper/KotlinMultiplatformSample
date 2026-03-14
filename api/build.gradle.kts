plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass = "com.retheviper.bbs.ServerMainKt"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src/jvmMain/kotlin"))
        resources.setSrcDirs(listOf("src/jvmMain/resources"))
    }
    test {
        java.setSrcDirs(listOf("src/jvmTest/kotlin"))
    }
}

dependencies {
    implementation(project(":contract"))

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.logback.classic)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)

    implementation(libs.hikari)
    implementation(libs.mysql.connector)
    implementation(libs.jhash)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.instancio.junit)
    testImplementation(libs.instancio.core)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

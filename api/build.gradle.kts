plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.flyway)
    application
}

application {
    mainClass = "com.retheviper.chat.ApplicationKt"
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
        resources.srcDir(layout.buildDirectory.dir("generated/frontendResources"))
    }
    test {
        java.setSrcDirs(listOf("src/jvmTest/kotlin"))
        resources.setSrcDirs(listOf("src/jvmTest/resources"))
    }
}

val syncFrontend by tasks.registering(Sync::class) {
    dependsOn(":shared:wasmJsBrowserDevelopmentExecutableDistribution")
    from(project(":shared").layout.buildDirectory.dir("dist/wasmJs/developmentExecutable"))
    into(layout.buildDirectory.dir("generated/frontendResources/web"))
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.websockets)
    implementation(libs.logback.classic)
    implementation(libs.mcp.sdk.server)

    implementation(libs.exposed.core)
    implementation(libs.exposed.r2dbc)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.postgresql)
    implementation(libs.r2dbc.pool)
    implementation(libs.r2dbc.postgresql)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.websockets)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
}

flyway {
    url = providers.environmentVariable("FLYWAY_URL")
        .orElse("jdbc:postgresql://localhost:5432/messaging_app")
        .get()
    user = providers.environmentVariable("FLYWAY_USER")
        .orElse("postgres")
        .get()
    password = providers.environmentVariable("FLYWAY_PASSWORD")
        .orElse("postgres")
        .get()
    locations = arrayOf("classpath:db/migration")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    dependsOn(syncFrontend)
}

kotlin {
    jvmToolchain(17)
}

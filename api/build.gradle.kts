import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val exposedVersion: String by project
val koinVersion: String by project
val mysqlVersion: String by project
val kotestVersion: String by project

plugins {
    application
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("io.ktor.plugin")
}

group = "com.retheviper"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    jvm {
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(compose.html.core)
                implementation(compose.runtime)
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val jvmMain by getting {
            dependencies {
                // Kotlin
                implementation(kotlin("stdlib-jdk8"))

                // Ktor
                implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
                implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
                implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
                implementation("io.ktor:ktor-server-websockets:$ktorVersion")
                implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
                implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
                implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
                implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
                implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
                implementation("io.ktor:ktor-server-request-validation:$ktorVersion")
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
                implementation(compose.runtime)

                // Koin
                implementation("io.insert-koin:koin-ktor:$koinVersion")
                implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

                // Exposed
                implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")


                // Database
                implementation("com.zaxxer:HikariCP:5.0.1")
                implementation("mysql:mysql-connector-java:$mysqlVersion")

                // Hash
                implementation("com.amdelamar:jhash:2.2.0")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.ktor:ktor-server-test-host:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
                implementation("io.mockk:mockk:1.13.5")
                implementation("org.instancio:instancio-junit:2.16.0")
                implementation("org.instancio:instancio-core:2.16.0")
            }
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    getByName<JavaExec>("run") {
        classpath(getByName<Jar>("jvmJar")) // so that the JS artifacts generated by `jvmJar` can be found and served
    }

    // include JS artifacts in any JAR when generate
    getByName<Jar>("jvmJar") {
        val taskName = if (project.hasProperty("isProduction")
            || project.gradle.startParameter.taskNames.contains("installDist")
        ) {
            "jsBrowserProductionWebpack"
        } else {
            "jsBrowserDevelopmentWebpack"
        }
        val webpackTask = getByName<KotlinWebpack>(taskName)
        dependsOn(webpackTask) // make sure JS gets compiled first
        from(File(webpackTask.destinationDirectory, webpackTask.outputFileName)) // bring output file along into the JAR
    }

    getByName<KotlinWebpack>("jsBrowserProductionWebpack") {
        dependsOn("jsDevelopmentExecutableCompileSync")
    }

    getByName<KotlinWebpack>("jsBrowserDevelopmentWebpack") {
        dependsOn("jsProductionExecutableCompileSync")
    }

    distZip {
        dependsOn(allMetadataJar)
        dependsOn(getByName<Jar>("jsJar"))
        dependsOn(shadowJar)
    }

    distTar {
        dependsOn(allMetadataJar)
        dependsOn(getByName<Jar>("jsJar"))
        dependsOn(shadowJar)
    }
}

distributions {
    main {
        contents {
            from("$buildDir/libs") {
                rename("${rootProject.name}-jvm", rootProject.name)
                into("lib")
            }
        }
    }
}
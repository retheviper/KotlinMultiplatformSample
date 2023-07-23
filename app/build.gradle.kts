buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.0")
    }
}

plugins {
    //trick: for the same plugin versions in all submodules
    id("com.android.application").version("7.4.0").apply(false)
    id("com.android.library").version("7.4.0").apply(false)
    id("org.jetbrains.compose").apply(false)
    kotlin("multiplatform").apply(false)
    kotlin("plugin.serialization").version("1.8.10").apply(false)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

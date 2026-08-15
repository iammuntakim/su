@Suppress("UnstableApiUsage")
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "9.0.1"
        id("org.jetbrains.kotlin.android") version "2.3.20"
        id("org.jetbrains.kotlin.plugin.parcelize") version "2.3.20"
        id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
        id("com.google.devtools.ksp") version "2.3.11"
        id("androidx.navigation.safeargs.kotlin") version "2.9.7"
        id("dev.zacsweers.moshix") version "0.34.4"
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "SuperSU"
include(":apk")

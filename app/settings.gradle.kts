@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://maven.yukonga.top/releases")
    }
}

Plugins {
    id("MagiskPlugin")
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)

    subprojects.forEach {
        dependsOn(":${it.name}:clean")
    }
}

rootProject.name = "Magisk"
include(":apk", ":core", ":shared", ":stub", ":test")

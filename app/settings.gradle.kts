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

rootProject.name = "Magisk"
include(":apk", ":core", ":shared", ":stub", ":test")

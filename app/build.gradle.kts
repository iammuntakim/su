plugins {
    id("SuperSuPlugin")
    id("com.android.application")
}

android {
    compileSdk = release(36) {
        minorApiLevel = 1
    }
    buildToolsVersion = "36.1.0"
    defaultConfig {
        minSdk = 23
        targetSdk = 36
    }
}

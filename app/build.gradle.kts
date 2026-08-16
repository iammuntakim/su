plugins {
    id("SuperSuPlugin")
    id("com.android.application")
}

android {
    namespace = "su.android"
    compileSdk = 36
    buildToolsVersion = "36.1.0"
    defaultConfig {
        minSdk = 23
        targetSdk = 36
    }
}

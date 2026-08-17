plugins {
    id("SuperSuPlugin")
    id("com.android.application")
    id("com.google.devtools.ksp")
}

android {
    namespace = "android.sum"
    compileSdk = 36
    buildToolsVersion = "36.1.0"
    defaultConfig {
        minSdk = 23
        targetSdk = 36
    }
}

dependencies {
    implementation(libs.timber)
    implementation(libs.markwon.core)
    implementation(libs.bcpkix)
    implementation(libs.commons.compress)

    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.libsu.nio)

    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.retrofit.scalars)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.dnsoverhttps)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.core.splashscreen)
    implementation(libs.core.ktx)
    implementation(libs.activity)
    implementation(libs.collection.ktx)
    implementation(libs.profileinstaller)
    implementation(libs.material)

    implementation(libs.indeterminate.checkbox)
    implementation(libs.rikka.layoutinflater)
    implementation(libs.rikka.insets)
    implementation(libs.rikka.recyclerview)

    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    implementation(libs.recyclerview)
    implementation(libs.transition)
    implementation(libs.fragment.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
}

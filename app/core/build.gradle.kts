plugins {
    id("com.android.library")
    kotlin("plugin.parcelize")
    id("dev.zacsweers.moshix")
    id("com.google.devtools.ksp")
}

setupCoreLib()

ksp {
    arg("room.generateKotlin", "true")
}

android {
    namespace = "su.android.core"

    defaultConfig {
        buildConfigField("String", "APP_PACKAGE_NAME", "\"su.android\"")
        buildConfigField("int", "APP_VERSION_CODE", "${Config.versionCode}")
        buildConfigField("String", "APP_VERSION_NAME", "\"${Config.version}\"")
        buildConfigField("int", "STUB_VERSION", Config.stubVersion)
        consumerProguardFile("proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // Enable resource shrinking if you have resources to strip
            // isShrinkResources = true 
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation(files("../lib/native.jar"))

    api(project(":shared"))
    coreLibraryDesugaring(libs.jdk.libs)

    api(libs.timber)
    api(libs.markwon.core)
    implementation(libs.bcpkix)
    implementation(libs.commons.compress)

    api(libs.libsu.core)
    api(libs.libsu.service)
    api(libs.libsu.nio)

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

    compileOnly(libs.test.junit)
    compileOnly(libs.test.uiautomator)
}

tasks.matching { it.name.contains("JniLibs") }.configureEach {
    enabled = false
}

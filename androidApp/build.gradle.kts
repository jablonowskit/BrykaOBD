import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "app.brykaobd.android"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.brykaobd"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        getByName("debug") {
            // A fresh GitHub Actions runner has no ~/.android/debug.keystore, so AGP
            // generates a new random one per build — reinstalling over a previous CI
            // build then fails with a signature mismatch. CI provides a stable keystore
            // via ANDROID_DEBUG_KEYSTORE_PATH (restored from a secret); local dev falls
            // back to the normal per-machine debug keystore untouched.
            System.getenv("ANDROID_DEBUG_KEYSTORE_PATH")?.let { path ->
                storeFile = file(path)
                storePassword = System.getenv("ANDROID_DEBUG_KEYSTORE_PASSWORD") ?: "android"
                keyAlias = System.getenv("ANDROID_DEBUG_KEY_ALIAS") ?: "brykaobddebugkey"
                keyPassword = System.getenv("ANDROID_DEBUG_KEY_PASSWORD") ?: "android"
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kitsunime.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kitsunime.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"
        // libmpv membawa pustaka native besar untuk 4 ABI; aplikasi ini
        // ditujukan ke perangkat arm64 (A15) dulu supaya APK ramping.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("stable") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("stable")
        }
        release {
            signingConfig = signingConfigs.getByName("stable")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    testImplementation("junit:junit:4.13.2")

    // Mesin pemutar: mpv lewat libmpv (mpv 0.42 + FFmpeg + libass).
    implementation("io.github.midasvo:libmpv:1.2.0")
}

// libmpv 1.2.0 diterbitkan dengan syarat minCompileSdk 37 (Android 17).
// Google kini menerbitkan platform SDK sebagai 37.0/37.1/37.2 ("minor SDK")
// yang hanya dipahami AGP 9; platform polos untuk AGP 8.7.3 berhenti di 36.
// API publik libmpv hanya memakai API Android lama (Context, Surface) dan
// minSdk-nya 26, jadi pemeriksaan metadata AAR ini dilewati secara sadar.
// Naikkan ke AGP 9 + compileSdk 37.0 bila perkakasnya diganti nanti.
tasks.configureEach {
    if (name.endsWith("AarMetadata")) enabled = false
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.goodbp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cuteysamurai.goodbp" // permanent once on Play; not shown to users
        minSdk = 26          // Android 8.0: covers most Oppo phones still in use
        targetSdk = 36
        // CI sets VERSION_CODE from the run number so every upload is higher.
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "0.2.1"
    }

    // Upload key comes from GitHub secrets (never committed). Without them, release stays unsigned.
    val ks = System.getenv("UPLOAD_KEYSTORE_PATH")
    signingConfigs {
        if (ks != null) create("upload") {
            storeFile = file(ks)
            storePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("UPLOAD_KEY_ALIAS")
            keyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (ks != null) signingConfig = signingConfigs.getByName("upload")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Local storage first; Firebase comes later for family linking.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Photo logging: camera + on-device OCR.
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // History chart (wired up in the history step).
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0")

    testImplementation("junit:junit:4.13.2")
}

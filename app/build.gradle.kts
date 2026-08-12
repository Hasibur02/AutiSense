plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.asddetection"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.asddetection"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // AndroidX
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // =========================================================
    // Retrofit
    // =========================================================

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")


    // =========================================================
    // CameraX
    // =========================================================

    implementation("androidx.camera:camera-core:1.5.0")
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")


    // =========================================================
    // EXIF Orientation
    // =========================================================

    implementation("androidx.exifinterface:exifinterface:1.4.2")


    // =========================================================
    // Testing
    // =========================================================

    testImplementation(libs.junit)

    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
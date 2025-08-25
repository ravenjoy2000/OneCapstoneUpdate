plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics") version "2.9.9"
    id ("kotlin-parcelize")



}

android {
    namespace = "com.example.mediconnect"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mediconnect"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase (use BoM for version alignment)
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-messaging")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Circle ImageView
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Facebook Login (pick one version, don’t mix `latest.release`)
    implementation("com.facebook.android:facebook-login:16.3.0")

    // Zego Call
    implementation("com.github.ZEGOCLOUD:zego_uikit_prebuilt_call_android:latest.release")

    // FirebaseUI Auth (optional — remove if doing custom login flows)
    // implementation("com.firebaseui:firebase-ui-auth:8.0.2")


    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

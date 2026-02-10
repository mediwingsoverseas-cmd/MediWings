plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Google Services plugin: Applies Firebase configuration from google-services.json
    // IMPORTANT: Ensure google-services.json is present in app/ directory before building
    id("com.google.gms.google-services")
}

android {
    // Package name for MediWings app - MUST match package_name in google-services.json
    namespace = "com.tripplanner.mediwings"
    compileSdk = 34

    defaultConfig {
        // Application ID for production release - MUST match Firebase project configuration
        applicationId = "com.tripplanner.mediwings"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // AndroidX Core Libraries
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Firebase SDK - Using BoM (Bill of Materials) for version management
    // BoM ensures all Firebase dependencies use compatible versions
    // Latest stable version as of Feb 2024
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    
    // Firebase Services (versions managed by BoM)
    implementation("com.google.firebase:firebase-analytics-ktx") // Analytics for app insights
    implementation("com.google.firebase:firebase-auth-ktx") // Authentication service
    implementation("com.google.firebase:firebase-database-ktx") // Realtime Database
    implementation("com.google.firebase:firebase-firestore-ktx") // Cloud Firestore for user records
    implementation("com.google.firebase:firebase-storage-ktx") // Cloud Storage for files/images
    implementation("com.google.firebase:firebase-messaging-ktx") // Cloud Messaging (FCM) for push notifications
    
    // Image Loading Library
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    // Rich Text Editor
    implementation("jp.wasabeef:richeditor-android:2.0.0")
}
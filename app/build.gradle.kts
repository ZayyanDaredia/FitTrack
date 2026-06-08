plugins {
    id("com.android.application")
    // REMOVED: id("org.jetbrains.kotlin.android") <- This was causing the "already registered" error
}

android {
    namespace = "com.example.fittrack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fittrack"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // ... your existing dependencies ...
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}

dependencies {
    // Standard AndroidX Libraries
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Material Design - REQUIRED for Side Menu/NavigationView
    implementation("com.google.android.material:material:1.9.0")

    // Navigation UI - REQUIRED for the Hamburger Menu logic
    implementation("androidx.navigation:navigation-fragment:2.6.0")
    implementation("androidx.navigation:navigation-ui:2.6.0")

    // UI Components
    implementation("androidx.cardview:cardview:1.0.0")
    implementation(libs.activity)

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("com.google.android.material:material:1.9.0")
}
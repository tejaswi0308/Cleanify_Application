plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.cleanify_application"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cleanify_demo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase configuration - read from local.properties
        val localPropertiesFile = rootProject.file("local.properties")
        val supabaseUrl = if (localPropertiesFile.exists()) {
            localPropertiesFile.readLines()
                .find { it.startsWith("SUPABASE_URL=") }
                ?.substringAfter("SUPABASE_URL=")
                ?: ""
        } else ""
        val supabaseKey = if (localPropertiesFile.exists()) {
            localPropertiesFile.readLines()
                .find { it.startsWith("SUPABASE_KEY=") }
                ?.substringAfter("SUPABASE_KEY=")
                ?: ""
        } else ""
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }

    buildFeatures {
        buildConfig = true
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Supabase REST
    implementation(libs.okhttp)
    implementation(libs.gson)

    // UI Libraries
    implementation(libs.circleimageview)
    implementation(libs.glide)

    // QR Code
    implementation(libs.zxing.android)
    implementation(libs.zxing.core)
}
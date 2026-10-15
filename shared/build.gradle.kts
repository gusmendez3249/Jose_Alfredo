plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "mx.utng.festivaltrack.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 30

        // URL del backend. Por defecto apunta al PC desde el emulador (10.0.2.2).
        // Para un teléfono real usa la de Railway, p. ej. en gradle.properties o con
        // ./gradlew assembleDebug -PfestivaltrackApiUrl=https://festivaltrack-backend.up.railway.app/api/v1/
        val apiUrl = (project.findProperty("festivaltrackApiUrl") as String?) ?: "http://10.0.2.2:3001/api/v1/"
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}

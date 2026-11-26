import java.util.Properties

// IP de la PC (backend) vista desde un celular físico. Se define en local.properties
// (ignorado por git):  festivaltrack.deviceHost=192.168.X.X
// Si no se define, un celular físico usará 10.0.2.2 (solo válido en emulador).
val localProperties: Properties = Properties().also { props ->
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
}
val deviceHost: String = localProperties.getProperty("festivaltrack.deviceHost") ?: "10.0.2.2"

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
        buildConfigField("String", "DEVICE_HOST", "\"$deviceHost\"")

        // URL fija del backend (opcional). Vacía = detección automática: 10.0.2.2 en el emulador
        // y la IP de la PC en un teléfono físico (ver FestivalApiService.SERVER_URL).
        // Ejemplo: ./gradlew assembleDebug -PfestivaltrackApiUrl=https://festivaltrack-backend.up.railway.app/api/v1/
        val apiUrl = (project.findProperty("festivaltrackApiUrl") as String?) ?: ""
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")

        // Página web donde se compran boletos (la abre el QR de la TV en el celular).
        // Se define con -PfestivaltrackWebUrl=https://... o en local.properties con
        // festivaltrack.webUrl=http://192.168.X.X:5173. Vacía = la PC del backend en el puerto 5173
        // (servidor de Vite con `npm run dev -- --host`).
        val webUrl = (project.findProperty("festivaltrackWebUrl") as String?)
            ?: localProperties.getProperty("festivaltrack.webUrl")
            ?: ""
        buildConfigField("String", "WEB_BASE_URL", "\"$webUrl\"")
    }

    buildFeatures { buildConfig = true }

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
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    kapt("androidx.room:room-compiler:2.8.5")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Códigos QR reales (escaneables): boletos en el móvil y compra desde la TV
    implementation("com.google.zxing:core:3.5.3")

    testImplementation("junit:junit:4.13.2")
}

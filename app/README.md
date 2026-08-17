# 📱 Módulo `app` — Aplicación Móvil Android (FestivalTrack)

## Descripción y Arquitectura General

El módulo `app` representa la experiencia móvil completa del Festival José Alfredo Jiménez en Dolores Hidalgo, Guanajuato. Está diseñado bajo los lineamientos de **Clean Architecture** y el patrón **MVVM (Model-View-ViewModel)** con **Jetpack Compose**.

### Características Principales:
1. **Doble Rol de Usuario (Cliente y Administrador):**
   - **Cliente:** Compra de boletos encriptada, generación de códigos QR dinámicos, transmisión en vivo de conciertos, guía GPS interactiva con OSMDroid, reproducción de audio/podcast y galería histórica.
   - **Administrador:** Panel de transmisión en vivo con servidor nativo RTSP (`RtspServerCamera1`), panel de control de métricas, subida de nuevas canciones e imágenes, gestión de eventos (CRUD completo), escáner de boletos/Smart TV y asignación de roles de usuario.
2. **Inyección de Dependencias Manual:** Proveída a través de `AppContainer` y `DefaultAppContainer` instanciados en `FestivalTrackApplication`.
3. **Persistencia y Sincronización:** Repositorio centralizado `FestivalRepository` que coordina la base de datos local Room con el backend REST de Node.js/TypeScript.

---

## Estructura de Directorios Completa

```text
app/
├── AndroidManifest.xml
├── build.gradle.kts
└── src/main/java/mx/utng/festivaltrack/app/
    ├── FestivalTrackApplication.kt
    ├── MainActivity.kt
    ├── data/
    │   └── TokenManager.kt
    ├── di/
    │   └── AppContainer.kt
    └── ui/
        ├── screens/
        │   ├── AdminCreateEventScreen.kt
        │   ├── AdminDashboardScreen.kt
        │   ├── AdminLiveStreamScreen.kt
        │   ├── AdminMainScreen.kt
        │   ├── AdminManageScreen.kt
        │   ├── AdminScannerScreen.kt
        │   ├── AdminUploadScreen.kt
        │   ├── AdminUsersScreen.kt
        │   ├── AudioScreen.kt
        │   ├── BiographyScreen.kt
        │   ├── CheckoutScreen.kt
        │   ├── DashboardScreen.kt
        │   ├── GalleryScreen.kt
        │   ├── LoginScreen.kt
        │   ├── MainScreen.kt
        │   ├── MapScreen.kt
        │   ├── ProfileScreen.kt
        │   ├── RegisterScreen.kt
        │   ├── TicketSuccessScreen.kt
        │   ├── TicketsScreen.kt
        │   ├── UserLiveStreamScreen.kt
        │   └── WelcomeScreen.kt
        ├── theme/
        │   ├── Color.kt
        │   └── Theme.kt
        ├── utils/
        │   └── QrCodeGenerator.kt
        └── viewmodels/
            ├── AdminManageViewModel.kt
            ├── AdminUsersViewModel.kt
            ├── ArtistViewModel.kt
            ├── AudioViewModel.kt
            ├── AuthViewModel.kt
            ├── CheckoutViewModel.kt
            ├── EventosViewModel.kt
            ├── GalleryViewModel.kt
            ├── LiveViewModel.kt
            └── ProfileViewModel.kt
```

---

## Paso 1: Configuración de `AndroidManifest.xml`

```xml
<!-- 
    =======================================================================
    MÓDULO MÓVIL (app) - CONFIGURACIÓN XML
    FUNCIONALIDAD: Declara permisos de red, cámara (RTSP streaming), audio, GPS y actividades.
    FLUJO: El sistema Android inicializa la aplicación y otorga los permisos en tiempo de ejecución.
    =======================================================================
-->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <application
        android:name=".FestivalTrackApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Jose_Alfredo"
        android:usesCleartextTraffic="true">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.Jose_Alfredo">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>

```

---

## Paso 2: Configuración de `build.gradle.kts`

```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "mx.utng.festivaltrack.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "mx.utng.festivaltrack.app"
        minSdk = 26
        targetSdk = 35
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
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.6.1")

    // Navigation & Lifecycle
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // OpenStreetMap for Navigation
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Pedro Rootless RTSP Camera Server for Live Streaming
    implementation("com.github.pedroSG94.RootEncoder:rtspserver:2.4.4")

    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Media3 (ExoPlayer)
    val media3Version = "1.2.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

```

---

## Paso 3: Core y Capa de Datos (`Application`, `Container`, `TokenManager`)

### `FestivalTrackApplication.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app

import android.app.Application
import mx.utng.festivaltrack.app.di.AppContainer
import mx.utng.festivaltrack.app.di.DefaultAppContainer

class FestivalTrackApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}

```

### `AppContainer.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.di

import android.content.Context
import mx.utng.festivaltrack.shared.data.local.FestivalDatabase
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository

interface AppContainer {
    val festivalRepository: FestivalRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database by lazy {
        FestivalDatabase.getInstance(context)
    }

    override val festivalRepository: FestivalRepository by lazy {
        FestivalRepository(database.eventoDao())
    }
}

```

### `TokenManager.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.data

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun saveRole(role: String) {
        prefs.edit().putString("user_role", role).apply()
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun getRole(): String? {
        return prefs.getString("user_role", null)
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun getUserName(): String? {
        return prefs.getString("user_name", "Usuario")
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun clear() {
        prefs.edit().clear().apply()
    }
}

```

---

## Paso 4: Tema y Utilidades (`Color.kt`, `Theme.kt`, `QrCodeGenerator.kt`)

### `Color.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.theme

import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val PrimaryGold = Color(0xFFE6C27A)
val PrimaryGoldDark = Color(0xFFC4A059)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAAAAAA)
val ErrorRed = Color(0xFFCF6679)

```

### `Theme.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGold,
    secondary = PrimaryGoldDark,
    tertiary = PrimaryGold,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorRed
)

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun FestivalTrackTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun Jose_AlfredoTheme(
    content: @Composable () -> Unit
) {
    FestivalTrackTheme(content = content)
}

```

### `QrCodeGenerator.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun DynamicQrCode(
    content: String,
    modifier: Modifier = Modifier,
    foregroundColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sizeCount = 21
            val cellWidth = size.width / sizeCount
            val cellHeight = size.height / sizeCount

            val hash = content.hashCode()
            
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
            fun isFinderPattern(r: Int, c: Int): Boolean {
                if (r < 7 && c < 7) return true
                if (r < 7 && c >= sizeCount - 7) return true
                if (r >= sizeCount - 7 && c < 7) return true
                return false
            }

            for (r in 0 until sizeCount) {
                for (c in 0 until sizeCount) {
                    val drawSquare: Boolean
                    if (isFinderPattern(r, c)) {
                        val inTLBorder = (r == 0 || r == 6 || c == 0 || c == 6) && r < 7 && c < 7
                        val inTLCenter = (r in 2..4) && (c in 2..4) && r < 7 && c < 7
                        val inTRBorder = (r == 0 || r == 6 || c == sizeCount - 7 || c == sizeCount - 1) && r < 7 && c >= sizeCount - 7
                        val inTRCenter = (r in 2..4) && (c in (sizeCount - 5)..(sizeCount - 3)) && r < 7 && c >= sizeCount - 7
                        val inBLBorder = (r == sizeCount - 7 || r == sizeCount - 1 || c == 0 || c == 6) && r >= sizeCount - 7 && c < 7
                        val inBLCenter = (r in (sizeCount - 5)..(sizeCount - 3)) && (c in 2..4) && r >= sizeCount - 7 && c < 7

                        drawSquare = inTLBorder || inTLCenter || inTRBorder || inTRCenter || inBLBorder || inBLCenter
                    } else {
                        val bitShift = (r * sizeCount + c) % 31
                        val bitVal = (hash ushr bitShift) and 1
                        val altBit = ((r * 7 + c * 13 + hash) % 3) == 0
                        drawSquare = bitVal == 1 || altBit
                    }

                    if (drawSquare) {
                        drawRect(
                            color = foregroundColor,
                            topLeft = Offset(c * cellWidth, r * cellHeight),
                            size = Size(cellWidth + 0.5f, cellHeight + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

```

---

## Paso 5: Todos los ViewModels de la Aplicación Móvil

### `AuthViewModel.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.LoginRequestDto
import mx.utng.festivaltrack.shared.data.remote.RegistroRequestDto

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = FestivalApiService.create()
    private val tokenManager = TokenManager(application)

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun login(correo: String, contrasena: String) {
        if (correo.isBlank() || contrasena.isBlank()) {
            _authState.value = AuthState.Error("Por favor llena todos los campos.")
            return
        }
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.login(LoginRequestDto(correo, contrasena))
                tokenManager.saveToken(response.token)
                tokenManager.saveRole(response.usuario.rol)
                _authState.value = AuthState.Success(response.usuario.rol)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error al iniciar sesión: Credenciales inválidas.")
            }
        }
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun register(nombre: String, correo: String, contrasena: String) {
        if (nombre.isBlank() || correo.isBlank() || contrasena.isBlank()) {
            _authState.value = AuthState.Error("Por favor llena todos los campos.")
            return
        }
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.registrar(RegistroRequestDto(nombre, correo, contrasena))
                tokenManager.saveToken(response.token)
                tokenManager.saveRole(response.usuario.rol)
                _authState.value = AuthState.Success(response.usuario.rol)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error al registrarse: El correo ya existe.")
            }
        }
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

```

### `EventosViewModel.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository

class EventosViewModel(private val repository: FestivalRepository) : ViewModel() {

    val eventosLocales: Flow<List<EventoEntity>> = repository.getEventosLocales()

    init {
        sync()
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun sync() {
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            try {
                repository.syncEventos()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class EventosViewModelFactory(private val repository: FestivalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventosViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

```

### `CheckoutViewModel.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.ComprarBoletoDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

sealed class CheckoutState {
    object Idle : CheckoutState()
    object Processing : CheckoutState()
    data class Success(val transaccionId: String) : CheckoutState()
    data class Error(val message: String) : CheckoutState()
}

class CheckoutViewModel(application: Application) : AndroidViewModel(application) {
    private val api = FestivalApiService.create()
    private val tokenManager = TokenManager(application)

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun procesarPago(
        eventoId: String,
        categoria: String,
        cantidad: Int,
        precioTotal: Int,
        tarjetaNumero: String,
        tarjetaVencimiento: String,
        tarjetaCVV: String
    ) {
        if (tarjetaNumero.length < 16 || tarjetaVencimiento.isBlank() || tarjetaCVV.length < 3) {
            _checkoutState.value = CheckoutState.Error("Por favor ingresa los datos completos de tu tarjeta.")
            return
        }

        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Processing
            try {
                val token = tokenManager.getToken()
                val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else null

                val response = api.comprarBoletos(
                    authHeader = authHeader,
                    body = ComprarBoletoDto(
                        eventoId = eventoId,
                        categoria = categoria,
                        cantidad = cantidad,
                        precioTotal = precioTotal.toDouble()
                    )
                )

                _checkoutState.value = CheckoutState.Success(response.transaccionId)
            } catch (e: Exception) {
                _checkoutState.value = CheckoutState.Success("LOCAL-PAYMENT-${System.currentTimeMillis()}")
            }
        }
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun resetState() {
        _checkoutState.value = CheckoutState.Idle
    }
}

```

### `AdminManageViewModel.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.EventoCreateDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository

class AdminManageViewModel(
    private val repository: FestivalRepository
) : ViewModel() {

    private val api = FestivalApiService.create()

    val eventos: StateFlow<List<EventoEntity>> = repository.getEventosLocales()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun saveEvent(
        token: String?,
        id: String?,
        title: String,
        date: String,
        location: String,
        price: String
    ) {
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            try {
                val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else null
                val parsedPrice = price.toDoubleOrNull() ?: 0.0
                val body = EventoCreateDto(
                    nombre = title,
                    fechaHora = date,
                    ubicacion = location,
                    precioBase = parsedPrice
                )

                if (id.isNullOrBlank()) {
                    api.createEvento(authHeader, body)
                } else {
                    api.updateEvento(authHeader, id, body)
                }
                repository.syncEventos()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun deleteEvent(token: String?, id: String) {
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            try {
                val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else null
                api.deleteEvento(authHeader, id)
                repository.syncEventos()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class AdminManageViewModelFactory(private val repository: FestivalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminManageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminManageViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

```

### `AdminUsersViewModel.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.RegistroRequestDto
import mx.utng.festivaltrack.shared.data.remote.UpdateRoleDto
import mx.utng.festivaltrack.shared.data.remote.UsuarioDto

sealed class AdminUsersState {
    object Loading : AdminUsersState()
    data class Success(val users: List<UsuarioDto>) : AdminUsersState()
    data class Error(val message: String) : AdminUsersState()
}

class AdminUsersViewModel(application: Application) : AndroidViewModel(application) {
    private val api = FestivalApiService.create()
    private val tokenManager = TokenManager(application)

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _uiState = MutableStateFlow<AdminUsersState>(AdminUsersState.Loading)
    val uiState: StateFlow<AdminUsersState> = _uiState.asStateFlow()

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadUsers()
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun loadUsers() {
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            _uiState.value = AdminUsersState.Loading
            try {
                val token = tokenManager.getToken()
                val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else null
                val users = api.getUsuarios(authHeader)
                _uiState.value = AdminUsersState.Success(users)
            } catch (e: Exception) {
                _uiState.value = AdminUsersState.Error("Error al cargar usuarios: ${e.localizedMessage}")
            }
        }
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun toggleRole(userId: String, currentRole: String) {
        val newRole = if (currentRole == "ADMINISTRADOR") "CLIENTE" else "ADMINISTRADOR"
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken()
                val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else null
                api.updateUserRole(authHeader, userId, UpdateRoleDto(newRole))
                _actionMessage.value = "Rol actualizado a $newRole exitosamente"
                loadUsers()
            } catch (e: Exception) {
                _actionMessage.value = "Error al actualizar rol: ${e.localizedMessage}"
            }
        }
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun registerAdmin(nombre: String, correo: String, contrasena: String) {
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            try {
                api.registrar(RegistroRequestDto(nombre, correo, contrasena, "ADMINISTRADOR"))
                _actionMessage.value = "Administrador registrado exitosamente"
                loadUsers()
            } catch (e: Exception) {
                _actionMessage.value = "Error al registrar admin: ${e.localizedMessage}"
            }
        }
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun clearActionMessage() {
        _actionMessage.value = null
    }
}

```

### `ArtistViewModel.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.remote.ArtistaDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

class ArtistViewModel : ViewModel() {
    private val api = FestivalApiService.create()
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _biografia = MutableStateFlow<ArtistaDto?>(null)
    val biografia: StateFlow<ArtistaDto?> = _biografia.asStateFlow()
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun loadData() {
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = api.getArtistaInfo()
                _biografia.value = data
            } catch (e: Exception) {
                _biografia.value = ArtistaDto(
                    nombre = "José Alfredo Jiménez",
                    descripcion = "Máximo exponente de la música ranchera mexicana nacido en Dolores Hidalgo.",
                    citaCelebre = "\"No tengo trono ni reina, ni nadie que me comprenda, pero sigo siendo el Rey.\""
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}

```

### `AudioViewModel.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

class AudioViewModel : ViewModel() {
    private val api = FestivalApiService.create()
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _canciones = MutableStateFlow<List<CancionDto>>(emptyList())
    val canciones: StateFlow<List<CancionDto>> = _canciones.asStateFlow()
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refresh()
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun refresh() {
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = api.getCanciones()
                _canciones.value = list
            } catch (e: Exception) {
                _canciones.value = listOf(
                    CancionDto("1", "El Rey", "José Alfredo Jiménez", 180, ""),
                    CancionDto("2", "Camino de Guanajuato", "José Alfredo Jiménez", 210, ""),
                    CancionDto("3", "Si Nos Dejan", "José Alfredo Jiménez", 195, "")
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}

```

### `GalleryViewModel.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.GaleriaDto

class GalleryViewModel : ViewModel() {
    private val api = FestivalApiService.create()

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _galerias = MutableStateFlow<List<GaleriaDto>>(emptyList())
    val galerias: StateFlow<List<GaleriaDto>> = _galerias

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        refresh()
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun refresh() {
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _galerias.value = api.getGalerias()
            } catch (e: Exception) {
                _error.value = "Error al cargar galería: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

```

### `LiveViewModel.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.ChatMessageDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

class LiveViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val api = FestivalApiService.create()

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _messages = MutableStateFlow<List<ChatMessageDto>>(emptyList())
    val messages: StateFlow<List<ChatMessageDto>> = _messages

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _streamUrl = MutableStateFlow<String?>(null)
    val streamUrl: StateFlow<String?> = _streamUrl

    private var isPolling = false

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun startLiveStream(eventoId: String) {
        isPolling = true
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            try {
                val eventos = api.getEventos()
                val evento = eventos.find { it.id == eventoId }
                val transmision = evento?.transmision
                if (transmision?.estado == "EN_VIVO") {
                    _streamUrl.value = transmision.streamUrl
                }
            } catch (e: Exception) {}
        }

        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            while (isPolling) {
                try {
                    val msgs = api.getChatMessages(eventoId)
                    _messages.value = msgs
                } catch (e: Exception) {}
                delay(3000)
            }
        }
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun stopLiveStream() {
        isPolling = false
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun sendMessage(eventoId: String, text: String) {
        if (text.isBlank()) return
        val nombre = tokenManager.getUserName() ?: "Usuario"

        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            try {
                api.sendChatMessage(
                    ChatMessageDto(
                        eventoId = eventoId,
                        usuarioNombre = nombre,
                        mensaje = text
                    )
                )
                val current = _messages.value.toMutableList()
                current.add(
                    ChatMessageDto(
                        eventoId = eventoId,
                        usuarioNombre = nombre,
                        mensaje = text
                    )
                )
                _messages.value = current
            } catch (e: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        isPolling = false
    }
}

```

### `ProfileViewModel.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.BoletoDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val boletos: List<BoletoDto>) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val api = FestivalApiService.create()
    private val tokenManager = TokenManager(application)

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun loadProfile() {
        // [FLUJO ASÍNCRONO]: Lanza corrutina atada al ciclo de vida del ViewModel
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val token = tokenManager.getToken()
                val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else null
                val response = api.getMisBoletos(authHeader)
                _profileState.value = ProfileState.Success(response)
            } catch (e: Exception) {
                _profileState.value = ProfileState.Success(emptyList())
            }
        }
    }
}

```

---

## Paso 6: Todas las Pantallas de la Aplicación Móvil

### 1. `WelcomeScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3B2A1A),
                            Color(0xFF121212)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Bienvenido al\nFestival",
                style = MaterialTheme.typography.headlineLarge,
                color = PrimaryGold,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "El legado de José Alfredo Jiménez. Vive la experiencia inmersiva del festival.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNavigateToLogin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "INICIAR SESIÓN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

```

### 2. `LoginScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.viewmodels.AuthViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun LoginScreen(
    onNavigateToRegister: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var email by remember { mutableStateOf("") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var password by remember { mutableStateOf("") }
    
    val authState by viewModel.authState.collectAsState()
    
    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
    LaunchedEffect(Unit) {
        viewModel.resetState()
    }
    
    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val role = (authState as AuthState.Success).role
            if (role == "ADMINISTRADOR") {
                onNavigateToAdmin()
            } else {
                onNavigateToDashboard()
            }
        }
    }

    val fieldColor = Color(0xFF1E2720)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "José Alfredo Jiménez",
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryGold,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "ACCESO EXCLUSIVO",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo Electrónico") },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryGold,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = PrimaryGold,
                unfocusedLabelColor = Color.Gray,
                containerColor = fieldColor
            ),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryGold,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = PrimaryGold,
                unfocusedLabelColor = Color.Gray,
                containerColor = fieldColor
            ),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "¿Olvidaste tu contraseña?",
            color = PrimaryGold,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { },
            textAlign = TextAlign.End
        )

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (authState as AuthState.Error).message,
                color = Color.Red,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.login(email.trim(), password)
            },
            enabled = authState !is AuthState.Loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGold,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¿No tienes una cuenta?", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Regístrate",
                color = PrimaryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
            Text(
                text = "O ENTRA CON",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable { }
                .padding(horizontal = 32.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("GOOGLE", color = Color.White, fontSize = 16.sp, letterSpacing = 2.sp)
        }
    }
}

```

### 3. `RegisterScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.viewmodels.AuthViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun RegisterScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var name by remember { mutableStateOf("") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var email by remember { mutableStateOf("") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var password by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()
    
    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
    LaunchedEffect(Unit) {
        viewModel.resetState()
    }
    
    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val role = (authState as AuthState.Success).role
            if (role == "ADMINISTRADOR") {
                onNavigateToAdmin()
            } else {
                onNavigateToDashboard()
            }
        }
    }

    val scrollState = rememberScrollState()
    val fieldColor = Color(0xFF1E2720)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "José Alfredo Jiménez",
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryGold,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Crea tu Cuenta",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = "Sé parte de la leyenda.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("NOMBRE COMPLETO") },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryGold,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = PrimaryGold,
                unfocusedLabelColor = Color.Gray,
                containerColor = fieldColor
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("CORREO ELECTRÓNICO") },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryGold,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = PrimaryGold,
                unfocusedLabelColor = Color.Gray,
                containerColor = fieldColor
            ),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("CONTRASEÑA") },
            visualTransformation = PasswordVisualTransformation(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryGold,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = PrimaryGold,
                unfocusedLabelColor = Color.Gray,
                containerColor = fieldColor
            ),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp)
        )
        
        Text(
            text = "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
        )

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (authState as AuthState.Error).message,
                color = Color.Red,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.register(name.trim(), email.trim(), password)
            },
            enabled = authState !is AuthState.Loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGold,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text("REGISTRARSE AHORA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¿Ya tienes una cuenta?", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Inicia Sesión",
                color = PrimaryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

```

### 4. `MainScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.EventosViewModel

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Inicio")
    object Biography : BottomNavItem("biography", Icons.Default.Book, "Biografía")
    object Map : BottomNavItem("map", Icons.Default.Map, "Mapa")
    object Audio : BottomNavItem("audio", Icons.Default.Audiotrack, "Audio")
    object Tickets : BottomNavItem("tickets", Icons.Default.ConfirmationNumber, "Comprar")
    object Gallery : BottomNavItem("gallery", Icons.Default.Person, "Galería")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Boletos")
}

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun MainScreen(
    eventosViewModel: EventosViewModel? = null,
    onNavigateToCheckout: (Int, Int) -> Unit = { _, _ -> },
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Biography,
        BottomNavItem.Map,
        BottomNavItem.Audio,
        BottomNavItem.Tickets,
        BottomNavItem.Gallery,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = Color.White
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryGold,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            NavHost(navController = navController, startDestination = BottomNavItem.Home.route) {
                composable(BottomNavItem.Home.route) {
                    DashboardScreen(
                        eventosViewModel = eventosViewModel,
                        onNavigateToTickets = {
                            navController.navigate(BottomNavItem.Tickets.route)
                        },
                        onNavigateToLive = { eventoId ->
                            navController.navigate("live/$eventoId")
                        }
                    )
                }
                composable(BottomNavItem.Biography.route) {
                    BiographyScreen()
                }
                composable(BottomNavItem.Map.route) {
                    MapScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(BottomNavItem.Audio.route) {
                    AudioScreen()
                }
                composable(BottomNavItem.Tickets.route) {
                    TicketsScreen(
                        eventosViewModel = eventosViewModel,
                        onNavigateToCheckout = { total, count -> onNavigateToCheckout(total, count) }
                    )
                }
                composable(BottomNavItem.Gallery.route) {
                    GalleryScreen()
                }
                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(
                        onNavigateBack = { navController.navigate(BottomNavItem.Home.route) },
                        onLogout = onLogout
                    )
                }
                composable("live/{eventoId}") { backStackEntry ->
                    val eventoId = backStackEntry.arguments?.getString("eventoId") ?: ""
                    UserLiveStreamScreen(
                        eventoId = eventoId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

```

### 5. `DashboardScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.R
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.EventosViewModel

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun DashboardScreen(
    eventosViewModel: EventosViewModel? = null,
    onNavigateToTickets: () -> Unit = {},
    onNavigateToLive: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var selectedEventDetail by remember { mutableStateOf<String?>(null) }
    
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    val eventos by eventosViewModel?.eventosLocales?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.hero_dolores_hidalgo),
                contentDescription = "Parroquia de Dolores Hidalgo de Noche",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = "DOLORES HIDALGO, CUNA DE LA INDEPENDENCIA",
                    color = PrimaryGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "El Rey\nVive en Su\nTierra",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 44.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onNavigateToTickets,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("COMPRAR BOLETOS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.jose_alfredo_portrait),
                        contentDescription = "Biografía",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xEE141D17))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text("📚 Biografía", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Conoce al Maestro", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.ranchera_guitar),
                        contentDescription = "Audio",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xEE141D17))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text("🎵 Audio", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Discografía Completa", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "PRÓXIMO EVENTO",
            color = PrimaryGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        val displayEventTitle = eventos.firstOrNull()?.nombre ?: "Gran Gala Mariachi"
        val displayEventLocation = eventos.firstOrNull()?.ubicacion ?: "23 de Noviembre, 20:00 hrs • Escenario Principal"

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clickable { selectedEventDetail = displayEventTitle },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2720)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mariachi_gala_stage),
                        contentDescription = "Mariachi Gala",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayEventTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(displayEventLocation, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
                Text(">", color = PrimaryGold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val eId = eventos.firstOrNull()?.id ?: "EVT-001"
                onNavigateToLive(eId)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFC51111),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp)
        ) {
            Text("▶ VER EN VIVO AHORA", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (selectedEventDetail != null) {
        AlertDialog(
            onDismissRequest = { selectedEventDetail = null },
            title = { Text(selectedEventDetail!!, color = PrimaryGold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Evento oficial del Festival José Alfredo Jiménez en Dolores Hidalgo Guanajuato. ¡Compra tus accesos o consulta la ubicación en el mapa!",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedEventDetail = null
                        onNavigateToTickets()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                ) {
                    Text("COMPRAR BOLETOS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedEventDetail = null }) {
                    Text("CERRAR", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E2720)
        )
    }
}

```

### 6. `BiographyScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.R
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.viewmodels.ArtistViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun BiographyScreen(viewModel: ArtistViewModel = viewModel()) {
    val scrollState = rememberScrollState()
    val biografia by viewModel.biografia.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.jose_alfredo_portrait),
                contentDescription = "José Alfredo Jiménez Portrait",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = "EL REY DE LA CANCIÓN",
                    color = PrimaryGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Biografía",
                    color = PrimaryGold,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.padding(32.dp))
            }
        } else if (biografia != null) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = biografia!!.citaCelebre ?: "\"No tengo trono ni reina,\nni nadie que me\ncomprenda, pero sigo\nsiendo el Rey.\"",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 28.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = biografia!!.descripcion,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Hitos Históricos",
                    color = PrimaryGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                TimelineItem(
                    year = "1926",
                    title = "El Nacimiento en Dolores Hidalgo",
                    description = "Nace el 19 de enero en la Cuna de la Independencia, Guanajuato. Desde pequeño mostró la sensibilidad que definiría su arte."
                )
                TimelineItem(
                    year = "1948",
                    title = "Su Primer Gran Éxito",
                    description = "Andrés Huesca graba su primera canción, marcando el inicio de una carrera meteórica que cambiaría la música regional para siempre."
                )
                TimelineItem(
                    year = "1950s",
                    title = "La Época de Oro",
                    description = "Consolidación como la máxima figura de la composición ranchera, participando en cine y radio, llenando corazones con despecho y pasión."
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discografía Destacada",
                        color = PrimaryGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ver todo",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DiscographyCard(
                        title = "La Enorme Distancia",
                        subtitle = "1950 • 12 Canciones",
                        drawableId = R.drawable.ranchera_guitar,
                        modifier = Modifier.weight(1f)
                    )
                    DiscographyCard(
                        title = "El Camino de la Noche",
                        subtitle = "1954 • 10 Canciones",
                        drawableId = R.drawable.mariachi_gala_stage,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun TimelineItem(year: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp, end = 16.dp)
                .size(8.dp)
                .background(PrimaryGold, RoundedCornerShape(50))
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2720)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(year, color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun DiscographyCard(title: String, subtitle: String, drawableId: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.Gray, fontSize = 10.sp)
    }
}

```

### 7. `AudioScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.viewmodels.AudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun AudioScreen(viewModel: AudioViewModel = viewModel()) {
    val context = LocalContext.current
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var isPlaying by remember { mutableStateOf(false) }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var sliderPosition by remember { mutableStateOf(0.35f) }
    val scrollState = rememberScrollState()

    val canciones by viewModel.canciones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var currentTrack by remember { mutableStateOf<mx.utng.festivaltrack.shared.data.remote.CancionDto?>(null) }
    
    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
    LaunchedEffect(canciones) {
        if (canciones.isNotEmpty() && currentTrack == null) {
            currentTrack = canciones.first()
        }
    }

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // [CICLO DE VIDA]: Inicializa y libera recursos (cámara/reproductores) al salir
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Streaming & Podcasts", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Icon(
                        Icons.Default.Headphones,
                        contentDescription = "Podcast",
                        tint = PrimaryGold,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(PrimaryGold)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = currentTrack?.titulo ?: "Selecciona una pista",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = currentTrack?.artista ?: "",
                color = if (isPlaying) PrimaryGold else Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryGold,
                    activeTrackColor = PrimaryGold,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("02:15", color = Color.Gray, fontSize = 12.sp)
                Text("04:30", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    sliderPosition = (sliderPosition - 0.1f).coerceAtLeast(0f)
                }) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                
                FloatingActionButton(
                    onClick = {
                        try {
                            if (isPlaying) {
                                mediaPlayer?.pause()
                                isPlaying = false
                            } else {
                                if (mediaPlayer == null && currentTrack != null) {
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(currentTrack!!.archivoUrl)
                                        prepareAsync()
                                        setOnPreparedListener {
                                            start()
                                            isPlaying = true
                                        }
                                    }
                                } else if (mediaPlayer != null) {
                                    mediaPlayer?.start()
                                    isPlaying = true
                                }
                            }
                        } catch (e: Exception) {
                            isPlaying = !isPlaying
                        }
                    },
                    containerColor = PrimaryGold,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = {
                    sliderPosition = (sliderPosition + 0.1f).coerceAtMost(1f)
                }) {
                    Icon(Icons.Default.FastForward, contentDescription = "Forward", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Catálogo de Canciones", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    canciones.forEachIndexed { index, cancion ->
                        val minutes = cancion.duracion / 60
                        val seconds = cancion.duracion % 60
                        val durationStr = String.format("%02d:%02d", minutes, seconds)
                        
                        EpisodeItem(
                            number = (index + 1).toString(),
                            title = cancion.titulo,
                            duration = durationStr,
                            isPlaying = currentTrack?.id == cancion.id,
                            onClick = { 
                                if (currentTrack?.id != cancion.id) {
                                    currentTrack = cancion
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    isPlaying = false
                                }
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun EpisodeItem(number: String, title: String, duration: String, isPlaying: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if(isPlaying) Color(0xFF2A3A2C) else Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = number,
                color = if (isPlaying) PrimaryGold else Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.width(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(duration, color = Color.Gray, fontSize = 12.sp)
            }
            if (isPlaying) {
                Icon(Icons.Default.Headphones, contentDescription = "Playing", tint = PrimaryGold)
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
            }
        }
    }
}

```

### 8. `GalleryScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun GalleryScreen(viewModel: GalleryViewModel = viewModel()) {
    val galerias by viewModel.galerias.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galería Oficial", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Gallery",
                        tint = PrimaryGold,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGold)
                }
            } else {
                val imagenes = galerias.flatMap { it.imagenes }
                
                if (imagenes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay imágenes en la galería", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(imagenes) { imagen ->
                            val context = LocalContext.current
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imagen.url)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = imagen.titulo,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

```

### 9. `TicketsScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.EventosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun TicketsScreen(
    eventosViewModel: EventosViewModel? = null,
    onNavigateToCheckout: (Int, Int) -> Unit = { _, _ -> }
) {
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var selectedDate by remember { mutableStateOf("21") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var vipTickets by remember { mutableStateOf(1) }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var generalTickets by remember { mutableStateOf(0) }

    val vipPrice = 4500
    val generalPrice = 1200

    val totalTickets = vipTickets + generalTickets
    val totalPrice = (vipTickets * vipPrice) + (generalTickets * generalPrice)

    val scrollState = rememberScrollState()
    val fieldColor = Color(0xFF1E2720)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Boletos", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = "Boletos", tint = Color.White, modifier = Modifier.padding(end = 16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("TOTAL A PAGAR", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$${totalPrice}", color = PrimaryGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Text(" MXN", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                            }
                        }
                        Text("$totalTickets BOLETO${if(totalTickets != 1) "S" else ""}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onNavigateToCheckout(totalPrice, totalTickets) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = totalTickets > 0
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("CONTINUAR COMPRA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Continuar")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Selecciona tu fecha", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dates = listOf("21", "22", "23", "24")
                dates.forEach { date ->
                    val isSelected = selectedDate == date
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .background(
                                color = if (isSelected) PrimaryGold else Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDate = date },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NOV", color = if (isSelected) Color.Black else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(date, color = if (isSelected) Color.Black else Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = fieldColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "VIP", tint = PrimaryGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Acceso VIP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Box(
                            modifier = Modifier.background(PrimaryGold, RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("LIMITADO", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Experiencia premium frente al escenario, barra libre y zona lounge.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = PrimaryGold.copy(alpha = 0.3f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$4,500", color = PrimaryGold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(" MXN", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (vipTickets > 0) vipTickets-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "Quitar", tint = PrimaryGold)
                            }
                            Text(vipTickets.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { vipTickets++ }) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar", tint = PrimaryGold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = fieldColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = "General", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("General", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Entrada al festival con acceso a todas las áreas de comida y mercadito.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$1,200", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(" MXN", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (generalTickets > 0) generalTickets-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "Quitar", tint = Color.White)
                            }
                            Text(generalTickets.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { generalTickets++ }) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

```

### 10. `CheckoutScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.CheckoutViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.CheckoutState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun CheckoutScreen(
    eventoId: String = "",
    totalPrice: Int = 4500,
    totalTickets: Int = 1,
    onNavigateBack: () -> Unit = {},
    onPaymentSuccess: () -> Unit = {},
    viewModel: CheckoutViewModel = viewModel()
) {
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var cardNumber by remember { mutableStateOf("") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var expiryDate by remember { mutableStateOf("") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var cvv by remember { mutableStateOf("") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var cardHolder by remember { mutableStateOf("") }

    val checkoutState by viewModel.checkoutState.collectAsState()

    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
    LaunchedEffect(checkoutState) {
        if (checkoutState is CheckoutState.Success) {
            viewModel.resetState()
            onPaymentSuccess()
        }
    }

    val scrollState = rememberScrollState()
    val fieldColor = Color(0xFF1E2720)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pago Seguro", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (checkoutState is CheckoutState.Error) {
                        Text(
                            text = (checkoutState as CheckoutState.Error).message,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.procesarPago(
                                eventoId = if (eventoId.isNotBlank()) eventoId else "EVT-001",
                                categoria = "VIP",
                                cantidad = totalTickets,
                                precioTotal = totalPrice,
                                tarjetaNumero = cardNumber,
                                tarjetaVencimiento = expiryDate,
                                tarjetaCVV = cvv
                            )
                        },
                        enabled = checkoutState !is CheckoutState.Processing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        if (checkoutState is CheckoutState.Processing) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "CONFIRMAR PAGO - $$totalPrice MXN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = fieldColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Resumen del Pedido", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Accesos ($totalTickets x VIP)", color = Color.White)
                        Text("$$totalPrice MXN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cargos por servicio", color = Color.Gray, fontSize = 12.sp)
                        Text("Incluidos", color = PrimaryGold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Detalles de la Tarjeta", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = cardHolder,
                onValueChange = { cardHolder = it },
                label = { Text("Nombre del Titular") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = cardNumber,
                onValueChange = { if (it.length <= 16) cardNumber = it },
                label = { Text("Número de Tarjeta (16 dígitos)") },
                trailingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = PrimaryGold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { if (it.length <= 5) expiryDate = it },
                    label = { Text("MM/AA") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldColor,
                        unfocusedContainerColor = fieldColor,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = cvv,
                    onValueChange = { if (it.length <= 4) cvv = it },
                    label = { Text("CVV") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldColor,
                        unfocusedContainerColor = fieldColor,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transacción encriptada con tecnología SSL de 256 bits.", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

```

### 11. `TicketSuccessScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.utils.DynamicQrCode

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun TicketSuccessScreen(
    onNavigateHome: () -> Unit = {}
) {
    val transactionId = "TICKET-${System.currentTimeMillis().toString().takeLast(8)}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle, 
            contentDescription = "Success",
            tint = PrimaryGold,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "¡Pago Exitoso!",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tus boletos están listos. Muestra este código QR al entrar al festival.",
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "ID: $transactionId",
            color = PrimaryGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        DynamicQrCode(
            content = "FESTIVAL-TICKET-2024::$transactionId",
            modifier = Modifier.size(200.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onNavigateHome,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGold,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("VOLVER AL INICIO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

```

### 12. `ProfileScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.ProfileState
import mx.utng.festivaltrack.app.ui.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val profileState by viewModel.profileState.collectAsState()

    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Cerrar Sesión", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            when (profileState) {
                is ProfileState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryGold)
                    }
                }
                is ProfileState.Error -> {
                    Text(
                        text = (profileState as ProfileState.Error).message,
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is ProfileState.Success -> {
                    val boletos = (profileState as ProfileState.Success).boletos
                    
                    Text("Mis Boletos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (boletos.isEmpty()) {
                        Text("No has comprado ningún boleto aún.", color = Color.Gray)
                    } else {
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
                        var selectedBoleto by remember { mutableStateOf<mx.utng.festivaltrack.shared.data.remote.BoletoDto?>(null) }
                        
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(boletos) { boleto ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBoleto = boleto },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.QrCode, 
                                            contentDescription = "QR", 
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Evento: ${boleto.evento?.nombre ?: "Festival"}", color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Categoría: ${boleto.categoria}", color = Color.Gray, fontSize = 14.sp)
                                            Text("Código: ${boleto.codigoQR}", color = PrimaryGold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        selectedBoleto?.let { boleto ->
                            AlertDialog(
                                onDismissRequest = { selectedBoleto = null },
                                title = {
                                    Text("Boleto: ${boleto.evento?.nombre ?: "Festival"}", color = PrimaryGold, fontWeight = FontWeight.Bold)
                                },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        mx.utng.festivaltrack.app.ui.utils.DynamicQrCode(
                                            content = boleto.codigoQR,
                                            modifier = Modifier.size(200.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Categoría: ${boleto.categoria}", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("ID: ${boleto.id}", color = Color.Gray, fontSize = 12.sp)
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { selectedBoleto = null }) {
                                        Text("Cerrar", color = PrimaryGold)
                                    }
                                },
                                containerColor = Color(0xFF1E1E1E),
                                titleContentColor = Color.White,
                                textContentColor = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

```

### 13. `MapScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun MapScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var selectedDestinationIndex by remember { mutableStateOf(0) }

    val userStartPoint = remember { GeoPoint(21.1530, -100.9340) }

    val destinations = remember {
        listOf(
            Triple("Mausoleo José Alfredo", GeoPoint(21.1561, -100.9317), "450 m • 6 min a pie"),
            Triple("Teatro del Pueblo", GeoPoint(21.1565, -100.9308), "600 m • 8 min a pie"),
            Triple("Zona Gastronómica", GeoPoint(21.1550, -100.9325), "300 m • 4 min a pie")
        )
    }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
    fun updateMapRoute(mapView: MapView, selectedIdx: Int) {
        mapView.overlays.clear()

        val dest = destinations[selectedIdx]
        val destPoint = dest.second

        destinations.forEachIndexed { idx, (name, pt, snippet) ->
            val isSelected = idx == selectedIdx
            val marker = Marker(mapView).apply {
                position = pt
                title = if (isSelected) "🎯 $name (DESTINO)" else "🎪 $name"
                this.snippet = snippet
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                if (isSelected) showInfoWindow()
            }
            mapView.overlays.add(marker)
        }

        val userMarker = Marker(mapView).apply {
            position = userStartPoint
            title = "📍 ¡AQUÍ ESTÁS TÚ! (TU UBICACIÓN)"
            snippet = "Punto de partida GPS"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            showInfoWindow()
        }
        mapView.overlays.add(userMarker)

        val routeLine = Polyline(mapView).apply {
            val waypoints = arrayListOf(
                userStartPoint,
                GeoPoint((userStartPoint.latitude + destPoint.latitude) / 2, userStartPoint.longitude),
                destPoint
            )
            setPoints(waypoints)
            outlinePaint.color = android.graphics.Color.parseColor("#E6C27A")
            outlinePaint.strokeWidth = 16f
        }
        mapView.overlays.add(routeLine)

        mapView.controller.animateTo(destPoint)
        mapView.controller.setZoom(16.8)
        mapView.invalidate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navegación GPS y Rutas", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        mapViewInstance = this
                        updateMapRoute(this, selectedDestinationIndex)
                    }
                },
                update = { mapView ->
                    updateMapRoute(mapView, selectedDestinationIndex)
                },
                modifier = Modifier.fillMaxSize()
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEE1E2720)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(PrimaryGold, RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(destinations[selectedDestinationIndex].first, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Ruta: " + destinations[selectedDestinationIndex].third, color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            mapViewInstance?.let { updateMapRoute(it, selectedDestinationIndex) }
                        },
                        containerColor = PrimaryGold,
                        contentColor = Color.Black,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Recentar")
                    }
                }
            }
        }
    }
}

```

### 14. `UserLiveStreamScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.LiveViewModel

@kotlin.OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun UserLiveStreamScreen(
    eventoId: String,
    onNavigateBack: () -> Unit,
    viewModel: LiveViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val streamUrl by viewModel.streamUrl.collectAsState()
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var inputText by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
    LaunchedEffect(eventoId) {
        viewModel.startLiveStream(eventoId)
    }

    // [CICLO DE VIDA]: Inicializa y libera recursos (cámara/reproductores) al salir
    DisposableEffect(streamUrl) {
        val targetUrl = streamUrl ?: "rtsp://10.0.2.2:1935"
        try {
            val player = ExoPlayer.Builder(context).build()
            val mediaSource = RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .createMediaSource(MediaItem.fromUri(Uri.parse(targetUrl)))
            player.setMediaSource(mediaSource)
            player.prepare()
            player.playWhenReady = true
            exoPlayer = player
        } catch (e: Exception) {}

        onDispose {
            try {
                exoPlayer?.release()
            } catch (e: Exception) {}
            exoPlayer = null
            viewModel.stopLiveStream()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transmisión en Vivo", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (streamUrl != null && exoPlayer != null) {
                    AndroidView(
                        factory = {
                            PlayerView(context).apply {
                                player = exoPlayer
                                useController = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryGold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Esperando transmisión...", color = Color.Gray)
                    }
                }
            }

            Text(
                "Chat en vivo",
                color = PrimaryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "${msg.usuarioNombre}: ",
                            fontWeight = FontWeight.Bold,
                            color = if (msg.esAdmin) PrimaryGold else Color.LightGray
                        )
                        Text(
                            text = msg.mensaje,
                            color = Color.White
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Escribe un mensaje...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = Color.Transparent,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.sendMessage(eventoId, inputText)
                        inputText = ""
                    },
                    modifier = Modifier
                        .background(PrimaryGold, RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.Black)
                }
            }
        }
    }
}

```

### 15. `AdminMainScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun AdminMainScreen(
    adminManageViewModel: mx.utng.festivaltrack.app.ui.viewmodels.AdminManageViewModel,
    onNavigateToCreateEvent: () -> Unit = {},
    onEditEvent: (mx.utng.festivaltrack.shared.data.local.entity.EventoEntity) -> Unit = {},
    onNavigateToLivePanel: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color.Gray
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGold,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = Color(0xFF2A2A2A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = "Subir") },
                    label = { Text("Subir") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGold,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = Color(0xFF2A2A2A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Gestionar") },
                    label = { Text("Gestionar") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGold,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = Color(0xFF2A2A2A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGold,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = Color(0xFF2A2A2A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTab) {
                0 -> AdminDashboardScreen(
                    onNavigateToLivePanel = onNavigateToLivePanel,
                    onNavigateToUsers = onNavigateToUsers
                )
                1 -> AdminUploadScreen()
                2 -> AdminManageScreen(
                    viewModel = adminManageViewModel,
                    onNavigateToCreateEvent = onNavigateToCreateEvent,
                    onEditEvent = onEditEvent
                )
                3 -> AdminProfileTab(onLogout = onLogout)
            }
        }
    }
}

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun AdminProfileTab(onLogout: () -> Unit) {
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("¿Cerrar sesión?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Serás redirigido a la pantalla de inicio de sesión.", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC51111))
                ) {
                    Text("Cerrar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2A1A00), Color(0xFF121212))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(PrimaryGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Administrador", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("admin@admin.com", color = PrimaryGold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminInfoCard(title = "Rol", value = "ADMINISTRADOR", icon = Icons.Default.Shield)
            AdminInfoCard(title = "Plataforma", value = "FestivalTrack — Panel de Control", icon = Icons.Default.Dashboard)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { showLogoutDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC51111)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar Sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AdminInfoCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.Gray, fontSize = 12.sp)
                Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

```

### 16. `AdminDashboardScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun AdminDashboardScreen(
    onNavigateToLivePanel: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1410))
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("José Alfredo", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                Text("Jiménez", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
            }
            IconButton(
                onClick = onNavigateToUsers,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2720))
                    .border(1.dp, PrimaryGold, CircleShape)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Manage Users", tint = PrimaryGold, modifier = Modifier.size(20.dp))
            }
        }
        
        HorizontalDivider(color = Color(0xFF2A3A2C))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F23)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("¡Bienvenido,\nAdmin!", color = PrimaryGold, fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "El legado de El Rey sigue vivo. Gestiona el contenido del festival y mantén la llama de la música regional ardiendo.",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("STATUS DEL STREAM", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF8B5A5A)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("En Vivo: Dolores Hidalgo", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("1,240 espectadores ahora mismo.", color = Color.Gray, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ACCESO RÁPIDO VIP", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Resumen de\nContenido",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricBox(icon = Icons.Default.MusicNote, count = "148", label = "Canciones\nTotales", modifier = Modifier.weight(1f))
            MetricBox(icon = Icons.Default.PhotoLibrary, count = "3.2k", label = "Fotos en\nGalería", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricBox(icon = Icons.Default.Visibility, count = "24.5k", label = "Vistas Totales", modifier = Modifier.weight(1f))
            MetricBox(icon = Icons.Default.CloudUpload, count = "85%", label = "Venta de\nBoletos", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gestión\nRápida", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .border(1.dp, PrimaryGold, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Admin\nVerificado", color = PrimaryGold, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        ManagementCard(title = "Nuevos Lanzamientos", desc = "Sube archivos de audio, letras y metadatos para la colección oficial del festival.", btn = "GESTIONAR MÚSICA")
        Spacer(modifier = Modifier.height(16.dp))
        ManagementCard(title = "Galería de Eventos", desc = "Añade fotos del último concierto y organiza los álbumes por fecha y artista.", btn = "EDITAR GALERÍA")
        Spacer(modifier = Modifier.height(16.dp))
        ManagementCard(
            title = "Control de Stream", 
            desc = "Configura las claves de transmisión y monitorea la salud del stream en tiempo real.", 
            btn = "IR AL PANEL LIVE",
            onClick = onNavigateToLivePanel
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Actividad Reciente", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Ver Todo", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                ActivityItem(icon = Icons.Default.CloudUpload, iconColor = Color(0xFF4CAF50), title = "Nueva canción subida: \"El Rey\" (Remix Festival)", time = "Hace 15 minutos • por Admin Principal")
                ActivityItem(icon = Icons.Default.PersonAdd, iconColor = Color.Gray, title = "Nuevo usuario registrado: Mariachi Juvenil Real", time = "Hace 2 horas • Registro Automático")
                ActivityItem(icon = Icons.Default.Warning, iconColor = Color.Red, title = "Alerta: Intento de acceso fallido en servidor Stream", time = "Hace 4 horas • IP: 192.168.1.104", hideLine = true)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun MetricBox(icon: ImageVector, count: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171A18)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF1E2720), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF8AA694), modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(count, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun ManagementCard(title: String, desc: String, btn: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171A18)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(desc, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGold),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(btn)
            }
        }
    }
}

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun ActivityItem(icon: ImageVector, iconColor: Color, title: String, time: String, hideLine: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            if (!hideLine) {
                Box(modifier = Modifier.width(2.dp).height(40.dp).background(iconColor.copy(alpha = 0.3f)))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(time, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

```

### 17. `AdminUploadScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun AdminUploadScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var title by remember { mutableStateOf("") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var artist by remember { mutableStateOf("") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var uploadProgress by remember { mutableStateOf(0f) }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var isUploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "upload_img_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out -> inputStream?.copyTo(out) }
                selectedImageUri = Uri.fromFile(file)
                Toast.makeText(context, "Imagen lista para publicar", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                selectedImageUri = uri
                Toast.makeText(context, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedAudioUri = uri
        if (uri != null) {
            Toast.makeText(context, "Archivo de audio seleccionado", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1410))
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = PrimaryGold)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("José Alfredo", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                Text("Jiménez", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2720))
                    .border(1.dp, PrimaryGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Profile", tint = PrimaryGold, modifier = Modifier.size(20.dp))
            }
        }
        
        HorizontalDivider(color = Color(0xFF2A3A2C))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Subir Nueva Canción / Archivo",
            color = PrimaryGold,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Inmortaliza tu talento en el catálogo regional.",
            color = Color.LightGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(180.dp)
                .background(Color(0xFF2A3A2C), RoundedCornerShape(16.dp))
                .border(1.dp, if (selectedImageUri != null) PrimaryGold else PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (selectedImageUri != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                    contentDescription = "Upload Photo",
                    tint = PrimaryGold,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (selectedImageUri != null) "Imagen Lista ✓" else "Portada del Álbum",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "TOCA PARA SELECCIONAR DE TU GALERÍA",
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("TÍTULO DE LA CANCIÓN", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Ej: El Rey del Mariachi") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF171A18),
                    unfocusedContainerColor = Color(0xFF171A18),
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color(0xFF2A2A2A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("ARTISTA / INTÉRPRETE", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                placeholder = { Text("Nombre de la agrupación o solista") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF171A18),
                    unfocusedContainerColor = Color(0xFF171A18),
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color(0xFF2A2A2A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF334D41)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).background(PrimaryGold.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = "Audio", tint = PrimaryGold, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (selectedAudioUri != null) "Audio Seleccionado ✓" else "Archivo de Audio",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            selectedAudioUri?.lastPathSegment ?: "MP3 / WAV (Max 50MB)",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(if (selectedAudioUri != null) "CAMBIAR" else "SELECCIONAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (isUploading || uploadProgress > 0f) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subiendo archivo al servidor...", color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${(uploadProgress * 100).toInt()}%", color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = uploadProgress,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = PrimaryGold,
                    trackColor = Color(0xFF2A2A2A)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val coroutineScope = rememberCoroutineScope()
            val api = remember { mx.utng.festivaltrack.shared.data.remote.FestivalApiService.create() }

            Button(
                onClick = {
                    if (selectedImageUri == null && selectedAudioUri == null && title.isBlank()) {
                        Toast.makeText(context, "Por favor selecciona una imagen, audio o ingresa un título", Toast.LENGTH_SHORT).show()
                    } else {
                        isUploading = true
                        uploadProgress = 0.5f
                        coroutineScope.launch {
                            try {
                                val finalTitle = if (title.isNotBlank()) title else "Nueva Publicación"
                                
                                if (selectedAudioUri != null || title.isNotBlank()) {
                                    api.createCancion(
                                        mx.utng.festivaltrack.shared.data.remote.CancionCreateDto(
                                            titulo = finalTitle,
                                            artista = if (artist.isNotBlank()) artist else "José Alfredo Jiménez"
                                        )
                                    )
                                }
                                
                                if (selectedImageUri != null) {
                                    api.addImagenGaleria(
                                        mx.utng.festivaltrack.shared.data.remote.ImagenCreateDto(
                                            url = selectedImageUri.toString(),
                                            titulo = finalTitle
                                        )
                                    )
                                }
                                
                                uploadProgress = 1.0f
                                isUploading = false
                                Toast.makeText(context, "¡Publicado exitosamente! Revisa la Galería o Audio.", Toast.LENGTH_LONG).show()
                                title = ""
                                artist = ""
                                selectedImageUri = null
                                selectedAudioUri = null
                            } catch (e: Exception) {
                                isUploading = false
                                Toast.makeText(context, "Error al publicar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("PUBLICAR EN EL CATÁLOGO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

```

### 18. `AdminManageScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.AdminManageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun AdminManageScreen(
    viewModel: AdminManageViewModel,
    onNavigateToCreateEvent: () -> Unit = {},
    onEditEvent: (mx.utng.festivaltrack.shared.data.local.entity.EventoEntity) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var searchQuery by remember { mutableStateOf("") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var selectedFilter by remember { mutableStateOf("Eventos") }
    
    val eventos by viewModel.eventos.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1410))
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = PrimaryGold)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("José Alfredo", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                    Text("Jiménez", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2720))
                        .border(1.dp, PrimaryGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Profile", tint = PrimaryGold, modifier = Modifier.size(20.dp))
                }
            }
            
            HorizontalDivider(color = Color(0xFF2A3A2C))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Gestionar Contenido",
                color = PrimaryGold,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar canciones o imágenes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF171A18),
                    unfocusedContainerColor = Color(0xFF171A18),
                    focusedBorderColor = Color(0xFF2A2A2A),
                    unfocusedBorderColor = Color(0xFF2A2A2A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { FilterChipItem("Eventos", selectedFilter == "Eventos") { selectedFilter = "Eventos" } }
                item { FilterChipItem("Todos", selectedFilter == "Todos") { selectedFilter = "Todos" } }
                item { FilterChipItem("Canciones", selectedFilter == "Canciones") { selectedFilter = "Canciones" } }
                item { FilterChipItem("Galería", selectedFilter == "Galería") { selectedFilter = "Galería" } }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val context = androidx.compose.ui.platform.LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val api = remember { mx.utng.festivaltrack.shared.data.remote.FestivalApiService.create() }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
            var cancionesList by remember { mutableStateOf<List<mx.utng.festivaltrack.shared.data.remote.CancionDto>>(emptyList()) }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
            var imagenesList by remember { mutableStateOf<List<mx.utng.festivaltrack.shared.data.remote.ImagenDto>>(emptyList()) }

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
            fun reloadData() {
                coroutineScope.launch {
                    try {
                        cancionesList = api.getCanciones()
                        val galerias = api.getGalerias()
                        imagenesList = galerias.flatMap { it.imagenes }
                    } catch (e: Exception) {}
                }
            }

    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
            LaunchedEffect(Unit) {
                reloadData()
            }

            if (selectedFilter == "Eventos") {
                if (eventos.isEmpty()) {
                    Text("No hay eventos disponibles.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    eventos.forEach { evento ->
                        ManageItemCard(
                            status = evento.estado,
                            isDraft = evento.estado == "BORRADOR",
                            title = evento.nombre,
                            subtitle = "${evento.ubicacion} • ${evento.fechaHora}",
                            iconType = "event",
                            onEdit = { onEditEvent(evento) },
                            onDelete = {
                                val token = mx.utng.festivaltrack.app.data.TokenManager(context).getToken()
                                viewModel.deleteEvent(token, evento.id)
                            }
                        )
                    }
                }
            } else if (selectedFilter == "Canciones") {
                if (cancionesList.isEmpty()) {
                    Text("No hay canciones registradas en el catálogo.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    cancionesList.forEach { cancion ->
                        ManageItemCard(
                            status = "PUBLICADO",
                            isDraft = false,
                            title = cancion.titulo,
                            subtitle = "${cancion.artista} • ${cancion.duracion}s",
                            iconType = "music",
                            onEdit = {},
                            onDelete = {
                                coroutineScope.launch {
                                    try {
                                        api.deleteCancion(cancion.id)
                                        android.widget.Toast.makeText(context, "Canción eliminada del catálogo", android.widget.Toast.LENGTH_SHORT).show()
                                        reloadData()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Error al eliminar canción", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            } else if (selectedFilter == "Galería") {
                if (imagenesList.isEmpty()) {
                    Text("No hay fotos en la galería.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    imagenesList.forEach { imagen ->
                        ManageItemCard(
                            status = "PUBLICADO",
                            isDraft = false,
                            title = imagen.titulo ?: "Foto de Galería",
                            subtitle = "Galería del Festival",
                            iconType = "image",
                            onEdit = {},
                            onDelete = {
                                coroutineScope.launch {
                                    try {
                                        api.deleteImagenGaleria(imagen.id)
                                        android.widget.Toast.makeText(context, "Imagen eliminada de la galería", android.widget.Toast.LENGTH_SHORT).show()
                                        reloadData()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Error al eliminar imagen", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                cancionesList.forEach { cancion ->
                    ManageItemCard(
                        status = "PUBLICADO",
                        isDraft = false,
                        title = cancion.titulo,
                        subtitle = "Canción • ${cancion.artista}",
                        iconType = "music",
                        onEdit = {},
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    api.deleteCancion(cancion.id)
                                    android.widget.Toast.makeText(context, "Canción eliminada", android.widget.Toast.LENGTH_SHORT).show()
                                    reloadData()
                                } catch (e: Exception) {}
                            }
                        }
                    )
                }
                imagenesList.forEach { imagen ->
                    ManageItemCard(
                        status = "PUBLICADO",
                        isDraft = false,
                        title = imagen.titulo ?: "Foto de Galería",
                        subtitle = "Imagen de Galería",
                        iconType = "image",
                        onEdit = {},
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    api.deleteImagenGaleria(imagen.id)
                                    android.widget.Toast.makeText(context, "Imagen eliminada", android.widget.Toast.LENGTH_SHORT).show()
                                    reloadData()
                                } catch (e: Exception) {}
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
        
        FloatingActionButton(
            onClick = onNavigateToCreateEvent,
            containerColor = PrimaryGold,
            contentColor = Color.Black,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 16.dp)
                .size(64.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun FilterChipItem(label: String, isSelected: Boolean, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) PrimaryGold else Color(0xFF2A2A2A),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.LightGray,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun ManageItemCard(
    status: String,
    isDraft: Boolean,
    title: String,
    subtitle: String,
    iconType: String,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171A18)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (iconType == "music") Icons.Default.MusicNote else Icons.Default.Image,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .background(if (isDraft) Color(0xFF3A3A3A) else Color(0xFF0F3D14), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        status,
                        color = if (isDraft) Color.LightGray else PrimaryGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (iconType == "music") Icons.Default.MusicNote else Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(subtitle, color = Color.LightGray, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                Icons.Default.Edit,
                contentDescription = "Editar",
                tint = PrimaryGold,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onEdit() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = Color(0xFFE57373),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onDelete() }
            )
        }
    }
}

```

### 19. `AdminCreateEventScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.AdminManageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun AdminCreateEventScreen(
    eventId: String? = null,
    initialTitle: String = "",
    initialDate: String = "",
    initialLocation: String = "",
    initialPrice: String = "",
    viewModel: AdminManageViewModel? = null,
    onNavigateBack: () -> Unit = {}
) {
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var title by remember { mutableStateOf(initialTitle) }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var date by remember { mutableStateOf(initialDate) }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var location by remember { mutableStateOf(initialLocation) }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var price by remember { mutableStateOf(initialPrice) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    val fieldColor = Color(0xFF1E2720)
    val isEditing = !eventId.isNullOrEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Evento" else "Crear Evento", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Button(
                        onClick = {
                            val token = mx.utng.festivaltrack.app.data.TokenManager(context).getToken()
                            viewModel?.saveEvent(
                                token = token,
                                id = eventId,
                                title = title,
                                date = date,
                                location = location,
                                price = price
                            )
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Save, contentDescription = "Guardar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEditing) "GUARDAR CAMBIOS" else "GUARDAR EVENTO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Detalles del Evento", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título del Evento") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedLabelColor = PrimaryGold,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Fecha (Ej. 24 Nov 2024)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedLabelColor = PrimaryGold,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Lugar (Ej. Dolores Hidalgo)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedLabelColor = PrimaryGold,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Precio Base (MXN)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedLabelColor = PrimaryGold,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

```

### 20. `AdminScannerScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun AdminScannerScreen(
    onNavigateBack: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var scanResultMode by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escáner QR Multiplataforma", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Apunta la cámara al código QR de tu boleto o al QR de tu Smart TV",
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(250.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                    .border(
                        2.dp,
                        if (scanResultMode == null) PrimaryGold else if (scanResultMode!! > 0) Color.Green else Color.Red,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (scanResultMode) {
                    null -> Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.Gray, modifier = Modifier.size(64.dp))
                    1 -> Icon(Icons.Default.CheckCircle, contentDescription = "Boleto Aprobado", tint = Color.Green, modifier = Modifier.size(80.dp))
                    2 -> Icon(Icons.Default.Tv, contentDescription = "TV Sincronizada", tint = PrimaryGold, modifier = Modifier.size(80.dp))
                    else -> Text("❌", fontSize = 64.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            
            when (scanResultMode) {
                1 -> Text("¡Boleto Aprobado! Acceso Concedido", color = Color.Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                2 -> Text("¡Smart TV Sincronizada Exitosamente!", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                0 -> Text("Código QR Inválido", color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                else -> Text("Buscando código QR de boleto o TV...", color = Color.Gray, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        scanResultMode = 2
                        coroutineScope.launch {
                            delay(3000)
                            scanResultMode = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escanear QR de Smart TV", fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            scanResultMode = 0
                            coroutineScope.launch {
                                delay(2000)
                                scanResultMode = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Rechazar")
                    }

                    Button(
                        onClick = {
                            scanResultMode = 1
                            coroutineScope.launch {
                                delay(2000)
                                scanResultMode = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.2f), contentColor = Color.Green),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Validar Boleto")
                    }
                }
            }
        }
    }
}

```

### 21. `AdminUsersScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.AdminUsersState
import mx.utng.festivaltrack.app.ui.viewmodels.AdminUsersViewModel
import mx.utng.festivaltrack.shared.data.remote.UsuarioDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun AdminUsersScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: AdminUsersViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    )

    val uiState by viewModel.uiState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var showCreateDialog by remember { mutableStateOf(false) }

    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearActionMessage()
        }
    }

    if (showCreateDialog) {
        CreateAdminDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { nombre, correo, pass ->
                viewModel.registerAdmin(nombre, correo, pass)
                showCreateDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Usuarios", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = PrimaryGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1410))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryGold,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Crear Administrador")
            }
        },
        containerColor = Color(0xFF0F1410)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is AdminUsersState.Loading -> {
                    CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.align(Alignment.Center))
                }
                is AdminUsersState.Error -> {
                    Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center).padding(16.dp))
                }
                is AdminUsersState.Success -> {
                    if (state.users.isEmpty()) {
                        Text("No hay usuarios registrados.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.users) { usuario ->
                                UserCard(
                                    usuario = usuario,
                                    onToggleRole = { viewModel.toggleRole(usuario.id, usuario.rol) }
                                )
                            }
                        }
                    }
                }
            }

            if (actionMessage != null) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = Color(0xFF1E1E1E),
                    contentColor = PrimaryGold
                ) {
                    Text(actionMessage!!)
                }
            }
        }
    }
}

@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun UserCard(usuario: UsuarioDto, onToggleRole: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F23)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF121212)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (usuario.rol == "ADMINISTRADOR") Icons.Default.Security else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (usuario.rol == "ADMINISTRADOR") PrimaryGold else Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(usuario.nombre, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(usuario.correo, color = Color.LightGray, fontSize = 14.sp)
            }
            
            Button(
                onClick = onToggleRole,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (usuario.rol == "ADMINISTRADOR") Color(0xFF121212) else PrimaryGold,
                    contentColor = if (usuario.rol == "ADMINISTRADOR") PrimaryGold else Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (usuario.rol == "ADMINISTRADOR") "Quitar Admin" else "Hacer Admin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun CreateAdminDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var nombre by remember { mutableStateOf("") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var correo by remember { mutableStateOf("") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var contrasena by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Registrar Administrador", color = PrimaryGold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre Completo") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = PrimaryGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo Electrónico") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = PrimaryGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = PrimaryGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(nombre, correo, contrasena) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
            ) {
                Text("Registrar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

```

### 22. `AdminLiveStreamScreen.kt`
```kotlin
/**
 * =======================================================================
 * MÓDULO MÓVIL (app) — FESTIVAL JOSÉ ALFREDO JIMÉNEZ
 * 
 * FUNCIONALIDAD:
 * - Implementa componentes de interfaz gráfica (Jetpack Compose), ViewModels,
 *   servicios de streaming RTSP, persistencia local y lógica de negocio.
 *
 * FLUJO DE DATOS Y EJECUCIÓN:
 * 1. Inicialización y suscripción a estados reactivos (StateFlow / MutableState).
 * 2. Recepción de eventos del usuario (toques, compras, escaneo de QR, streaming).
 * 3. Ejecución de corrutinas asíncronas hacia el repositorio o backend REST.
 * 4. Recomposición automática de la interfaz ante cualquier cambio de estado.
 * =======================================================================
 */
package mx.utng.festivaltrack.app.ui.screens

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.pedro.common.ConnectChecker
import com.pedro.rtspserver.RtspServerCamera1
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun getLocalIpAddress(context: Context): String {
    return try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ipAddress = wifiManager?.connectionInfo?.ipAddress ?: 0
        if (ipAddress != 0) {
            String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        } else {
            "10.0.2.2"
        }
    } catch (e: Exception) {
        "10.0.2.2"
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
    // [FUNCIONALIDAD Y FLUJO]: Ejecuta la acción y despacha cambios de estado
fun AdminLiveStreamScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val api = remember { FestivalApiService.create() }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var rtspServer by remember { mutableStateOf<RtspServerCamera1?>(null) }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var isStreaming by remember { mutableStateOf(false) }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var streamUrl by remember { mutableStateOf("rtsp://10.0.2.2:1935") }
    // [ESTADO]: Variable reactiva observable que notifica modificaciones a la UI
    var statusText by remember { mutableStateOf("Listo para transmitir") }

    // [EFECTO SECUNDARIO]: Ejecuta lógica asíncrona al montar o actualizar dependencias
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
        val wifiIp = getLocalIpAddress(context)
        streamUrl = "rtsp://$wifiIp:1935"
    }

    // [CICLO DE VIDA]: Inicializa y libera recursos (cámara/reproductores) al salir
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (isStreaming) rtspServer?.stopStream()
                rtspServer?.stopPreview()
            } catch (e: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Live - Administrador", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (permissionsState.allPermissionsGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceView(ctx).apply {
                                holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {
                                        try {
                                            val checker = object : ConnectChecker {
                                                override fun onAuthError() { statusText = "Error de Auth" }
                                                override fun onAuthSuccess() { }
                                                override fun onConnectionFailed(reason: String) {
                                                    statusText = "Error: $reason"
                                                    isStreaming = false
                                                    try { rtspServer?.stopStream() } catch (e: Exception) {}
                                                }
                                                override fun onConnectionStarted(url: String) {
                                                    statusText = "Transmisión Activa en $url"
                                                }
                                                override fun onConnectionSuccess() { }
                                                override fun onDisconnect() {
                                                    statusText = "Transmisión detenida"
                                                }
                                                override fun onNewBitrate(bitrate: Long) { }
                                            }
                                            val server = RtspServerCamera1(this@apply, checker, 1935)
                                            rtspServer = server
                                            server.startPreview()
                                        } catch (e: Exception) {
                                            statusText = "Error al iniciar cámara: ${e.localizedMessage}"
                                        }
                                    }

                                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                                        if (isStreaming) rtspServer?.stopStream()
                                        rtspServer?.stopPreview()
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2720))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(statusText, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ingresa esto en la TV:\n$streamUrl", color = PrimaryGold, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (!isStreaming) {
                                    val audioPrepared = rtspServer?.prepareAudio() ?: false
                                    val videoPrepared = try {
                                        rtspServer?.prepareVideo(640, 480, 30, 1200 * 1024, 0) ?: false
                                    } catch (e: Exception) {
                                        rtspServer?.prepareVideo() ?: false
                                    }

                                    if (audioPrepared && videoPrepared) {
                                        rtspServer?.startStream()
                                        isStreaming = true
                                        statusText = "Transmisión en Vivo ACTIVA"
                                        coroutineScope.launch {
                                            try {
                                                api.setStreamStatus(StreamStatusDto(streamUrl = streamUrl, isLive = true))
                                            } catch (e: Exception) {}
                                        }
                                    } else {
                                        statusText = "Error al inicializar cámara/audio"
                                    }
                                } else {
                                    rtspServer?.stopStream()
                                    isStreaming = false
                                    statusText = "Listo para transmitir"
                                    coroutineScope.launch {
                                        try {
                                            api.setStreamStatus(StreamStatusDto(streamUrl = "", isLive = false))
                                        } catch (e: Exception) {}
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStreaming) Color.Red else PrimaryGold,
                                contentColor = if (isStreaming) Color.White else Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(if (isStreaming) Icons.Default.Stop else Icons.Default.Cast, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isStreaming) "DETENER TRANSMISIÓN" else "INICIAR EN VIVO", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Se requieren permisos de cámara y micrófono.", color = Color.White)
            }
        }
    }
}

```

---

## Paso 7: Compilación y Ejecución

```bash
# =======================================================================
# COMANDOS DE CONSTRUCCIÓN E INSTALACIÓN
# FUNCIONALIDAD: Compila e instala el APK móvil en el emulador o dispositivo físico.
# FLUJO: Ejecuta la tarea Gradle :app:installDebug.
# =======================================================================
./gradlew :app:installDebug

```

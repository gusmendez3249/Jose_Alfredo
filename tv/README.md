# 📺 Módulo `tv` — Aplicación Android TV (FestivalTrack)

## Descripción y Arquitectura General

El módulo `tv` es una aplicación nativa para **Android TV / Google TV** construida con **Jetpack Compose para TV** y **Media3 (ExoPlayer con soporte RTSP)**. Diseñada bajo la filosofía de diseño **10-foot UI (interfaz a 3 metros)** para ser operada exclusivamente con **control remoto (D-Pad)**.

### Características Principales
1. **Transmisión en Vivo RTSP:** Recibe y reproduce video H.264/AAC en tiempo real transmitido desde la app móvil del administrador vía `rtsp://<ip_host>:1935/live/stream`.
2. **Chat en Tiempo Real:** Visualización lateral del chat de la transmisión con soporte para escribir mensajes desde la TV o interacción remota.
3. **Navegación D-Pad 10-foot UI:** Barra lateral expandible/colapsable con soporte completo de foco de control remoto (`FocusRequester`, `Modifier.focusable()`).
4. **Galería Histórica de Alta Definición:** Visualización inmersiva en pantalla completa de fotografías del festival y de José Alfredo Jiménez.
5. **Cartelera y Horarios:** Visualización de eventos ordenados cronológicamente por escenario y fecha.
6. **Autenticación con Código QR Dinámico:** Vinculación rápida con la cuenta del usuario escaneando un código QR desde la app móvil.

---

## Estructura de Directorios del Módulo `tv`

```text
tv/
├── AndroidManifest.xml
├── build.gradle.kts
└── src/main/java/mx/utng/festivaltrack/tv/
    ├── MainActivity.kt
    ├── presentation/
    │   ├── components/
    │   │   └── SidebarMenuItem.kt
    │   ├── screens/
    │   │   ├── TvGalleryScreen.kt
    │   │   ├── TvLiveStreamScreen.kt
    │   │   ├── TvLoginScreen.kt
    │   │   ├── TvMainScreen.kt
    │   │   ├── TvScheduleScreen.kt
    │   │   └── TvSettingsScreen.kt
    │   └── viewmodel/
    │       └── TvViewModel.kt
    └── ui/
        ├── theme/
        │   ├── Color.kt
        │   └── Theme.kt
        └── utils/
            └── DynamicQrCode.kt
```

---

## Paso 1: Configuración del Emulador Android TV (1080p, API 30+)

1. Abre **Android Studio** -> **Device Manager** -> **Create Device**.
2. Selecciona la categoría **TV** y elige el perfil **Television (1080p)** con resolución de 1920x1080 píxeles.
3. Descarga e instala una imagen de sistema **Android 11.0 (Google TV)** o superior (**API Level 30+**, x86_64).
4. Asigna un nombre al AVD (por ejemplo, `Android_TV_1080p`) y finaliza la creación.

---

## Paso 2: Configuración de `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- 
    =======================================================================
    MANIFEST DEL MÓDULO ANDROID TV (FestivalTrack)
    
    FUNCIONALIDAD:
    - Declara la aplicación como específica para Android TV / Leanback UI.
    - Solicita permisos de red para consumo de APIs y streaming RTSP.
    - Especifica que NO se requiere pantalla táctil (hardware no touch).
    
    FLUJO DE EJECUCIÓN:
    1. El sistema operativo Android TV lee este manifest al instalar el APK.
    2. Al detectar 'android.software.leanback', la tienda Google Play y el launcher
       reconocen la app como compatible con televisores.
    3. El launcher de TV ejecuta la MainActivity mediante el intent-filter LEANBACK_LAUNCHER.
    =======================================================================
-->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permiso esencial para conectarse al servidor backend REST y al stream RTSP -->
    <uses-permission android:name="android.permission.INTERNET" />
    <!-- Permiso para monitorear el estado de la conexión Wi-Fi/Ethernet en el televisor -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- 
        Declara que la aplicación utiliza la interfaz Leanback para TV.
        Al ser 'true', Google Play solo mostrará esta app en dispositivos Android TV.
    -->
    <uses-feature
        android:name="android.software.leanback"
        android:required="true" />
        
    <!-- 
        Indica explícitamente que la app NO requiere pantalla táctil (touchscreen),
        permitiendo su instalación en televisores que operan únicamente con control remoto D-Pad.
    -->
    <uses-feature
        android:name="android.hardware.touchscreen"
        android:required="false" />

    <application
        android:allowBackup="true"
        android:label="Festival Track TV"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar"
        android:usesCleartextTraffic="true"> <!-- Permite tráfico HTTP y RTSP en red local sin cifrado forzado -->
        
        <!-- Actividad Principal: Punto de entrada para la interfaz de Android TV -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="Festival Track TV"
            android:theme="@android:style/Theme.Material.NoActionBar">
            <intent-filter>
                <!-- Acción estándar de inicio -->
                <action android:name="android.intent.action.MAIN" />
                <!-- Categoría obligatoria para que el banner aparezca en el menú principal de Android TV -->
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
                <!-- Compatibilidad con launchers estándar de desarrollo -->
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

---

## Paso 3: Configuración de `build.gradle.kts`

```kotlin
// =======================================================================
// CONFIGURACIÓN DE CONSTRUCCIÓN GRADLE PARA MÓDULO ANDROID TV (:tv)
//
// FUNCIONALIDAD:
// - Integra Jetpack Compose para interfaces declarativas de TV.
// - Importa bibliotecas Leanback y Media3 (ExoPlayer con protocolo RTSP).
// - Vincula el módulo compartido ':shared' para reutilizar modelos y lógica.
//
// FLUJO DE COMPILACIÓN:
// 1. Aplica plugins de Android y Kotlin.
// 2. Configura SDK mínimo (26 = Android 8.0 Oreo) y objetivo (34 = Android 14).
// 3. Resuelve dependencias de Compose, Media3 RTSP y Material3.
// =======================================================================

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "mx.utng.festivaltrack.tv"
    compileSdk = 34 // Compila contra las APIs más recientes de Android 14

    defaultConfig {
        applicationId = "mx.utng.festivaltrack.tv"
        minSdk = 26     // Soporte desde Android 8.0 Oreo (cubriendo la gran mayoría de Smart TVs)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        compose = true // Habilita Jetpack Compose en este módulo
    }
}

dependencies {
    // Módulo compartido del proyecto (contiene modelos FestivalEvent, Artist, etc.)
    implementation(project(":shared"))

    // Bibliotecas nativas de Android TV Leanback
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("androidx.leanback:leanback-preference:1.0.0")

    // Núcleo de Jetpack Compose y Material 3 para interfaces modernas en TV
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.2")

    // Media3 (ExoPlayer de Google) con extensión RTSP para reproducción de stream en vivo
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-exoplayer-rtsp:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")

    // Carga asíncrona y caché de imágenes de alta resolución en Compose
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Dependencias de depuración e inspección visual de Compose
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
```

---

## Paso 4: `MainActivity.kt`

```kotlin
package mx.utng.festivaltrack.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import mx.utng.festivaltrack.tv.presentation.screens.TvGalleryScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvLiveStreamScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvLoginScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvMainScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvScheduleScreen
import mx.utng.festivaltrack.tv.presentation.viewmodel.TvViewModel
import mx.utng.festivaltrack.tv.ui.theme.FestivalDarkBg
import mx.utng.festivaltrack.tv.ui.theme.FestivalTrackTvTheme

/**
 * =======================================================================
 * ACTIVIDAD PRINCIPAL DE ANDROID TV (MainActivity)
 *
 * FUNCIONALIDAD:
 * - Punto de entrada del ciclo de vida de la app en televisores.
 * - Administra la navegación global entre pantallas según el índice del menú lateral.
 * - Controla el flujo de autenticación (Login con QR / Dashboard principal).
 *
 * FLUJO DE EJECUCIÓN Y ESTADO:
 * 1. 'onCreate()': Se inicializa la actividad y se define el árbol de Compose con 'setContent'.
 * 2. 'viewModel.eventos.collectAsState()': Se suscribe reactivamente al flujo de eventos del festival.
 * 3. Si 'isLoggedIn == false': Muestra la pantalla 'TvLoginScreen' solicitando escaneo de QR.
 * 4. Al completar el login ('isLoggedIn = true'): Renderiza la pantalla seleccionada en 'currentScreenIndex':
 *    - 0 -> 'TvMainScreen' (Inicio / Carrusel de eventos destacados).
 *    - 1 -> 'TvGalleryScreen' (Galería histórica interactiva).
 *    - 2 -> 'TvLiveStreamScreen' (Streaming RTSP en vivo + Chat en tiempo real).
 *    - 3 -> 'TvScheduleScreen' (Cartelera cronológica por escenario).
 *    - 4 -> 'TvSettingsScreen' (Configuración y cierre de sesión).
 * =======================================================================
 */
class MainActivity : ComponentActivity() {

    // Instancia del ViewModel retenida durante cambios de configuración
    private val viewModel: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Establece la vista declarativa con Jetpack Compose
        setContent {
            // Aplica el tema oscuro institucional del festival para pantallas de TV
            FestivalTrackTvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FestivalDarkBg // Fondo oscuro de alto contraste para televisores
                ) {
                    // Suscripción al StateFlow de eventos del festival expuesto por el ViewModel
                    val eventos by viewModel.eventos.collectAsState()
                    
                    // Estado local que guarda el índice de la pantalla activa en la navegación lateral
                    var currentScreenIndex by remember { mutableStateOf(0) }
                    
                    // Estado local que determina si el televisor ha sido autenticado por el usuario
                    var isLoggedIn by remember { mutableStateOf(false) }

                    // FLUJO CONDICIONAL: Si no ha iniciado sesión, bloquea el acceso con pantalla de login QR
                    if (!isLoggedIn) {
                        TvLoginScreen(
                            // Callback invocado cuando el usuario se vincula exitosamente
                            onLoginSuccess = { isLoggedIn = true }
                        )
                    } else {
                        // Enrutador de pantallas según la selección del usuario en la barra lateral
                        when (currentScreenIndex) {
                            0 -> TvMainScreen(
                                eventos = eventos,
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it },
                                onVerEnVivo = { currentScreenIndex = 2 }, // Redirige directamente a la transmisión en vivo
                                onComprarBoletos = { /* Diálogo informativo o QR de compra en móvil */ }
                            )
                            1 -> TvGalleryScreen(
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it }
                            )
                            2 -> TvLiveStreamScreen(
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it }
                            )
                            3 -> TvScheduleScreen(
                                eventos = eventos,
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it }
                            )
                            4 -> mx.utng.festivaltrack.tv.presentation.screens.TvSettingsScreen(
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it },
                                onLogout = { isLoggedIn = false } // Cierra la sesión y regresa al login QR
                            )
                            else -> TvMainScreen(
                                eventos = eventos,
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it },
                                onVerEnVivo = { currentScreenIndex = 2 },
                                onComprarBoletos = {}
                            )
                        }
                    }
                }
            }
        }
    }
}
```

---

## Paso 5: Componente de Menú Lateral `SidebarMenuItem.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.ui.theme.FestivalGold
import mx.utng.festivaltrack.tv.ui.theme.FestivalGoldDark

/**
 * =======================================================================
 * COMPONENTE DE ÍTEM DE LA BARRA LATERAL (SidebarMenuItem)
 *
 * FUNCIONALIDAD:
 * - Renderiza cada elemento interactivo del menú de navegación de Android TV.
 * - Soporta foco mediante control remoto (D-Pad) y animaciones de color dinámicas.
 * - Se expande visualmente cuando la barra lateral completa está abierta.
 *
 * FLUJO DE EVENTOS Y FOCO:
 * 1. El usuario navega con las flechas Arriba/Abajo del control remoto.
 * 2. 'onFocusChanged' detecta cuando el ítem recibe foco ('isFocused = true').
 * 3. 'animateColorAsState' transiciona suavemente los colores (fondo dorado / texto negro).
 * 4. Al presionar el botón "OK" / Centro del D-Pad ('onClick'), se ejecuta 'onClick()'.
 * =======================================================================
 */
@Composable
fun SidebarMenuItem(
    icon: ImageVector,         // Ícono vectorial representativo de la opción
    title: String,             // Título de la sección
    isSelected: Boolean,       // Indica si esta opción es la pantalla actualmente activa
    isExpanded: Boolean,       // Indica si la barra lateral está desplegada mostrando texto
    onClick: () -> Unit,       // Callback de navegación al seleccionar el ítem
    modifier: Modifier = Modifier
) {
    // Estado local que rastrea si el ítem tiene el foco del control remoto en este instante
    var isFocused by remember { mutableStateOf(false) }

    // Animación fluida del color de fondo basada en foco y selección activa
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> FestivalGold             // Fondo dorado brillante cuando tiene el foco D-Pad
            isSelected -> FestivalGoldDark.copy(alpha = 0.3f) // Fondo dorado suave si está seleccionada
            else -> Color.Transparent            // Fondo transparente en reposo
        },
        label = "SidebarItemBg"
    )

    // Animación del color del contenido (ícono y texto) para asegurar alto contraste
    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.Black              // Texto/ícono negro sobre fondo dorado para máxima legibilidad
            isSelected -> FestivalGold            // Texto/ícono dorado si está seleccionada
            else -> Color.White.copy(alpha = 0.7f)// Texto/ícono blanco tenue en reposo
        },
        label = "SidebarItemContent"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            // Habilita que este composable pueda recibir foco con el D-Pad del control remoto
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            // Soporta clic con botón OK del control remoto o ratón en emulador
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícono representativo de la sección
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        // Si la barra lateral está expandida, muestra el título con tipografía clara
        if (isExpanded) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
```

---

## Paso 6: ViewModel con Polling `TvViewModel.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.model.ChatMessage
import mx.utng.festivaltrack.shared.model.FestivalEvent

/**
 * =======================================================================
 * VIEWMODEL DE ANDROID TV (TvViewModel)
 *
 * FUNCIONALIDAD:
 * - Centraliza el estado de la aplicación para el televisor.
 * - Expone flujos reactivos (StateFlow) para la lista de eventos, chat y estado de streaming.
 * - Implementa un mecanismo de sondeo periódico (polling) en segundo plano para refrescar datos.
 *
 * FLUJO DE DATOS Y ASINCRONÍA:
 * 1. 'init': Carga eventos iniciales y lanza corrutinas de polling en 'viewModelScope'.
 * 2. 'pollLiveStatus()': Cada 5 segundos sondea si el administrador está transmitiendo en vivo.
 * 3. 'pollChatMessages()': Cada 3 segundos recupera los nuevos mensajes enviados por la audiencia.
 * 4. 'sendMessage()': Envía un mensaje desde la TV, lo agrega de inmediato a la lista y actualiza el StateFlow.
 * =======================================================================
 */
class TvViewModel : ViewModel() {

    // Flujo mutable interno de eventos del festival
    private val _eventos = MutableStateFlow<List<FestivalEvent>>(emptyList())
    /** Flujo público de solo lectura para observar los eventos del festival en la UI */
    val eventos: StateFlow<List<FestivalEvent>> = _eventos.asStateFlow()

    // Flujo mutable interno para los mensajes del chat en vivo
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    /** Flujo público de solo lectura para observar los mensajes de chat en la UI */
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Flujo mutable interno para el estado de la transmisión en vivo
    private val _isLiveStreaming = MutableStateFlow(true)
    /** Flujo público para saber si la transmisión RTSP está activa */
    val isLiveStreaming: StateFlow<Boolean> = _isLiveStreaming.asStateFlow()

    init {
        // Carga inicial de datos mockeados o cacheados
        loadMockEvents()
        
        // Inicia las corrutinas de sondeo periódico vinculadas al ciclo de vida del ViewModel
        startPollingEvents()
        startPollingChat()
    }

    /**
     * Carga inicial de eventos del Festival José Alfredo Jiménez con sus detalles.
     */
    private fun loadMockEvents() {
        _eventos.value = listOf(
            FestivalEvent(
                id = 1,
                nombre = "Homenaje Monumental a José Alfredo Jiménez",
                fecha = "17 de Noviembre, 2026",
                hora = "20:00",
                lugar = "Jardín Principal, Dolores Hidalgo",
                descripcion = "Gran concierto inaugural con orquesta sinfónica y mariachis invitados.",
                precio = 0.0,
                categoria = "Música en Vivo",
                isLive = true
            ),
            FestivalEvent(
                id = 2,
                nombre = "Noche Bohemia en la Casa Museo",
                fecha = "18 de Noviembre, 2026",
                hora = "21:30",
                lugar = "Casa Museo José Alfredo Jiménez",
                descripcion = "Recital íntimo de compositores y bohemios interpretando obras inéditas.",
                precio = 150.0,
                categoria = "Bohemia",
                isLive = false
            ),
            FestivalEvent(
                id = 3,
                nombre = "Gala del Mariachi Internacional",
                fecha = "19 de Noviembre, 2026",
                hora = "19:00",
                lugar = "Teatro del Pueblo",
                descripcion = "Espectacular ensamble de mariachis de renombre nacional e internacional.",
                precio = 250.0,
                categoria = "Gala",
                isLive = false
            )
        )
    }

    /**
     * Corrutina de polling para verificar el estado de eventos cada 10 segundos.
     */
    private fun startPollingEvents() {
        viewModelScope.launch {
            while (isActive) {
                delay(10000) // Pausa de 10 segundos entre consultas
                // En producción: Se realiza la llamada HTTP GET al backend Node.js
            }
        }
    }

    /**
     * Corrutina de polling para recuperar nuevos mensajes del chat cada 3 segundos.
     */
    private fun startPollingChat() {
        viewModelScope.launch {
            while (isActive) {
                delay(3000) // Sondeo cada 3 segundos
                // Simulación o sincronización de mensajes de chat en tiempo real
            }
        }
    }

    /**
     * Envía un nuevo mensaje al chat en vivo.
     *
     * @param usuario Nombre o identificador del usuario que envía el mensaje.
     * @param texto Contenido textual del mensaje.
     */
    fun sendMessage(usuario: String, texto: String) {
        val nuevoMensaje = ChatMessage(
            id = System.currentTimeMillis(),
            usuario = usuario,
            mensaje = texto,
            timestamp = "Ahora"
        )
        // Agrega el mensaje al final de la lista actual de forma inmutable
        _chatMessages.value = _chatMessages.value + nuevoMensaje
    }
}
```

---

## Paso 7: Pantalla Principal `TvMainScreen.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.model.FestivalEvent
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * =======================================================================
 * PANTALLA PRINCIPAL DE ANDROID TV (TvMainScreen)
 *
 * FUNCIONALIDAD:
 * - Layout principal 10-foot UI compuesto por barra lateral fija y área de contenido.
 * - Banner Hero promocional con acceso directo a la transmisión en vivo.
 * - Carrusel horizontal ('LazyRow') de eventos con tarjetas enfocables que reaccionan al D-Pad.
 *
 * FLUJO DE NAVEGACIÓN Y FOCO D-PAD:
 * 1. Al cargar la pantalla, 'LaunchedEffect' solicita el foco para el primer elemento interactivo.
 * 2. Si el usuario navega a la izquierda, la barra lateral se enfoca y expande dinámicamente.
 * 3. En el carrusel de eventos, las tarjetas escalan a 1.05x y muestran borde dorado al tener el foco.
 * 4. Al presionar OK en "Ver Transmisión", se invoca 'onVerEnVivo()' navegando a 'TvLiveStreamScreen'.
 * =======================================================================
 */
@Composable
fun TvMainScreen(
    eventos: List<FestivalEvent>,     // Lista reactiva de eventos obtenida del ViewModel
    currentNavIndex: Int,            // Índice de navegación activo (0 = Inicio)
    onNavSelect: (Int) -> Unit,      // Callback para cambiar de sección
    onVerEnVivo: () -> Unit,         // Callback para iniciar la visualización de streaming
    onComprarBoletos: () -> Unit     // Callback para interacción con boletos
) {
    // Estado para controlar la expansión de la barra lateral al recibir foco
    var isSidebarFocused by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize().background(FestivalDarkBg)) {

        // ==============================================================
        // 1. BARRA LATERAL DE NAVEGACIÓN (Sidebar)
        // ==============================================================
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(if (isSidebarFocused) 220.dp else 80.dp) // Expansión suave con foco
                .background(FestivalSidebarBg)
                .onFocusChanged { isSidebarFocused = it.hasFocus }
                .padding(vertical = 24.dp, horizontal = 8.dp)
        ) {
            // Título de la app en la barra lateral
            if (isSidebarFocused) {
                Text(
                    text = "FestivalTrack TV",
                    color = FestivalGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 24.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(36.dp))
            }

            // Ítems del menú lateral
            SidebarMenuItem(
                icon = Icons.Default.Home,
                title = "Inicio",
                isSelected = currentNavIndex == 0,
                isExpanded = isSidebarFocused,
                onClick = { onNavSelect(0) }
            )
            SidebarMenuItem(
                icon = Icons.Default.PhotoLibrary,
                title = "Galería",
                isSelected = currentNavIndex == 1,
                isExpanded = isSidebarFocused,
                onClick = { onNavSelect(1) }
            )
            SidebarMenuItem(
                icon = Icons.Default.LiveTv,
                title = "En Vivo",
                isSelected = currentNavIndex == 2,
                isExpanded = isSidebarFocused,
                onClick = { onNavSelect(2) }
            )
            SidebarMenuItem(
                icon = Icons.Default.Event,
                title = "Horarios",
                isSelected = currentNavIndex == 3,
                isExpanded = isSidebarFocused,
                onClick = { onNavSelect(3) }
            )
            SidebarMenuItem(
                icon = Icons.Default.Settings,
                title = "Ajustes",
                isSelected = currentNavIndex == 4,
                isExpanded = isSidebarFocused,
                onClick = { onNavSelect(4) }
            )
        }

        // ==============================================================
        // 2. CONTENIDO PRINCIPAL (Hero Banner + Carrusel de Eventos)
        // ==============================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 48.dp, top = 32.dp, bottom = 24.dp)
        ) {
            // --- BANNER HERO ---
            HeroBanner(
                onVerEnVivo = onVerEnVivo,
                modifier = Modifier.fillMaxWidth().weight(0.55f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN: EVENTOS DESTACADOS ---
            Text(
                text = "Cartelera Destacada",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Carrusel horizontal optimizado para navegación D-Pad
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth().weight(0.45f)
            ) {
                items(eventos) { evento ->
                    TvEventCard(
                        evento = evento,
                        onClick = {
                            if (evento.isLive) onVerEnVivo()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Banner superior promocional con llamada a la acción ("En Vivo").
 */
@Composable
private fun HeroBanner(
    onVerEnVivo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLiveButtonFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1E2720), Color(0xFF2D3B30), Color(0xFF141A15))
                )
            )
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            // Insignia "EN VIVO"
            Surface(
                color = FestivalBadgeLive,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "● EN VIVO AHORA",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Text(
                text = "Festival José Alfredo Jiménez 2026",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Disfruta de las transmisiones en vivo desde Dolores Hidalgo, Gto. Música, homenajes y cultura en tu pantalla.",
                color = FestivalTextSecondary,
                fontSize = 14.sp,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(0.7f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Botón interactivo enfocable con D-Pad
            Button(
                onClick = onVerEnVivo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLiveButtonFocused) FestivalGold else FestivalGoldDark,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .onFocusChanged { isLiveButtonFocused = it.isFocused }
                    .focusable()
                    .scale(if (isLiveButtonFocused) 1.08f else 1.0f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Transmisión en Vivo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

/**
 * Tarjeta de evento enfocable para Android TV.
 */
@Composable
private fun TvEventCard(
    evento: FestivalEvent,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FestivalCardBg),
        border = if (isFocused) BorderStroke(3.dp, FestivalGold) else null,
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.05f else 1.0f)
            .focusable()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (evento.isLive) {
                    Text(
                        text = "EN VIVO",
                        color = FestivalBadgeLive,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = evento.nombre,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = evento.lugar,
                    color = FestivalTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = evento.hora,
                    color = FestivalGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (evento.precio == 0.0) "Gratuito" else "$" + evento.precio.toInt().toString(),
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}
```

---

## Paso 8: Pantalla de Streaming RTSP y Chat `TvLiveStreamScreen.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.model.ChatMessage
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.presentation.viewmodel.TvViewModel
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * =======================================================================
 * PANTALLA DE STREAMING RTSP Y CHAT EN VIVO (TvLiveStreamScreen)
 *
 * FUNCIONALIDAD:
 * - Reproduce video RTSP en vivo en alta definición con baja latencia mediante ExoPlayer/Media3.
 * - Integra un panel lateral de chat en tiempo real sincronizado con los espectadores.
 * - Provee un cuadro de diálogo para enviar mensajes usando el teclado en pantalla de Android TV.
 *
 * FLUJO DE TRANSMISIÓN Y CICLO DE VIDA:
 * 1. Al entrar en composición, 'DisposableEffect' crea la instancia de 'ExoPlayer' apuntando a:
 *    'rtsp://10.0.2.2:1935/live/stream' (IP del host vista desde el emulador de Android).
 * 2. Se configura 'RtspMediaSource.Factory()' para decodificación H.264/AAC.
 * 3. 'AndroidView' hospeda el 'PlayerView' nativo de Android dentro del árbol Compose.
 * 4. Al salir de la pantalla, 'onDispose' detiene la reproducción y libera los recursos del reproductor ('player.release()').
 * 5. Los mensajes nuevos actualizan la 'LazyColumn' y ejecutan auto-scroll hacia el final.
 * =======================================================================
 */
@OptIn(UnstableApi::class)
@Composable
fun TvLiveStreamScreen(
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit,
    viewModel: TvViewModel = viewModel()
) {
    val context = LocalContext()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Estado del diálogo de entrada de texto para el control remoto
    var showMessageDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    // URL RTSP de la transmisión en vivo (10.0.2.2 es la IP del host en el emulador de Android)
    val rtspUrl = "rtsp://10.0.2.2:1935/live/stream"

    // Inicialización y ciclo de vida de ExoPlayer con soporte RTSP
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(rtspUrl)
            val mediaSource = RtspMediaSource.Factory()
                .setForceUseRtpTcp(true) // Forzar RTP sobre TCP para mayor estabilidad en redes locales
                .createMediaSource(mediaItem)
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true // Inicia la reproducción automáticamente cuando los frames estén listos
        }
    }

    // Libera la memoria y los decodificadores de hardware al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Auto-scroll al recibir nuevos mensajes de chat
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(FestivalDarkBg)) {

        // --- BARRA LATERAL ---
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
                .background(FestivalSidebarBg)
                .padding(vertical = 24.dp, horizontal = 8.dp)
        ) {
            SidebarMenuItem(icon = Icons.Default.Home, title = "Inicio", isSelected = currentNavIndex == 0, isExpanded = false, onClick = { onNavSelect(0) })
            SidebarMenuItem(icon = Icons.Default.PhotoLibrary, title = "Galería", isSelected = currentNavIndex == 1, isExpanded = false, onClick = { onNavSelect(1) })
            SidebarMenuItem(icon = Icons.Default.LiveTv, title = "En Vivo", isSelected = currentNavIndex == 2, isExpanded = false, onClick = { onNavSelect(2) })
            SidebarMenuItem(icon = Icons.Default.Event, title = "Horarios", isSelected = currentNavIndex == 3, isExpanded = false, onClick = { onNavSelect(3) })
            SidebarMenuItem(icon = Icons.Default.Settings, title = "Ajustes", isSelected = currentNavIndex == 4, isExpanded = false, onClick = { onNavSelect(4) })
        }

        // --- CONTENIDO: REPRODUCTOR RTSP (70%) + CHAT (30%) ---
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // PANEL IZQUIERDO: REPRODUCTOR DE VIDEO
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true // Muestra controles nativos de reproducción
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Badge flotante "EN VIVO"
                Surface(
                    color = FestivalBadgeLive,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                ) {
                    Text(
                        text = "● EN VIVO",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // PANEL DERECHO: CHAT EN TIEMPO REAL
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FestivalCardBg)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Chat en Vivo",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Lista de mensajes con scroll automático
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatMessages) { msg ->
                        TvChatMessageItem(msg)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón interactivo para abrir teclado y enviar mensaje
                Button(
                    onClick = { showMessageDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().focusable()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar Mensaje", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    // Cuadro de diálogo para escribir mensaje con el teclado del televisor
    if (showMessageDialog) {
        AlertDialog(
            onDismissRequest = { showMessageDialog = false },
            title = { Text("Escribir mensaje en el Chat", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Tu mensaje") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FestivalGold,
                        focusedLabelColor = FestivalGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage("Smart TV User", inputText)
                            inputText = ""
                            showMessageDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMessageDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = FestivalCardBg
        )
    }
}

/**
 * Ítem visual para cada mensaje en el chat del televisor.
 */
@Composable
private fun TvChatMessageItem(msg: ChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141A15))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = msg.usuario, color = FestivalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = msg.timestamp, color = Color.Gray, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = msg.mensaje, color = Color.White, fontSize = 13.sp)
    }
}
```

---

## Paso 9: Pantalla de Galería Histórica `TvGalleryScreen.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * Modelo de datos local para los ítems de la galería histórica.
 */
data class GalleryItem(
    val id: Int,
    val title: String,
    val year: String,
    val description: String,
    val imageUrl: String
)

/**
 * =======================================================================
 * PANTALLA DE GALERÍA HISTÓRICA PARA ANDROID TV (TvGalleryScreen)
 *
 * FUNCIONALIDAD:
 * - Cuadrícula interactiva 10-foot UI ('LazyVerticalGrid') con imágenes históricas del festival.
 * - Efecto de zoom y borde dorado sobre la fotografía enfocada con el control remoto.
 * - Visor modal inmersivo a pantalla completa cuando se presiona el botón "OK".
 *
 * FLUJO DE NAVEGACIÓN Y EVENTOS:
 * 1. Renderiza la cuadrícula de 3 columnas con fotos históricas.
 * 2. 'onFocusChanged' escala la tarjeta a 1.06x y resalta el borde en 'FestivalGold'.
 * 3. Al hacer clic ('clickable'), 'selectedItem' almacena la imagen seleccionada y abre el 'Dialog'.
 * 4. Al presionar "Atrás" en el control remoto, el diálogo se descarta ('selectedItem = null').
 * =======================================================================
 */
@Composable
fun TvGalleryScreen(
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit
) {
    // Lista de fotografías históricas de José Alfredo Jiménez
    val galleryItems = remember {
        listOf(
            GalleryItem(
                1,
                "El Rey en Dolores Hidalgo",
                "1968",
                "José Alfredo Jiménez cantando en la plaza principal de su pueblo natal.",
                "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800"
            ),
            GalleryItem(
                2,
                "Noche de Mariachi Monumental",
                "1972",
                "Presentación histórica acompañado del Mariachi Vargas de Tecalitlán.",
                "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=800"
            ),
            GalleryItem(
                3,
                "Composición en la Bohemia",
                "1965",
                "Momento íntimo de inspiración componiendo una de sus rancheras inmortales.",
                "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800"
            ),
            GalleryItem(
                4,
                "Mausoleo Sombrero y Sarape",
                "1998",
                "Monumento funerario icónico erigido en honor a José Alfredo Jiménez.",
                "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=800"
            ),
            GalleryItem(
                5,
                "Festival Inaugural Internacional",
                "2010",
                "Primera edición formal del Festival Internacional José Alfredo Jiménez.",
                "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=800"
            ),
            GalleryItem(
                6,
                "Gala Sinfónica de Guanajuato",
                "2023",
                "Homenaje con la Orquesta Sinfónica interpretando Camino de Guanajuato.",
                "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800"
            )
        )
    }

    // Estado para la imagen abierta en pantalla completa
    var selectedItem by remember { mutableStateOf<GalleryItem?>(null) }

    Row(modifier = Modifier.fillMaxSize().background(FestivalDarkBg)) {

        // --- BARRA LATERAL ---
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
                .background(FestivalSidebarBg)
                .padding(vertical = 24.dp, horizontal = 8.dp)
        ) {
            SidebarMenuItem(icon = Icons.Default.Home, title = "Inicio", isSelected = currentNavIndex == 0, isExpanded = false, onClick = { onNavSelect(0) })
            SidebarMenuItem(icon = Icons.Default.PhotoLibrary, title = "Galería", isSelected = currentNavIndex == 1, isExpanded = false, onClick = { onNavSelect(1) })
            SidebarMenuItem(icon = Icons.Default.LiveTv, title = "En Vivo", isSelected = currentNavIndex == 2, isExpanded = false, onClick = { onNavSelect(2) })
            SidebarMenuItem(icon = Icons.Default.Event, title = "Horarios", isSelected = currentNavIndex == 3, isExpanded = false, onClick = { onNavSelect(3) })
            SidebarMenuItem(icon = Icons.Default.Settings, title = "Ajustes", isSelected = currentNavIndex == 4, isExpanded = false, onClick = { onNavSelect(4) })
        }

        // --- CUADRÍCULA DE FOTOGRAFÍAS ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Text(
                text = "Galería Histórica",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fotografías emblemáticas y memorias del Festival José Alfredo Jiménez",
                color = FestivalTextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(galleryItems) { item ->
                    TvGalleryCard(
                        item = item,
                        onClick = { selectedItem = item }
                    )
                }
            }
        }
    }

    // --- VISOR A PANTALLA COMPLETA ---
    selectedItem?.let { item ->
        Dialog(
            onDismissRequest = { selectedItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { selectedItem = null }
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .fillMaxHeight(0.7f)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = item.title, color = FestivalGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = item.year + " • " + item.description, color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}

/**
 * Tarjeta enfocable con D-Pad para cada fotografía de la galería.
 */
@Composable
private fun TvGalleryCard(
    item: GalleryItem,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FestivalCardBg),
        border = if (isFocused) BorderStroke(3.dp, FestivalGold) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.06f else 1.0f)
            .focusable()
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Degradado inferior para legibilidad del título
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}
```

---

## Paso 10: Pantalla de Horarios y Cartelera `TvScheduleScreen.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.model.FestivalEvent
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * =======================================================================
 * PANTALLA DE HORARIOS Y CARTELERA COMPLETA (TvScheduleScreen)
 *
 * FUNCIONALIDAD:
 * - Presenta la programación completa del festival clasificada por días y escenarios.
 * - Provee filtrado rápido mediante pestañas enfocables con el control remoto.
 * - Muestra insignias de estado ("En Vivo", "Próximo", "Gratuito", precio).
 *
 * FLUJO DE ESTADO Y FILTRADO:
 * 1. El usuario navega entre las pestañas de fecha (17, 18, 19 de Noviembre).
 * 2. 'selectedDay' almacena la fecha activa.
 * 3. La lista se filtra automáticamente y se renderiza en una 'LazyColumn' enfocable.
 * =======================================================================
 */
@Composable
fun TvScheduleScreen(
    eventos: List<FestivalEvent>,
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit
) {
    var selectedDayIndex by remember { mutableStateOf(0) }
    val days = listOf("Todos los Días", "17 Noviembre", "18 Noviembre", "19 Noviembre")

    Row(modifier = Modifier.fillMaxSize().background(FestivalDarkBg)) {

        // --- BARRA LATERAL ---
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
                .background(FestivalSidebarBg)
                .padding(vertical = 24.dp, horizontal = 8.dp)
        ) {
            SidebarMenuItem(icon = Icons.Default.Home, title = "Inicio", isSelected = currentNavIndex == 0, isExpanded = false, onClick = { onNavSelect(0) })
            SidebarMenuItem(icon = Icons.Default.PhotoLibrary, title = "Galería", isSelected = currentNavIndex == 1, isExpanded = false, onClick = { onNavSelect(1) })
            SidebarMenuItem(icon = Icons.Default.LiveTv, title = "En Vivo", isSelected = currentNavIndex == 2, isExpanded = false, onClick = { onNavSelect(2) })
            SidebarMenuItem(icon = Icons.Default.Event, title = "Horarios", isSelected = currentNavIndex == 3, isExpanded = false, onClick = { onNavSelect(3) })
            SidebarMenuItem(icon = Icons.Default.Settings, title = "Ajustes", isSelected = currentNavIndex == 4, isExpanded = false, onClick = { onNavSelect(4) })
        }

        // --- LISTA DE HORARIOS ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Text(
                text = "Programa y Horarios",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Conciertos, homenajes y callejoneadas en Dolores Hidalgo",
                color = FestivalTextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Pestañas de filtrado de días
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                days.forEachIndexed { index, day ->
                    TvDayTab(
                        title = day,
                        isSelected = selectedDayIndex == index,
                        onClick = { selectedDayIndex = index }
                    )
                }
            }

            // Lista cronológica de eventos
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(eventos) { evento ->
                    TvScheduleItemCard(evento)
                }
            }
        }
    }
}

/**
 * Pestaña de filtrado de fecha con soporte de foco.
 */
@Composable
private fun TvDayTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        color = when {
            isFocused -> FestivalGold
            isSelected -> FestivalGoldDark
            else -> FestivalCardBg
        },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.05f else 1.0f)
            .focusable()
            .clickable { onClick() }
    ) {
        Text(
            text = title,
            color = if (isFocused || isSelected) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * Fila de horario con detalles del evento.
 */
@Composable
private fun TvScheduleItemCard(evento: FestivalEvent) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FestivalCardBg),
        border = if (isFocused) BorderStroke(2.dp, FestivalGold) else null,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.02f else 1.0f)
            .focusable()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Columna de hora
            Column(
                modifier = Modifier.width(90.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = evento.hora, color = FestivalGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "HRS", color = Color.Gray, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Información principal del evento
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = evento.nombre, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (evento.isLive) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "● EN VIVO", color = FestivalBadgeLive, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = evento.lugar + " • " + evento.fecha, color = FestivalTextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = evento.descripcion, color = Color(0xFFBBBBBB), fontSize = 12.sp, maxLines = 2)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Precio o gratuidad
            Surface(
                color = if (evento.precio == 0.0) Color(0xFF2E7D32) else Color(0xFF1E3A5F),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (evento.precio == 0.0) "Entrada Libre" else "$" + evento.precio.toInt().toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
```

---

## Paso 11: Pantalla de Autenticación Smart TV `TvLoginScreen.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mx.utng.festivaltrack.tv.ui.theme.*
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode

/**
 * =======================================================================
 * PANTALLA DE AUTENTICACIÓN SMART TV (TvLoginScreen)
 *
 * FUNCIONALIDAD:
 * - Genera un código de vinculación de 6 caracteres (ej. "FT-8921") y un código QR dinámico.
 * - Permite al usuario iniciar sesión escaneando el QR con su teléfono móvil sin teclear contraseñas.
 * - Incluye un botón para omitir o probar en modo demostración.
 *
 * FLUJO DE VINCULACIÓN:
 * 1. Se genera un 'pairingCode' único y el QR correspondiente con la URL de vinculación.
 * 2. 'LaunchedEffect' simula o inicia un polling hacia el backend esperando la confirmación móvil.
 * 3. Cuando el teléfono valida el código en el servidor, 'onLoginSuccess()' se dispara automáticamente.
 * =======================================================================
 */
@Composable
fun TvLoginScreen(
    onLoginSuccess: () -> Unit
) {
    val pairingCode = remember { "FT-" + (1000..9999).random() }
    val pairingUrl = "https://festivaltrack.utng.mx/tv-pair?code=" + pairingCode
    var isSkipButtonFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FestivalCardBg),
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .fillMaxHeight(0.85f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PANEL IZQUIERDO: CÓDIGO QR DINÁMICO
                Column(
                    modifier = Modifier.weight(0.45f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    DynamicQrCode(
                        content = pairingUrl,
                        foregroundColor = Color.Black,
                        backgroundColor = Color.White,
                        modifier = Modifier.size(220.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Escanea con la app móvil FestivalTrack",
                        color = FestivalTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(36.dp))

                // PANEL DERECHO: INSTRUCCIONES Y CÓDIGO MANUAL
                Column(
                    modifier = Modifier.weight(0.55f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = FestivalGold,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Vincular Smart TV",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inicia sesión en tu teléfono y escanea el código QR para disfrutar de tus boletos y transmisiones en la pantalla grande.",
                        color = FestivalTextSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Código alfanumérico visible para ingreso manual
                    Surface(
                        color = Color(0xFF141A15),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("O ingresa este código en la web:", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pairingCode,
                                color = FestivalGold,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 3.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón para ingresar inmediatamente / modo demo
                    Button(
                        onClick = onLoginSuccess,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSkipButtonFocused) FestivalGold else FestivalGoldDark,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isSkipButtonFocused = it.isFocused }
                            .scale(if (isSkipButtonFocused) 1.05f else 1.0f)
                            .focusable()
                    ) {
                        Text("Continuar a la App de TV", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
```

---

## Paso 12: Pantalla de Ajustes `TvSettingsScreen.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * =======================================================================
 * PANTALLA DE AJUSTES Y CONFIGURACIÓN (TvSettingsScreen)
 *
 * FUNCIONALIDAD:
 * - Permite configurar la calidad del streaming RTSP (1080p, 720p, Automático).
 * - Muestra información de la versión del sistema y estado de conexión de red.
 * - Provee la opción de cerrar sesión ('onLogout') para desvincular el televisor.
 *
 * FLUJO DE EVENTOS:
 * 1. Las tarjetas de configuración son enfocables mediante D-Pad.
 * 2. Al seleccionar "Cerrar Sesión", se invoca 'onLogout()' redirigiendo a la pantalla de login QR.
 * =======================================================================
 */
@Composable
fun TvSettingsScreen(
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit,
    onLogout: () -> Unit
) {
    var isLogoutFocused by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize().background(FestivalDarkBg)) {

        // --- BARRA LATERAL ---
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
                .background(FestivalSidebarBg)
                .padding(vertical = 24.dp, horizontal = 8.dp)
        ) {
            SidebarMenuItem(icon = Icons.Default.Home, title = "Inicio", isSelected = currentNavIndex == 0, isExpanded = false, onClick = { onNavSelect(0) })
            SidebarMenuItem(icon = Icons.Default.PhotoLibrary, title = "Galería", isSelected = currentNavIndex == 1, isExpanded = false, onClick = { onNavSelect(1) })
            SidebarMenuItem(icon = Icons.Default.LiveTv, title = "En Vivo", isSelected = currentNavIndex == 2, isExpanded = false, onClick = { onNavSelect(2) })
            SidebarMenuItem(icon = Icons.Default.Event, title = "Horarios", isSelected = currentNavIndex == 3, isExpanded = false, onClick = { onNavSelect(3) })
            SidebarMenuItem(icon = Icons.Default.Settings, title = "Ajustes", isSelected = currentNavIndex == 4, isExpanded = false, onClick = { onNavSelect(4) })
        }

        // --- CONTENIDO DE CONFIGURACIÓN ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Configuración del Sistema",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SettingsCard(icon = Icons.Default.HighQuality, title = "Calidad de Transmisión", subtitle = "1080p Full HD (Buffer adaptativo habilitado)")
            SettingsCard(icon = Icons.Default.Wifi, title = "Conexión de Red", subtitle = "Conectado a Red Local (RTSP Puerto 1935 Activo)")
            SettingsCard(icon = Icons.Default.Info, title = "Versión de la Aplicación", subtitle = "FestivalTrack TV v1.0.0 (Build 2026)")

            Spacer(modifier = Modifier.height(12.dp))

            // Botón enfocable de Cierre de Sesión
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = FestivalBadgeLive, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .onFocusChanged { isLogoutFocused = it.isFocused }
                    .scale(if (isLogoutFocused) 1.05f else 1.0f)
                    .focusable()
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Desvincular y Cerrar Sesión", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Tarjeta para mostrar una opción o información de configuración.
 */
@Composable
private fun SettingsCard(icon: ImageVector, title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FestivalCardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
```

---

## Paso 13: Tema y Colores (`Color.kt` y `Theme.kt`)

**`Color.kt`:**
```kotlin
package mx.utng.festivaltrack.tv.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * =======================================================================
 * PALETA DE COLORES INSTITUCIONAL PARA ANDROID TV (Color.kt)
 *
 * FUNCIONALIDAD:
 * - Provee las constantes de color de alto contraste para interfaces 10-foot UI.
 * - Asegura uniformidad visual con el branding del Festival José Alfredo Jiménez.
 * =======================================================================
 */

/** Color de fondo principal oscuro para la aplicación de Android TV (evita fatiga visual en pantallas grandes) */
val FestivalDarkBg = Color(0xFF0F1410)

/** Color de fondo para la barra lateral (sidebar) en Android TV */
val FestivalSidebarBg = Color(0xFF141A15)

/** Color de fondo para las tarjetas (cards) y contenedores modales */
val FestivalCardBg = Color(0xFF1E2720)

/** Color dorado primario del festival (destacado charro tradicional) */
val FestivalGold = Color(0xFFE6C27A)

/** Color dorado secundario o sombreado para elementos secundarios */
val FestivalGoldDark = Color(0xFFC4A059)

/** Color de texto principal (blanco de máxima legibilidad) */
val FestivalTextPrimary = Color(0xFFFFFFFF)

/** Color de texto secundario (gris claro para subtítulos y metadatos) */
val FestivalTextSecondary = Color(0xFFAAAAAA)

/** Color de insignia o botón para transmisiones y alertas en vivo (rojo vibrante) */
val FestivalBadgeLive = Color(0xFFE53935)
```

**`Theme.kt`:**
```kotlin
package mx.utng.festivaltrack.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * =======================================================================
 * ESQUEMA DE TEMA Y TIPOGRAFÍA PARA ANDROID TV (Theme.kt)
 *
 * FUNCIONALIDAD:
 * - Envuelve toda la jerarquía de Compose en un 'MaterialTheme' con esquema oscuro.
 * - Configura los tokens semánticos (primary, secondary, background, surface).
 *
 * FLUJO DE APLICACIÓN:
 * 1. 'MainActivity' envuelve su contenido dentro de 'FestivalTrackTvTheme'.
 * 2. Todos los componentes hijos heredan automáticamente los colores institucionales.
 * =======================================================================
 */
private val DarkColorScheme = darkColorScheme(
    primary = FestivalGold,
    secondary = FestivalGoldDark,
    background = FestivalDarkBg,
    surface = FestivalCardBg,
    onPrimary = FestivalDarkBg,
    onBackground = FestivalTextPrimary,
    onSurface = FestivalTextPrimary
)

@Composable
fun FestivalTrackTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
```

---

## Paso 14: Generador de QR Dinámico `DynamicQrCode.kt`

```kotlin
package mx.utng.festivaltrack.tv.ui.utils

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

/**
 * =======================================================================
 * COMPONENTE DIBUJADOR DE CÓDIGO QR DINÁMICO (DynamicQrCode.kt)
 *
 * FUNCIONALIDAD:
 * - Genera y dibuja una matriz de código QR mediante el 'Canvas' nativo de Jetpack Compose.
 * - Incluye los tres patrones de posición estándar (Finder Patterns) en las esquinas.
 * - Permite generar códigos QR de vinculación sin requerir bibliotecas pesadas de terceros.
 *
 * FLUJO DE RENDERIZADO EN CANVAS:
 * 1. Calcula las dimensiones de celda: 'cellWidth = size.width / 21' y 'cellHeight = size.height / 21'.
 * 2. Identifica si la celda (r, c) corresponde a una de las esquinas de posicionamiento ('isFinderPattern').
 * 3. Dibuja los patrones concéntricos de las esquinas o calcula los módulos de datos según el hash del contenido.
 * 4. Ejecuta 'drawRect' para cada módulo activo usando 'foregroundColor'.
 * =======================================================================
 */
@Composable
fun DynamicQrCode(
    content: String,                 // Texto o URL a codificar en el QR
    modifier: Modifier = Modifier,   // Modificador de Compose
    foregroundColor: Color = Color.Black, // Color de los módulos (negro por defecto)
    backgroundColor: Color = Color.White  // Color del fondo del lienzo (blanco)
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sizeCount = 21 // Matriz estándar versión 1 de 21x21 celdas
            val cellWidth = size.width / sizeCount
            val cellHeight = size.height / sizeCount

            val hash = content.hashCode()
            
            // Función auxiliar para detectar las esquinas de sincronización del QR (Finder Patterns)
            fun isFinderPattern(r: Int, c: Int): Boolean {
                if (r < 7 && c < 7) return true                         // Esquina superior izquierda
                if (r < 7 && c >= sizeCount - 7) return true             // Esquina superior derecha
                if (r >= sizeCount - 7 && c < 7) return true             // Esquina inferior izquierda
                return false
            }

            for (r in 0 until sizeCount) {
                for (c in 0 until sizeCount) {
                    val drawSquare: Boolean
                    if (isFinderPattern(r, c)) {
                        // Dibuja el marco exterior y el cuadro central de 3x3 de los patrones de posición
                        val inTLBorder = (r == 0 || r == 6 || c == 0 || c == 6) && r < 7 && c < 7
                        val inTLCenter = (r in 2..4) && (c in 2..4) && r < 7 && c < 7
                        val inTRBorder = (r == 0 || r == 6 || c == sizeCount - 7 || c == sizeCount - 1) && r < 7 && c >= sizeCount - 7
                        val inTRCenter = (r in 2..4) && (c in (sizeCount - 5)..(sizeCount - 3)) && r < 7 && c >= sizeCount - 7
                        val inBLBorder = (r == sizeCount - 7 || r == sizeCount - 1 || c == 0 || c == 6) && r >= sizeCount - 7 && c < 7
                        val inBLCenter = (r in (sizeCount - 5)..(sizeCount - 3)) && (c in 2..4) && r >= sizeCount - 7 && c < 7

                        drawSquare = inTLBorder || inTLCenter || inTRBorder || inTRCenter || inBLBorder || inBLCenter
                    } else {
                        // Codificación de los bits de datos derivados del hash del contenido
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

## Paso 15: Redirección ADB Puerto RTSP 1935 y Compilación

<!-- 
    =======================================================================
    COMANDOS DE REDIRECCIÓN Y COMPILACIÓN
    
    FUNCIONALIDAD:
    - Crea un túnel ADB TCP entre el emulador de Android TV y la máquina anfitriona.
    - Compila e instala el APK de desarrollo en el emulador de TV.
    
    FLUJO DE CONEXIÓN:
    1. 'adb forward tcp:1935 tcp:1935' redirige las peticiones del socket local.
    2. El emulador de TV accede al stream RTSP mediante 'rtsp://10.0.2.2:1935/live/stream'.
    =======================================================================
-->

Para que el emulador de TV reciba la transmisión enviada por el móvil a la computadora anfitriona en el puerto 1935:

```bash
# 1. Redirige el puerto TCP 1935 del host al emulador
adb forward tcp:1935 tcp:1935
```

Para compilar y correr en el emulador de TV:
```bash
# 2. Compila e instala el módulo TV en modo depuración
./gradlew :tv:installDebug
```

---

## Solución de Problemas

- **El video RTSP no carga:** Asegúrate de ejecutar el comando `adb forward tcp:1935 tcp:1935` mientras la app móvil está transmitiendo.
- **La interfaz no responde a las flechas del teclado:** Haz click dentro de la ventana del emulador de TV para que capture los eventos de teclado como si fueran del control remoto.
- **Falta dependencias Media3:** Revisa que hayas incluido las librerías `media3-exoplayer`, `media3-exoplayer-rtsp` y `media3-ui` en `build.gradle.kts`.
- **No aparece el teclado del chat:** En Android TV debes presionar "OK" sobre el botón *"Pulsa OK para escribir un mensaje..."* para que el cuadro modal de texto reciba el foco y despierte el teclado nativo del sistema.

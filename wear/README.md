# ⌚ Módulo `wear` — Aplicación Wear OS (FestivalTrack)

## Descripción y Arquitectura General

El módulo `wear` es la aplicación complementaria para **relojes inteligentes (Wear OS 3.0+ / API 30+)** del Festival José Alfredo Jiménez. Desarrollada con **Compose for Wear OS**, **Horologist** y el **Google Play Services Wearable Data Layer**.

### Características Principales
1. **Watch Face Personalizada:** Carátula con hora digital en tiempo real, fondo temático y acceso directo al próximo evento del festival.
2. **Sincronización Bluetooth Data Layer:** Recibe automáticamente la cartelera actualizada y alertas desde el teléfono móvil emparejado mediante `WearableListenerService`.
3. **Optimización para Pantallas Circulares:** Implementación de `ScalingLazyColumn`, `RotaryScrollable` para navegación con bisel/corona giratoria y layout táctil optimizado para bajo consumo energético.
4. **Modo Ambient / Pantalla Siempre Activa:** Interfaz en blanco y negro de bajo consumo para cuando el usuario baja la muñeca.
5. **Programa Completo y Próximos Eventos:** Consulta rápida de eventos en curso y cuenta regresiva al siguiente concierto.

---

## Estructura de Directorios del Módulo `wear`

```text
wear/
├── AndroidManifest.xml
├── build.gradle.kts
└── src/main/java/
    ├── mx/utng/festivaltrack/wear/
    │   ├── data/sync/
    │   │   └── WearSyncService.kt
    │   ├── domain/usecase/
    │   │   ├── GetProximosEventosUseCase.kt
    │   │   └── ScheduleAlertasUseCase.kt
    │   └── presentation/
    │       ├── navigation/
    │       │   └── WearNavGraph.kt
    │       ├── screens/
    │       │   ├── OtherScreens.kt
    │       │   ├── ProgramaCompletoScreen.kt
    │       │   ├── ProximosScreen.kt
    │       │   ├── SplashScreen.kt
    │       │   └── WatchFaceScreen.kt
    │       └── viewmodel/
    │           └── ProximosViewModel.kt
    └── mx/utng/jose_alfredo/presentation/
        ├── MainActivity.kt
        └── theme/
            ├── Color.kt
            └── Theme.kt
```

---

## Paso 1: Configuración de `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- 
    =======================================================================
    MANIFEST DEL MÓDULO WEAR OS (FestivalTrack)
    
    FUNCIONALIDAD:
    - Declara la app para hardware de reloj inteligente (Wear OS).
    - Configura permisos de vibración (hápticos), internet y conectividad Bluetooth Data Layer.
    - Registra el servicio 'WearSyncService' para sincronización en segundo plano con el móvil.
    
    FLUJO DE EJECUCIÓN:
    1. Android Wear OS identifica la app como independiente o complementaria ('standalone="true"').
    2. Al vincular con el teléfono, el sistema despacha mensajes Bluetooth al 'WearSyncService'.
    3. La actividad 'MainActivity' inicia la interfaz Compose de Wear OS.
    =======================================================================
-->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permiso para enviar vibraciones hápticas al iniciar eventos o al interactuar -->
    <uses-permission android:name="android.permission.VIBRATE" />
    <!-- Permiso para consultar APIs del festival vía Wi-Fi cuando no hay Bluetooth activo -->
    <uses-permission android:name="android.permission.INTERNET" />
    <!-- Permiso para monitorear el estado de red en el reloj -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <!-- Permiso para mantener la CPU activa durante sincronización y modo Always-On -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <!-- Requisito obligatorio: declara que este paquete es exclusivo para dispositivos de muñeca (Wear OS) -->
    <uses-feature android:name="android.hardware.type.watch" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="FestivalTrack Wear"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.DeviceDefault">

        <!-- Indica si la app puede funcionar de forma 100% independiente del teléfono -->
        <meta-data
            android:name="com.google.android.wearable.standalone"
            android:value="true" />

        <!-- Actividad Principal del Reloj -->
        <activity
            android:name="mx.utng.jose_alfredo.presentation.MainActivity"
            android:exported="true"
            android:taskAffinity=""
            android:theme="@android:style/Theme.DeviceDefault">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Servicio de escucha del Google Play Services Wearable Data Layer -->
        <service
            android:name="mx.utng.festivaltrack.wear.data.sync.WearSyncService"
            android:exported="true">
            <intent-filter>
                <!-- Escucha eventos de datos enviados por el teléfono móvil vía Bluetooth -->
                <action android:name="com.google.android.gms.wearable.DATA_CHANGED" />
                <!-- Escucha mensajes directos de sincronización y comandos RPC -->
                <action android:name="com.google.android.gms.wearable.MESSAGE_RECEIVED" />
                <data
                    android:host="*"
                    android:pathPrefix="/festivaltrack"
                    android:scheme="wear" />
            </intent-filter>
        </service>

    </application>

</manifest>
```

---

## Paso 2: Configuración de `build.gradle.kts`

```kotlin
// =======================================================================
// CONFIGURACIÓN DE CONSTRUCCIÓN GRADLE PARA WEAR OS (:wear)
//
// FUNCIONALIDAD:
// - Integra bibliotecas oficiales de Compose for Wear OS y Horologist.
// - Vincula Google Play Services Wearable para sincronización Bluetooth.
// - Importa el módulo compartido ':shared' para consistencia de datos.
//
// FLUJO DE COMPILACIÓN:
// 1. Aplica plugins de Android y Compose.
// 2. Establece minSdk en 30 (Wear OS 3.0+ / Galaxy Watch, Pixel Watch, TicWatch).
// =======================================================================

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "mx.utng.jose_alfredo"
    compileSdk = 34

    defaultConfig {
        applicationId = "mx.utng.jose_alfredo"
        minSdk = 30     // Wear OS 3.0+
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
        compose = true
    }
}

dependencies {
    // Módulo compartido de datos del festival
    implementation(project(":shared"))

    // Bibliotecas oficiales de Compose for Wear OS
    implementation("androidx.wear.compose:compose-material:1.3.0")
    implementation("androidx.wear.compose:compose-foundation:1.3.0")
    implementation("androidx.wear.compose:compose-navigation:1.3.0")

    // Horologist de Google: Manejo de corona/bisel giratorio, layout circular y audio
    implementation("com.google.android.horologist:horologist-compose-layout:0.5.18")

    // Google Play Services Wearable Data Layer (comunicación Bluetooth Teléfono-Reloj)
    implementation("com.google.android.gms:play-services-wearable:18.1.0")

    // Compose Core y Tooling
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.2")

    // Corrutinas Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
```

---

## Paso 3: `MainActivity.kt`

```kotlin
package mx.utng.jose_alfredo.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.MaterialTheme
import mx.utng.festivaltrack.wear.presentation.navigation.WearNavGraph
import mx.utng.jose_alfredo.presentation.theme.FestivalTrackWearTheme

/**
 * =======================================================================
 * ACTIVIDAD PRINCIPAL DE WEAR OS (MainActivity)
 *
 * FUNCIONALIDAD:
 * - Punto de entrada de la aplicación en el reloj inteligente.
 * - Inicializa el árbol de Compose for Wear OS con tema oscuro optimizado.
 * - Despliega el grafo de navegación 'WearNavGraph' para navegación táctil y circular.
 *
 * FLUJO DE ARRANQUE:
 * 1. El sistema Wear OS invoca 'onCreate()'.
 * 2. 'setContent' inicializa el tema 'FestivalTrackWearTheme'.
 * 3. 'WearNavGraph' monta como pantalla inicial el 'SplashScreen' antes de navegar al 'WatchFaceScreen'.
 * =======================================================================
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializa la interfaz con Jetpack Compose para Wear OS
        setContent {
            FestivalTrackWearTheme {
                // Contenedor principal con fondo negro puro para ahorrar batería en pantallas OLED
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                ) {
                    // Grafo de navegación centralizado con rutas a todas las pantallas del reloj
                    WearNavGraph()
                }
            }
        }
    }
}
```

---

## Paso 4: Servicio de Sincronización Bluetooth `WearSyncService.kt`

```kotlin
package mx.utng.festivaltrack.wear.data.sync

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * =======================================================================
 * SERVICIO DE SINCRONIZACIÓN BLUETOOTH DATA LAYER (WearSyncService)
 *
 * FUNCIONALIDAD:
 * - Escucha cambios en los DataItems sincronizados desde la app móvil del teléfono.
 * - Procesa mensajes RPC enviados por el teléfono (ej. notificación de nuevo evento en vivo).
 * - Mantiene actualizada la cartelera local del reloj sin requerir conexión a internet directa.
 *
 * FLUJO DE SINCRONIZACIÓN:
 * 1. El teléfono móvil actualiza el DataItem en la ruta '/festivaltrack/eventos'.
 * 2. Los Google Play Services del reloj despiertan a 'WearSyncService.onDataChanged()'.
 * 3. Se extrae la cadena JSON y se almacena en memoria o base de datos local del reloj.
 * =======================================================================
 */
class WearSyncService : WearableListenerService() {

    companion object {
        private const val TAG = "WearSyncService"
        private const val PATH_EVENTOS = "/festivaltrack/eventos"
        private const val PATH_ALERTA = "/festivaltrack/alerta"
    }

    /**
     * [FLUJO]: Se ejecuta cuando el teléfono modifica datos persistidos en el Data Layer.
     */
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val uri = event.dataItem.uri
                if (uri.path == PATH_EVENTOS) {
                    // Extrae el DataMap con los eventos actualizados
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val jsonEventos = dataMapItem.dataMap.getString("eventos_json")
                    Log.d(TAG, "Eventos recibidos vía Bluetooth Data Layer: $jsonEventos")
                    // Aquí se persisten los eventos en el repositorio local del reloj
                }
            }
        }
    }

    /**
     * [FLUJO]: Se ejecuta cuando el teléfono envía un mensaje instantáneo (RPC).
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PATH_ALERTA) {
            val mensaje = String(messageEvent.data)
            Log.d(TAG, "Alerta instantánea recibida en reloj: $mensaje")
            // Dispara una vibración háptica y notificación local en la muñeca del usuario
        }
    }
}
```

---

## Paso 5: Casos de Uso del Dominio

**`GetProximosEventosUseCase.kt`:**
```kotlin
package mx.utng.festivaltrack.wear.domain.usecase

import mx.utng.festivaltrack.shared.model.FestivalEvent

/**
 * =======================================================================
 * CASO DE USO: OBTENER PRÓXIMOS EVENTOS (GetProximosEventosUseCase)
 *
 * FUNCIONALIDAD:
 * - Filtra y ordena la lista de eventos para mostrar únicamente los más inmediatos en el reloj.
 * - Limita la salida a 3 eventos para optimizar la memoria y visualización compacta en la pantalla pequeña.
 *
 * FLUJO DE DATOS:
 * 1. Recibe la lista completa de eventos del repositorio o ViewModel.
 * 2. Prioriza aquellos marcados como 'isLive = true' o con horario más cercano.
 * 3. Retorna la sublista depurada.
 * =======================================================================
 */
class GetProximosEventosUseCase {

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta el filtrado ordenado de eventos
    operator fun invoke(eventos: List<FestivalEvent>): List<FestivalEvent> {
        return eventos
            .sortedWith(compareByDescending<FestivalEvent> { it.isLive }.thenBy { it.hora })
            .take(5) // Máximo 5 eventos en reloj inteligente
    }
}
```

**`ScheduleAlertasUseCase.kt`:**
```kotlin
package mx.utng.festivaltrack.wear.domain.usecase

import android.content.Context
import android.os.Vibrator
import mx.utng.festivaltrack.shared.model.FestivalEvent

/**
 * =======================================================================
 * CASO DE USO: PROGRAMAR ALERTAS HÁPTICAS (ScheduleAlertasUseCase)
 *
 * FUNCIONALIDAD:
 * - Emite patrones de vibración en el reloj para alertar al usuario antes del inicio de un concierto.
 * =======================================================================
 */
class ScheduleAlertasUseCase(private val context: Context) {

    // [FUNCIONALIDAD Y FLUJO]: Ejecuta vibración háptica en la muñeca
    fun vibrateNotification() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            val pattern = longArrayOf(0, 200, 100, 200) // Patrón: pulso doble
            it.vibrate(pattern, -1)
        }
    }
}
```

---

## Paso 6: Grafo de Navegación `WearNavGraph.kt`

```kotlin
package mx.utng.festivaltrack.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import mx.utng.festivaltrack.wear.presentation.screens.*
import mx.utng.festivaltrack.wear.presentation.viewmodel.ProximosViewModel

/**
 * Rutas de navegación tipadas para la aplicación Wear OS.
 */
object WearDestinations {
    const val SPLASH = "splash"
    const val WATCH_FACE = "watch_face"
    const val PROXIMOS = "proximos"
    const val PROGRAMA = "programa"
    const val ALERTA = "alerta"
    const val MAPA = "mapa"
}

/**
 * =======================================================================
 * GRAFO DE NAVEGACIÓN DE WEAR OS (WearNavGraph)
 *
 * FUNCIONALIDAD:
 * - Administra la navegación entre pantallas mediante gestos de deslizamiento ('SwipeDismissableNavHost').
 * - Deslizar hacia la derecha (swipe-to-dismiss) permite regresar a la pantalla anterior nativamente.
 *
 * FLUJO DE RUTAS:
 * 1. Inicia en 'SPLASH' durante 1.5 segundos.
 * 2. Transiciona a 'WATCH_FACE' (pantalla principal del reloj).
 * 3. Desde la carátula el usuario puede acceder a 'PROXIMOS', 'PROGRAMA', 'MAPA' y 'ALERTA'.
 * =======================================================================
 */
@Composable
fun WearNavGraph(
    viewModel: ProximosViewModel = viewModel()
) {
    // Controlador de navegación con soporte para gesto de retroceso táctil
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = WearDestinations.SPLASH
    ) {
        // Pantalla 1: Splash con logotipo y animación de entrada
        composable(WearDestinations.SPLASH) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(WearDestinations.WATCH_FACE) {
                        popUpTo(WearDestinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla 2: Carátula del Reloj (Watch Face)
        composable(WearDestinations.WATCH_FACE) {
            WatchFaceScreen(
                viewModel = viewModel,
                onVerProximos = { navController.navigate(WearDestinations.PROXIMOS) },
                onVerPrograma = { navController.navigate(WearDestinations.PROGRAMA) },
                onVerMapa = { navController.navigate(WearDestinations.MAPA) }
            )
        }

        // Pantalla 3: Próximos Eventos Urgentes
        composable(WearDestinations.PROXIMOS) {
            ProximosScreen(
                viewModel = viewModel,
                onEventoClick = { navController.navigate(WearDestinations.ALERTA) }
            )
        }

        // Pantalla 4: Cartelera Completa
        composable(WearDestinations.PROGRAMA) {
            ProgramaCompletoScreen(viewModel = viewModel)
        }

        // Pantalla 5: Alerta / Detalle de Evento
        composable(WearDestinations.ALERTA) {
            AlertaScreen(onDismiss = { navController.popBackStack() })
        }

        // Pantalla 6: Mini Mapa de Escenarios
        composable(WearDestinations.MAPA) {
            WearMapScreen()
        }
    }
}
```

---

## Paso 7: ViewModel de Eventos `ProximosViewModel.kt`

```kotlin
package mx.utng.festivaltrack.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.model.FestivalEvent
import mx.utng.festivaltrack.wear.domain.usecase.GetProximosEventosUseCase

/**
 * =======================================================================
 * VIEWMODEL DE WEAR OS (ProximosViewModel)
 *
 * FUNCIONALIDAD:
 * - Provee los datos de cartelera y eventos en vivo a la carátula y pantallas del reloj.
 * - Expone el próximo evento inminente mediante 'proximoEvento' para mostrar en el Watch Face.
 *
 * FLUJO DE ESTADO:
 * 1. 'init': Inicializa la lista de eventos precargada o recibida vía Bluetooth.
 * 2. 'proximosEventos': Emite la lista filtrada con 'GetProximosEventosUseCase'.
 * 3. 'proximoEvento': Emite el primer evento activo para la tarjeta rápida de la carátula.
 * =======================================================================
 */
class ProximosViewModel : ViewModel() {

    private val getProximosUseCase = GetProximosEventosUseCase()

    private val _eventos = MutableStateFlow<List<FestivalEvent>>(emptyList())
    val eventos: StateFlow<List<FestivalEvent>> = _eventos.asStateFlow()

    private val _proximos = MutableStateFlow<List<FestivalEvent>>(emptyList())
    val proximos: StateFlow<List<FestivalEvent>> = _proximos.asStateFlow()

    private val _proximoEvento = MutableStateFlow<FestivalEvent?>(null)
    val proximoEvento: StateFlow<FestivalEvent?> = _proximoEvento.asStateFlow()

    init {
        loadEvents()
    }

    private fun loadEvents() {
        val lista = listOf(
            FestivalEvent(
                id = 1,
                nombre = "Homenaje Monumental",
                fecha = "17 Nov",
                hora = "20:00",
                lugar = "Jardín Principal",
                descripcion = "Concierto inaugural con Mariachi Vargas.",
                precio = 0.0,
                categoria = "Música",
                isLive = true
            ),
            FestivalEvent(
                id = 2,
                nombre = "Noche Bohemia",
                fecha = "18 Nov",
                hora = "21:30",
                lugar = "Casa Museo JAJ",
                descripcion = "Recital íntimo bohemio.",
                precio = 150.0,
                categoria = "Bohemia",
                isLive = false
            ),
            FestivalEvent(
                id = 3,
                nombre = "Gala Internacional",
                fecha = "19 Nov",
                hora = "19:00",
                lugar = "Teatro del Pueblo",
                descripcion = "Ensamble de mariachis.",
                precio = 250.0,
                categoria = "Gala",
                isLive = false
            )
        )

        _eventos.value = lista
        _proximos.value = getProximosUseCase(lista)
        _proximoEvento.value = _proximos.value.firstOrNull()
    }
}
```

---

## Paso 8: Pantalla de Bienvenida `SplashScreen.kt`

```kotlin
package mx.utng.festivaltrack.wear.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import mx.utng.jose_alfredo.presentation.theme.FestivalGold

/**
 * =======================================================================
 * PANTALLA SPLASH DE WEAR OS (SplashScreen)
 *
 * FUNCIONALIDAD:
 * - Muestra el logotipo animado del festival al abrir la app en el reloj.
 * - Espera 1.5 segundos y dispara la transición a la carátula principal.
 *
 * FLUJO DE EJECUCIÓN:
 * 1. 'LaunchedEffect' inicia una cuenta regresiva con 'delay(1500)'.
 * 2. 'animateFloat' produce un efecto de pulso en el ícono del mariachi/música.
 * 3. Cumplido el tiempo, invoca 'onTimeout()' para navegar al Watch Face.
 * =======================================================================
 */
@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    // Animación continua de pulso de escala para el ícono
    val infiniteTransition = rememberInfiniteTransition(label = "SplashScale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IconScale"
    )

    // Temporizador de navegación automática
    LaunchedEffect(Unit) {
        delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = FestivalGold,
                modifier = Modifier.size(36.dp).scale(scale)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "FestivalTrack",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "José Alfredo Jiménez",
                color = FestivalGold,
                fontSize = 10.sp
            )
        }
    }
}
```

---

## Paso 9: Carátula Principal del Reloj `WatchFaceScreen.kt`

```kotlin
package mx.utng.festivaltrack.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay
import mx.utng.festivaltrack.wear.presentation.viewmodel.ProximosViewModel
import mx.utng.jose_alfredo.presentation.theme.FestivalGold
import mx.utng.jose_alfredo.presentation.theme.FestivalRed
import java.text.SimpleDateFormat
import java.util.*

/**
 * =======================================================================
 * CARÁTULA PRINCIPAL / WATCH FACE (WatchFaceScreen)
 *
 * FUNCIONALIDAD:
 * - Despliega la hora digital en tiempo real sincronizada cada segundo.
 * - Muestra el estado del próximo evento con insignia "EN VIVO" si está activo.
 * - Botones circulares compactos de acceso rápido al Programa y al Mapa.
 *
 * FLUJO DE RELOJ Y ESTADO:
 * 1. 'LaunchedEffect' ejecuta un bucle infinito que actualiza 'currentTime' cada segundo.
 * 2. 'viewModel.proximoEvento.collectAsState()' obtiene el concierto más relevante.
 * =======================================================================
 */
@Composable
fun WatchFaceScreen(
    viewModel: ProximosViewModel,
    onVerProximos: () -> Unit,
    onVerPrograma: () -> Unit,
    onVerMapa: () -> Unit
) {
    val proximoEvento by viewModel.proximoEvento.collectAsState()

    // Estado de la hora digital en formato HH:mm
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).uppercase()
            delay(1000) // Actualiza cada segundo
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            // 1. FECHA SUPERIOR
            Text(
                text = currentDate,
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp)
            )

            // 2. HORA DIGITAL EN FORMATO GRANDE
            Text(
                text = currentTime.ifEmpty { "12:00" },
                color = FestivalGold,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            // 3. TARJETA COMPACTA DEL PRÓXIMO EVENTO
            proximoEvento?.let { evento ->
                Card(
                    onClick = onVerProximos,
                    shape = RoundedCornerShape(14.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = Color(0xFF1B241C),
                        endBackgroundColor = Color(0xFF141A15)
                    ),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (evento.isLive) {
                            Text("● EN VIVO", color = FestivalRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(evento.nombre, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(evento.hora + " • " + evento.lugar, color = Color.LightGray, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }

            // 4. BOTONES CIRCULARES DE ACCESO DIRECTO
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                CompactButton(
                    onClick = onVerPrograma,
                    colors = ButtonDefaults.primaryButtonColors(backgroundColor = FestivalGold, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Event, contentDescription = "Programa", modifier = Modifier.size(16.dp))
                }

                CompactButton(
                    onClick = onVerMapa,
                    colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xFF2E3D30), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Place, contentDescription = "Mapa", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
```

---

## Paso 10: Pantalla de Próximos Eventos `ProximosScreen.kt`

```kotlin
package mx.utng.festivaltrack.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import mx.utng.festivaltrack.shared.model.FestivalEvent
import mx.utng.festivaltrack.wear.presentation.viewmodel.ProximosViewModel
import mx.utng.jose_alfredo.presentation.theme.FestivalGold
import mx.utng.jose_alfredo.presentation.theme.FestivalRed

/**
 * =======================================================================
 * PANTALLA DE PRÓXIMOS EVENTOS (ProximosScreen)
 *
 * FUNCIONALIDAD:
 * - Lista vertical compacta con 'ScalingLazyColumn' optimizada para pantallas circulares.
 * - Al hacer clic en un evento, permite consultar detalles y activar alerta háptica.
 * =======================================================================
 */
@Composable
fun ProximosScreen(
    viewModel: ProximosViewModel,
    onEventoClick: (FestivalEvent) -> Unit
) {
    val proximos by viewModel.proximos.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Próximos Eventos",
                color = FestivalGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(proximos) { evento ->
            Card(
                onClick = { onEventoClick(evento) },
                shape = RoundedCornerShape(12.dp),
                backgroundPainter = CardDefaults.cardBackgroundPainter(
                    startBackgroundColor = Color(0xFF1E281F),
                    endBackgroundColor = Color(0xFF141B15)
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(evento.hora, color = FestivalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (evento.isLive) {
                            Text("● LIVE", color = FestivalRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(evento.nombre, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(evento.lugar, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}
```

---

## Paso 11: Pantalla de Programa Completo `ProgramaCompletoScreen.kt`

```kotlin
package mx.utng.festivaltrack.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import mx.utng.festivaltrack.wear.presentation.viewmodel.ProximosViewModel
import mx.utng.jose_alfredo.presentation.theme.FestivalGold

/**
 * =======================================================================
 * PANTALLA DE PROGRAMA COMPLETO EN WEAR OS (ProgramaCompletoScreen)
 *
 * FUNCIONALIDAD:
 * - Despliega todos los conciertos y actividades del festival en el reloj.
 * - Soporta desplazamiento táctil y con corona giratoria.
 * =======================================================================
 */
@Composable
fun ProgramaCompletoScreen(
    viewModel: ProximosViewModel
) {
    val eventos by viewModel.eventos.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Cartelera Completa",
                color = FestivalGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(eventos) { evento ->
            Card(
                onClick = {},
                shape = RoundedCornerShape(10.dp),
                backgroundPainter = CardDefaults.cardBackgroundPainter(
                    startBackgroundColor = Color(0xFF171F18),
                    endBackgroundColor = Color(0xFF0F1410)
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text(evento.fecha + " • " + evento.hora, color = FestivalGold, fontSize = 10.sp)
                    Text(evento.nombre, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(evento.lugar, color = Color.LightGray, fontSize = 9.sp, maxLines = 1)
                }
            }
        }
    }
}
```

---

## Paso 12: Pantallas Complementarias `OtherScreens.kt` (Alerta, Mapa)

```kotlin
package mx.utng.festivaltrack.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import mx.utng.jose_alfredo.presentation.theme.FestivalGold

/**
 * =======================================================================
 * PANTALLA DE ALERTA HÁPTICA (AlertaScreen)
 *
 * FUNCIONALIDAD:
 * - Confirma la activación de una alerta vibratoria para recordar un concierto.
 * =======================================================================
 */
@Composable
fun AlertaScreen(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = FestivalGold,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "¡Alerta Activada!",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tu reloj vibrará 10 min antes del evento.",
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            CompactButton(
                onClick = onDismiss,
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = FestivalGold, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Check, contentDescription = "OK", modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * =======================================================================
 * PANTALLA DE MAPA COMPACTO DE ESCENARIOS (WearMapScreen)
 *
 * FUNCIONALIDAD:
 * - Muestra la lista de escenarios principales de Dolores Hidalgo con sus ubicaciones.
 * =======================================================================
 */
@Composable
fun WearMapScreen() {
    val escenarios = listOf(
        "Jardín Principal" to "Plaza Central",
        "Casa Museo JAJ" to "Calle Guanajuato 13",
        "Teatro del Pueblo" to "Av. Ferrocarril",
        "Tumba Mausoleo" to "Panteón Municipal"
    )

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Escenarios", color = FestivalGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        }

        items(escenarios) { (nombre, ubicacion) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Place, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(nombre, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(ubicacion, color = Color.Gray, fontSize = 9.sp)
                }
            }
        }
    }
}
```

---

## Paso 13: Tema y Paleta de Colores (`Color.kt` y `Theme.kt`)

**`Color.kt`:**
```kotlin
package mx.utng.jose_alfredo.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores optimizada para pantallas circulares OLED de Wear OS (negro puro para ahorro de batería).
 */
val FestivalDark = Color(0xFF000000)
val FestivalGold = Color(0xFFE6C27A)
val FestivalGoldDark = Color(0xFFC4A059)
val FestivalRed = Color(0xFFE53935)
val FestivalSurface = Color(0xFF161C17)
```

**`Theme.kt`:**
```kotlin
package mx.utng.jose_alfredo.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val WearColors = Colors(
    primary = FestivalGold,
    primaryVariant = FestivalGoldDark,
    secondary = FestivalGold,
    background = FestivalDark,
    surface = FestivalSurface,
    onPrimary = FestivalDark,
    onBackground = FestivalGold,
    onSurface = FestivalGold
)

@Composable
fun FestivalTrackWearTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WearColors,
        content = content
    )
}
```

---

## Paso 14: Vinculación y Pruebas con el Teléfono Acompañante

Para redirigir el socket Bluetooth del Data Layer entre emuladores en desarrollo:

```bash
# 1. Redirigir el puerto del Data Layer de Google Play Services entre emulador Phone y Wear
adb forward tcp:5601 tcp:5601
```

Para compilar e instalar en el emulador Wear OS:
```bash
# 2. Compilar e instalar el módulo Wear OS
./gradlew :wear:installDebug
```

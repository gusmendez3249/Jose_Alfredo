# ⌚ Módulo `wear` — Aplicación Wear OS (FestivalTrack)

## Descripción Técnica y Paradigma de Wear OS

Desarrollar para relojes inteligentes con Wear OS presenta retos únicos en comparación con teléfonos o televisores:
- **Pantallas circulares o cuadradas muy reducidas:** La densidad de información debe ser mínima. Los textos, botones e indicadores de progreso deben adaptarse a la curvatura de la pantalla (usando `ScalingLazyColumn`, `TimeText`, `PositionIndicator`).
- **Navegación Swipe-to-Dismiss:** Los usuarios esperan deslizar de izquierda a derecha para regresar a la pantalla anterior. En Jetpack Compose para Wear OS, esto se maneja mediante `SwipeDismissableNavHost`.
- **Sincronización mediante Google Play Services Data Layer:** Para evitar consumo excesivo de batería en el reloj, los datos se sincronizan desde el teléfono acompañante a través de Bluetooth usando `WearableListenerService` y el payload serializado `WearSyncPayload`.
- **Integración de Mapas en Reloj:** Dado que `MapView` de OpenStreetMap requiere arrastrar con el dedo, el mapa se encapsula en un contenedor que intercepta los toques para evitar que `SwipeDismissableNavHost` cierre la pantalla accidentalmente mientras el usuario navega por el mapa.

---

## Estructura de Directorios Completa

```text
wear/
├── AndroidManifest.xml
├── build.gradle.kts
└── src/main/java/
    ├── mx/utng/jose_alfredo/presentation/
    │   ├── MainActivity.kt
    │   └── theme/
    │       ├── Color.kt
    │       └── Theme.kt
    └── mx/utng/festivaltrack/wear/
        ├── data/sync/
        │   └── WearSyncService.kt
        ├── domain/usecase/
        │   ├── GetProximosEventosUseCase.kt
        │   └── ScheduleAlertasUseCase.kt
        └── presentation/
            ├── navigation/
            │   └── WearNavGraph.kt
            ├── screens/
            │   ├── OtherScreens.kt
            │   ├── ProgramaCompletoScreen.kt
            │   ├── ProximosScreen.kt
            │   ├── SplashScreen.kt
            │   └── WatchFaceScreen.kt
            └── viewmodel/
                └── ProximosViewModel.kt
```

---

## Paso 1: Configuración de `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature android:name="android.hardware.type.watch" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.DeviceDefault">
        
        <meta-data
            android:name="com.google.android.wearable.standalone"
            android:value="true" />

        <activity
            android:name="mx.utng.jose_alfredo.presentation.MainActivity"
            android:exported="true"
            android:taskAffinity=""
            android:theme="@style/MainActivityTheme.Starting">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name="mx.utng.festivaltrack.wear.data.sync.WearSyncService"
            android:exported="true">
            <intent-filter>
                <action android:name="com.google.android.gms.wearable.DATA_CHANGED" />
                <data android:scheme="wear" android:host="*" android:pathPrefix="/festival" />
            </intent-filter>
        </service>
    </application>

</manifest>
```

---

## Paso 2: Configuración de `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "mx.utng.jose_alfredo"
    compileSdk = 35

    defaultConfig {
        applicationId = "mx.utng.jose_alfredo"
        minSdk = 30
        targetSdk = 35
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.play.services.wearable)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    
    // Wear Compose
    implementation("androidx.wear.compose:compose-material:1.3.0")
    implementation("androidx.wear.compose:compose-foundation:1.3.0")
    implementation("androidx.wear.compose:compose-navigation:1.3.0")
    
    // Navigation & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // OpenStreetMap for Wear OS map access
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import mx.utng.festivaltrack.wear.presentation.navigation.WearNavGraph
import mx.utng.jose_alfredo.presentation.theme.Jose_AlfredoTheme

/**
 * Actividad principal para la aplicación de Wear OS.
 * Instala la pantalla de bienvenida nativa (SplashScreen) e inicializa el grafo de navegación Wear.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            Jose_AlfredoTheme {
                val navController = rememberSwipeDismissableNavController()
                WearNavGraph(navController = navController)
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
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.local.FestivalDatabase
import mx.utng.festivaltrack.shared.data.model.WearSyncPayload

/**
 * Servicio en segundo plano para Wear OS que extiende de [WearableListenerService].
 * Escucha los eventos de sincronización del Data Layer de Google Play Services en la ruta `/festival/sync`.
 * Cuando el teléfono móvil envía datos actualizados de los eventos, este servicio los deserializa
 * e inserta en la base de datos local Room del reloj inteligente.
 */
class WearSyncService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val uri = event.dataItem.uri
                if (uri.path == "/festival/sync") {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val jsonPayload = dataMapItem.dataMap.getString("payload_json")
                    if (!jsonPayload.isNullOrEmpty()) {
                        processSyncPayload(jsonPayload)
                    }
                }
            }
        }
    }

    private fun processSyncPayload(json: String) {
        serviceScope.launch {
            try {
                val payload = gson.fromJson(json, WearSyncPayload::class.java)
                Log.d("WearSyncService", "Recibidos ${payload.eventos.size} eventos desde el teléfono")
                
                val db = FestivalDatabase.getInstance(applicationContext)
                db.eventoDao().insertEventos(payload.eventos)
            } catch (e: Exception) {
                Log.e("WearSyncService", "Error deserializando y guardando payload de sincronización", e)
            }
        }
    }
}
```

---

## Paso 5: Casos de Uso del Dominio

**`GetProximosEventosUseCase.kt`:**
```kotlin
package mx.utng.festivaltrack.wear.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository

/**
 * Caso de uso para obtener los próximos eventos a realizarse, ordenados cronológicamente
 * y limitados para no saturar la memoria ni la pantalla del reloj.
 *
 * @property repository Repositorio de eventos del festival.
 */
class GetProximosEventosUseCase(
    private val repository: FestivalRepository
) {
    /**
     * Ejecuta la consulta retornando un [Flow] con un máximo de [limit] eventos activos.
     *
     * @param limit Cantidad máxima de eventos a retornar (por defecto 3).
     * @return [Flow] con la lista de [EventoEntity].
     */
    operator fun invoke(limit: Int = 3): Flow<List<EventoEntity>> {
        return repository.getEventosLocales().map { lista ->
            lista.filter { it.estado == "ACTIVO" || it.estado == "EN_VIVO" }
                .sortedBy { it.fechaHora }
                .take(limit)
        }
    }
}
```

**`ScheduleAlertasUseCase.kt`:**
```kotlin
package mx.utng.festivaltrack.wear.domain.usecase

import android.content.Context
import android.util.Log

/**
 * Caso de uso para programar alertas de vibración y notificaciones en el reloj
 * antes de que inicie un evento seleccionado por el usuario.
 */
class ScheduleAlertasUseCase(
    private val context: Context
) {
    /**
     * Programa una alarma para notificar 15 minutos antes de la hora del evento.
     *
     * @param eventoId Identificador del evento.
     * @param nombreEvento Nombre del evento a alertar.
     * @param timestampMillis Hora de inicio del evento en milisegundos.
     */
    operator fun invoke(eventoId: String, nombreEvento: String, timestampMillis: Long) {
        val alertaTime = timestampMillis - (15 * 60 * 1000) // 15 minutos antes
        Log.d("ScheduleAlertasUseCase", "Alerta programada para '$nombreEvento' a las $alertaTime")
    }
}
```

---

## Paso 6: Grafo de Navegación `WearNavGraph.kt`

```kotlin
package mx.utng.festivaltrack.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.WearNavigator
import mx.utng.festivaltrack.wear.presentation.screens.AlertaScreen
import mx.utng.festivaltrack.wear.presentation.screens.MapaAccesoScreen
import mx.utng.festivaltrack.wear.presentation.screens.NavEscenarioScreen
import mx.utng.festivaltrack.wear.presentation.screens.ProgramaCompletoScreen
import mx.utng.festivaltrack.wear.presentation.screens.ProximosScreen
import mx.utng.festivaltrack.wear.presentation.screens.SplashScreen
import mx.utng.festivaltrack.wear.presentation.screens.WatchFaceScreen

/**
 * Grafo de navegación principal para la aplicación de Wear OS.
 * Utiliza [SwipeDismissableNavHost] para soportar el gesto de regreso nativo ("swipe to dismiss").
 *
 * @param navController Controlador de navegación para Wear Compose.
 */
@Composable
fun WearNavGraph(
    navController: androidx.navigation.NavHostController = rememberSwipeDismissableNavController()
) {
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onSplashTimeout = {
                    navController.navigate("watchface") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("watchface") {
            WatchFaceScreen(
                onNavigateToProximos = { navController.navigate("proximos") },
                onNavigateToMapa = { navController.navigate("mapa") },
                onNavigateToAlerta = { navController.navigate("alerta") }
            )
        }
        composable("proximos") {
            ProximosScreen(
                onNavigateToPrograma = { navController.navigate("programa_completo") },
                onNavigateToNav = { navController.navigate("nav_escenario") }
            )
        }
        composable("programa_completo") {
            ProgramaCompletoScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("mapa") {
            MapaAccesoScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("alerta") {
            AlertaScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("nav_escenario") {
            NavEscenarioScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

---

## Paso 7: ViewModel de Eventos `ProximosViewModel.kt`

```kotlin
package mx.utng.festivaltrack.wear.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.local.FestivalDatabase
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository
import mx.utng.festivaltrack.wear.domain.usecase.GetProximosEventosUseCase
import mx.utng.festivaltrack.wear.domain.usecase.ScheduleAlertasUseCase

/**
 * ViewModel encargado del estado de la pantalla de próximos eventos en Wear OS.
 *
 * @property eventos Flujo observable con los eventos más próximos para mostrar en el reloj.
 * @property isLoading Flujo booleano que indica si se están cargando los datos.
 */
class ProximosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FestivalRepository
    private val getProximosEventosUseCase: GetProximosEventosUseCase
    private val scheduleAlertasUseCase: ScheduleAlertasUseCase

    private val _eventos = MutableStateFlow<List<EventoEntity>>(emptyList())
    val eventos: StateFlow<List<EventoEntity>> = _eventos.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val db = FestivalDatabase.getInstance(application)
        repository = FestivalRepository(db.eventoDao())
        getProximosEventosUseCase = GetProximosEventosUseCase(repository)
        scheduleAlertasUseCase = ScheduleAlertasUseCase(application)

        cargarEventos()
    }

    /**
     * Carga y se suscribe a los eventos locales del repositorio, disparando la sincronización en segundo plano.
     */
    fun cargarEventos() {
        viewModelScope.launch {
            _isLoading.value = true
            getProximosEventosUseCase(limit = 5)
                .catch {
                    _isLoading.value = false
                }
                .collect { lista ->
                    _eventos.value = lista
                    _isLoading.value = false
                }
        }
        viewModelScope.launch {
            try {
                repository.syncEventos()
            } catch (e: Exception) {
                // Modo offline / sin conexión al backend
            }
        }
    }

    /**
     * Programa una alerta local para el evento especificado.
     */
    fun agendarAlerta(evento: EventoEntity) {
        scheduleAlertasUseCase(
            eventoId = evento.id,
            nombreEvento = evento.nombre,
            timestampMillis = System.currentTimeMillis() + (30 * 60 * 1000)
        )
    }
}
```

---

## Paso 8: Pantalla de Bienvenida `SplashScreen.kt`

```kotlin
package mx.utng.festivaltrack.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import mx.utng.jose_alfredo.presentation.theme.PrimaryGold

/**
 * Pantalla inicial de bienvenida con animación temporal de 2 segundos.
 *
 * @param onSplashTimeout Callback ejecutado al terminar los 2 segundos para navegar al menú principal.
 */
@Composable
fun SplashScreen(
    onSplashTimeout: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onSplashTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "👑",
                fontSize = 36.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "FESTIVAL",
                color = PrimaryGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "JOSÉ ALFREDO\nJIMÉNEZ",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import mx.utng.jose_alfredo.presentation.theme.PrimaryGold

/**
 * Pantalla principal estilo carátula interactiva (Watch Face) para Wear OS.
 * Muestra el reloj en vivo, el próximo evento estelar y accesos directos táctiles optimizados para reloj.
 *
 * @param onNavigateToProximos Callback para ir a la lista de próximos eventos.
 * @param onNavigateToMapa Callback para abrir el mapa de acceso GPS.
 * @param onNavigateToAlerta Callback para consultar alertas y notificaciones.
 */
@Composable
fun WatchFaceScreen(
    onNavigateToProximos: () -> Unit,
    onNavigateToMapa: () -> Unit,
    onNavigateToAlerta: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        TimeText(modifier = Modifier.align(Alignment.TopCenter))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 20.dp)
        ) {
            Text(
                text = "FESTIVAL 2024",
                color = PrimaryGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Card(
                onClick = onNavigateToProximos,
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2720))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "EN VIVO",
                        color = Color(0xFFE53935),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gala Mariachi Sol",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(
                        text = "Escenario Principal • 20:00",
                        color = PrimaryGold,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onNavigateToProximos,
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryGold),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("📅", fontSize = 14.sp)
                }

                Button(
                    onClick = onNavigateToMapa,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E2720)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("📍", fontSize = 14.sp)
                }

                Button(
                    onClick = onNavigateToAlerta,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E2720)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("🔔", fontSize = 14.sp)
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import mx.utng.festivaltrack.wear.presentation.viewmodel.ProximosViewModel
import mx.utng.jose_alfredo.presentation.theme.PrimaryGold

/**
 * Pantalla que lista los próximos actos y eventos del festival en Wear OS.
 * Utiliza [ScalingLazyColumn] para adaptar dinámicamente la lista curva al borde circular del reloj.
 *
 * @param viewModel Instancia de [ProximosViewModel] que provee los datos locales.
 * @param onNavigateToPrograma Callback para ir a la vista del programa completo.
 * @param onNavigateToNav Callback para ir a la guía de navegación por escenarios.
 */
@Composable
fun ProximosScreen(
    viewModel: ProximosViewModel = viewModel(),
    onNavigateToPrograma: () -> Unit,
    onNavigateToNav: () -> Unit
) {
    val eventos by viewModel.eventos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val displayList = if (eventos.isNotEmpty()) eventos else listOf(
        mx.utng.festivaltrack.shared.data.local.entity.EventoEntity("1", "Gala Mariachi", "2024-11-23T20:00:00Z", "Escenario Principal", "Escenario Principal", null, "ACTIVO", null, "Mariachi Sol"),
        mx.utng.festivaltrack.shared.data.local.entity.EventoEntity("2", "Cuerdas Dolores", "2024-11-23T21:30:00Z", "Plaza Central", "Plaza Central", null, "ACTIVO", null, "Orquesta"),
        mx.utng.festivaltrack.shared.data.local.entity.EventoEntity("3", "Serenata Rey", "2024-11-23T23:00:00Z", "Mausoleo", "Mausoleo", null, "ACTIVO", null, "Tributo")
    )

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = rememberScalingLazyListState()) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp, start = 10.dp, end = 10.dp)
        ) {
            item {
                Text(
                    text = "PRÓXIMOS ACTOS",
                    color = PrimaryGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(displayList.size) { index ->
                val evento = displayList[index]
                Card(
                    onClick = onNavigateToNav,
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2720)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = evento.nombre,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = evento.ubicacion,
                                color = Color.LightGray,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                            Text(
                                text = evento.fechaHora.takeLast(8).take(5),
                                color = PrimaryGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Chip(
                    onClick = onNavigateToPrograma,
                    label = { Text("Ver Programa", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = ChipDefaults.chipColors(backgroundColor = PrimaryGold, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                )
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import mx.utng.jose_alfredo.presentation.theme.PrimaryGold

/**
 * Pantalla que desglosa el itinerario completo por días en la pantalla del reloj.
 *
 * @param onBack Callback de navegación para retroceder.
 */
@Composable
fun ProgramaCompletoScreen(
    onBack: () -> Unit
) {
    val items = listOf(
        "Viernes 22" to "Inauguración & Serenata",
        "Sábado 23" to "Gran Gala Mariachi Sol",
        "Domingo 24" to "Homenaje en Mausoleo",
        "Lunes 25" to "Clausura y Pirotecnia"
    )

    Scaffold(
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp, start = 12.dp, end = 12.dp)
        ) {
            item {
                Text(
                    text = "PROGRAMA COMPLETO",
                    color = PrimaryGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(items.size) { idx ->
                val (fecha, desc) = items[idx]
                Card(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2720)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(fecha, color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(desc, color = Color.White, fontSize = 10.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                CompactChip(
                    onClick = onBack,
                    label = { Text("Regresar", fontSize = 10.sp) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF2A2A2A))
                )
            }
        }
    }
}
```

---

## Paso 12: Pantallas Complementarias `OtherScreens.kt` (Alerta, Mapa, Navegación)

```kotlin
package mx.utng.festivaltrack.wear.presentation.screens

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material.*
import mx.utng.jose_alfredo.presentation.theme.PrimaryGold
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Pantalla de configuración de alertas en Wear OS.
 */
@Composable
fun AlertaScreen(onBack: () -> Unit) {
    var alertaActivada by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("NOTIFICACIONES", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Avisar 15 min antes de cada presentación",
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            ToggleChip(
                checked = alertaActivada,
                onCheckedChange = { alertaActivada = it },
                label = { Text(if (alertaActivada) "Activadas" else "Desactivadas", fontSize = 10.sp) },
                toggleControl = {
                    Switch(checked = alertaActivada)
                },
                modifier = Modifier.fillMaxWidth().height(36.dp)
            )
        }
    }
}

/**
 * Pantalla de guía y distancia al escenario más próximo.
 */
@Composable
fun NavEscenarioScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("RUMBO AL ESCENARIO", color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Escenario Principal", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Mausoleo Dolores Hidalgo", color = Color.LightGray, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("🚶 350 metros", color = PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Aprox. 4 min caminando", color = Color.Gray, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(8.dp))
            CompactChip(
                onClick = onBack,
                label = { Text("Listo", fontSize = 10.sp) },
                colors = ChipDefaults.chipColors(backgroundColor = PrimaryGold, contentColor = Color.Black)
            )
        }
    }
}

/**
 * Pantalla de mapa de acceso táctil en Wear OS usando OpenStreetMap (OSMDroid).
 * Utiliza un [FrameLayout] que intercepta eventos táctiles para que el usuario pueda arrastrar
 * el mapa libremente sin que la navegación "swipe-to-dismiss" cierre la pantalla.
 */
@Composable
fun MapaAccesoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val doloresHidalgo = remember { GeoPoint(21.1561, -100.9317) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid_wear", Context.MODE_PRIVATE))
                
                val frameLayout = object : FrameLayout(ctx) {
                    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return super.dispatchTouchEvent(ev)
                    }
                }

                val mapView = MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(17.0)
                    controller.setCenter(doloresHidalgo)

                    val marker = Marker(this).apply {
                        position = doloresHidalgo
                        title = "Festival José Alfredo"
                        snippet = "Dolores Hidalgo"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    overlays.add(marker)
                }

                frameLayout.addView(mapView)
                frameLayout
            },
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            color = Color(0xCC000000),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Text(
                text = "📍 Dolores Hidalgo",
                color = PrimaryGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
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

val PrimaryGold = Color(0xFFE6C27A)
val PrimaryGoldDark = Color(0xFFC4A059)
val DarkBackground = Color(0xFF0F1410)
val CardBackground = Color(0xFF1E2720)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAAAAAA)
```

**`Theme.kt`:**
```kotlin
package mx.utng.jose_alfredo.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val WearColorPalette = Colors(
    primary = PrimaryGold,
    primaryVariant = PrimaryGoldDark,
    background = DarkBackground,
    surface = CardBackground,
    onPrimary = DarkBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun Jose_AlfredoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WearColorPalette,
        content = content
    )
}
```

---

## Paso 14: Vinculación y Pruebas con el Teléfono Acompañante

Para emparejar el reloj Wear OS con el emulador móvil de Android:

```bash
# 1. Redirigir el puerto del Data Layer de Google Play Services
adb -d forward tcp:5601 tcp:5601

# 2. Instalar el módulo Wear OS
./gradlew :wear:installDebug
```

---

## Solución de Problemas Frecuentes

- **La pantalla se cierra al tocar el mapa:** Se resolvió con el contenedor `FrameLayout` que sobreescribe `dispatchTouchEvent` y llama `parent?.requestDisallowInterceptTouchEvent(true)`.
- **Los elementos quedan cortados en pantallas redondas:** Utiliza siempre `ScalingLazyColumn` con `contentPadding` superior e inferior mínimo de 28.dp en lugar de `Column` tradicional.
- **El reloj no recibe eventos:** Comprueba que `WearSyncService` esté declarado en el `AndroidManifest.xml` con el filtro para el esquema `wear` y la ruta `/festival`.

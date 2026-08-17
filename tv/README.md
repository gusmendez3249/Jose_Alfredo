# 📺 Módulo `tv` — Aplicación Android TV (FestivalTrack)

## Descripción Técnica y Paradigma de Android TV

Desarrollar para Android TV es un paradigma radicalmente diferente al desarrollo móvil tradicional. Las principales diferencias son:
- **Navegación basada en D-Pad:** A diferencia de los móviles que dependen del "touch", en Android TV el usuario utiliza un control remoto con flechas direccionales (Arriba, Abajo, Izquierda, Derecha) y un botón de selección (OK/Enter). Esto requiere un manejo exhaustivo del "foco" en los elementos de la interfaz.
- **Pantallas grandes (10-foot UI):** Los elementos visuales, tipografías y el espaciado deben estar optimizados para ser leídos desde una distancia aproximada de 3 metros (10 pies).
- **Interacción indirecta:** No existen gestos de deslizamiento directo sobre componentes. Si un menú lateral o un cuadro de texto necesita interacción, este debe solicitar proactivamente el foco o mostrar diálogos accesibles.

---

## Estructura de Directorios Completa

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

1. Abre el **Device Manager** en Android Studio.
2. Selecciona **Create Device** -> Categoría **TV**.
3. Selecciona el perfil **Television (1080p)**.
4. Elige una imagen del sistema con **API 30 o superior** (Android 11+).
5. Inicia el emulador y asegúrate de que responde correctamente a los botones de flecha del teclado (simulando el D-Pad).

---

## Paso 2: Configuración de `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <uses-feature
        android:name="android.software.leanback"
        android:required="true" />
    <uses-feature
        android:name="android.hardware.touchscreen"
        android:required="false" />

    <application
        android:allowBackup="true"
        android:label="Festival Track TV"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar"
        android:usesCleartextTraffic="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="Festival Track TV"
            android:theme="@android:style/Theme.Material.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

---

## Paso 3: Configuración de `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "mx.utng.festivaltrack.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "mx.utng.festivaltrack.tv"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.core:core-ktx:1.12.0")

    // Compose
    implementation("androidx.compose.ui:ui:1.6.1")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material:material-icons-core:1.6.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.1")

    // Navigation & Lifecycle
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Image Loading (Coil)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Media3 (ExoPlayer)
    val media3Version = "1.2.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
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
 * Actividad principal y punto de entrada para la aplicación en Android TV.
 *
 * Configura la interfaz de usuario con Jetpack Compose y maneja la navegación básica
 * a través de un estado simple (`currentScreenIndex`).
 */
class MainActivity : ComponentActivity() {

    private val viewModel: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FestivalTrackTvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FestivalDarkBg
                ) {
                    val eventos by viewModel.eventos.collectAsState()
                    var currentScreenIndex by remember { mutableStateOf(0) }
                    var isLoggedIn by remember { mutableStateOf(false) }

                    if (!isLoggedIn) {
                        TvLoginScreen(
                            onLoginSuccess = { isLoggedIn = true }
                        )
                    } else {
                        when (currentScreenIndex) {
                            0 -> TvMainScreen(
                                eventos = eventos,
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it },
                                onVerEnVivo = { currentScreenIndex = 2 },
                                onComprarBoletos = { /* Open ticket dialog */ }
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
                                onLogout = { isLoggedIn = false }
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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.ui.theme.FestivalGold

/**
 * Componente [Composable] para los elementos del menú en el Sidebar de la interfaz para Android TV.
 * Renderiza el ícono, el texto y reacciona al enfoque (D-Pad) cambiando el color y el fondo.
 *
 * @param label El texto a mostrar para este elemento del menú.
 * @param icon El [ImageVector] que representa a este elemento de forma visual.
 * @param isSelected Indica si este elemento es el que está actualmente activo/seleccionado.
 * @param onClick La función que se ejecutará al pulsar "OK" en el D-Pad sobre este elemento.
 */
@Composable
fun SidebarMenuItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) FestivalGold else if (isFocused) Color.White.copy(alpha = 0.1f) else Color.Transparent,
        contentColor = if (isSelected) Color.Black else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.Black else if (isFocused) FestivalGold else Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
```

---

## Paso 6: ViewModel con Polling `TvViewModel.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.local.FestivalDatabase
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository

/**
 * ViewModel central para la app en Android TV.
 * Gestiona el estado y la sincronización de los eventos mostrados en el televisor.
 *
 * @property eventos Flujo continuo con la lista de eventos actualizados.
 * @constructor Crea el ViewModel iniciando la base de datos de Room y empezando el polling periódico.
 */
class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FestivalRepository
    private val _eventos = MutableStateFlow<List<EventoEntity>>(emptyList())
    val eventos: StateFlow<List<EventoEntity>> = _eventos.asStateFlow()

    init {
        val database = FestivalDatabase.getInstance(application)
        repository = FestivalRepository(database.eventoDao())

        viewModelScope.launch {
            repository.getEventosLocales().collectLatest { list ->
                _eventos.value = list
            }
        }

        // Sync with backend API immediately and poll every 5 seconds for real-time updates
        viewModelScope.launch {
            while (true) {
                try {
                    repository.syncEventos()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    /**
     * Sincroniza explícitamente los eventos desde el backend hacia la base de datos local (Room).
     */
    fun sync() {
        viewModelScope.launch {
            try {
                repository.syncEventos()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
```

---

## Paso 7: Pantalla Principal `TvMainScreen.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.tv.R
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * Pantalla principal (Home) de la aplicación para Android TV.
 *
 * Muestra el panel lateral (sidebar) izquierdo para navegación principal y
 * un área central (main content) con un hero banner y un carrusel (`LazyRow`) de próximos eventos.
 * Todo el layout está optimizado para navegación con D-Pad, usando [onFocusChanged] en las tarjetas
 * para resaltar visualmente el elemento actual.
 *
 * @param eventos Lista de eventos actual proveniente del ViewModel (Room).
 * @param currentNavIndex Índice actual de la opción seleccionada en el Sidebar.
 * @param onNavSelect Callback al seleccionar un elemento en el Sidebar.
 * @param onVerEnVivo Callback disparado al hacer clic en el botón "Ver en Vivo" del banner principal.
 * @param onComprarBoletos Callback disparado al pulsar el botón "Comprar Boletos".
 */
@Composable
fun TvMainScreen(
    eventos: List<EventoEntity>,
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit,
    onVerEnVivo: () -> Unit,
    onComprarBoletos: () -> Unit
) {
    var activeDialogText by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
    ) {
        // ------------------ LEFT SIDEBAR ------------------
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(FestivalSidebarBg)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Festival 2024",
                    color = FestivalGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Dolores Hidalgo, Cuna de la Independencia",
                    color = FestivalTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(32.dp))

                val navItems = listOf(
                    "Inicio" to Icons.Default.Home,
                    "Galería Histórica" to Icons.Default.Collections,
                    "Transmisión En Vivo" to Icons.Default.LiveTv,
                    "Programación" to Icons.Default.Event,
                    "Ajustes" to Icons.Default.Settings
                )

                navItems.forEachIndexed { index, (label, icon) ->
                    val isSelected = currentNavIndex == index
                    SidebarMenuItem(
                        label = label,
                        icon = icon,
                        isSelected = isSelected,
                        onClick = { onNavSelect(index) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Bottom CTA Button in Sidebar
            Button(
                onClick = onComprarBoletos,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FestivalGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("COMPRAR BOLETOS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ------------------ MAIN CONTENT AREA ------------------
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = FestivalTextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BUSCAR ARTISTA", color = FestivalTextSecondary, fontSize = 14.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("DOLORES HIDALGO 19:42", color = FestivalTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(FestivalCardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = FestivalGold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // HERO SECTION CARD with Local Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_dolores_hidalgo),
                    contentDescription = "Hero Festival",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.95f), Color.Black.copy(alpha = 0.5f))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "FESTIVAL REGIONAL MEXICANO",
                                color = FestivalGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "La Voz del Pueblo:\nHomenaje a José Alfredo Jiménez",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 28.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sintoniza en vivo desde Dolores Hidalgo, la Cuna de la Independencia. Vive una noche de gala con los mejores intérpretes.",
                                color = FestivalTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onVerEnVivo,
                                colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ver en Vivo", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { activeDialogText = "Sintonizando la transmisión en vivo del Festival José Alfredo Jiménez." },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color.White, Color.White))),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Detalles")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CAROUSEL SECTION: PRÓXIMOS EN EL ESCENARIO
            Text(
                text = "PRÓXIMOS EN EL ESCENARIO",
                color = FestivalGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            val displayList = if (eventos.isNotEmpty()) eventos else listOf(
                EventoEntity("1", "Voz del Mariachi", "2020-11-20T20:00:00Z", "Escenario Principal", "Escenario Principal", null, "ACTIVO", null, "Mariachi Sol"),
                EventoEntity("2", "Cuerdas de Dolores", "2020-11-20T21:30:00Z", "Plaza Principal", "Plaza Principal", null, "ACTIVO", null, "Orquesta"),
                EventoEntity("3", "Cena de Gala", "2020-11-20T23:00:00Z", "Jardín Histórico", "Jardín Histórico", null, "ACTIVO", null, "Tributo")
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(displayList) { evento ->
                    TvEventCard(evento = evento, onClick = { activeDialogText = "Evento: ${evento.nombre}\nLugar: ${evento.ubicacion}\nHora: ${evento.fechaHora.takeLast(8).take(5)}" })
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Navigation Hint at bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "→ USA EL CONTROL PARA NAVEGAR",
                    color = FestivalTextSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    if (activeDialogText != null) {
        AlertDialog(
            onDismissRequest = { activeDialogText = null },
            title = { Text("Smart TV Festival", color = FestivalGold, fontWeight = FontWeight.Bold) },
            text = { Text(activeDialogText!!, color = Color.White) },
            confirmButton = {
                Button(
                    onClick = { activeDialogText = null },
                    colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)
                ) {
                    Text("CERRAR", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = FestivalCardBg
        )
    }
}

/**
 * Componente que representa una tarjeta de evento en el carrusel horizontal.
 * Gestiona su propio estado de foco ([onFocusChanged]) para dibujar un borde dorado
 * y cambiar el color de fondo al ser seleccionado con el D-Pad del control remoto.
 *
 * @param evento El [EventoEntity] con la información a mostrar en la tarjeta.
 * @param onClick La acción a ejecutar al pulsar el botón de OK en el control.
 */
@Composable
fun TvEventCard(evento: EventoEntity, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF2E3D30) else FestivalCardBg
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(220.dp)
            .height(130.dp)
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) FestivalGold else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.mariachi_gala_stage),
                contentDescription = evento.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = FestivalGold,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = evento.fechaHora.takeLast(8).take(5).ifEmpty { "20:00 HRS" },
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = evento.nombre,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = evento.ubicacion,
                        color = FestivalTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
```

---

## Paso 8: Pantalla de Streaming RTSP y Chat `TvLiveStreamScreen.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.*

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.launch

/**
 * Representa un mensaje dentro del chat comunitario de la Smart TV.
 *
 * @property user Nombre del remitente.
 * @property text Texto del mensaje.
 * @property time Etiqueta de tiempo (ej. "Hace 2m" o "En vivo").
 * @property isAdmin Indica si el mensaje proviene del administrador (para resaltado).
 */
data class ChatMessage(val user: String, val text: String, val time: String, val isAdmin: Boolean = false)

/**
 * Pantalla de Transmisión en Vivo para Smart TV ([TvLiveStreamScreen]).
 *
 * Esta pantalla integra el flujo en vivo del festival adaptado a pantallas grandes.
 * Layout estructurado en:
 * - **Reproductor RTSP (68%)**: Reproductor principal implementado con [ExoPlayer] (Media3) para
 *   consumir una señal de video en tiempo real (RTSP) directamente desde el emulador o red local.
 * - **Panel de Chat (32%)**: Barra lateral que muestra mensajes en vivo. Utiliza una técnica de
 *   polling cada 3 segundos para sincronizar la lista de mensajes con el backend mediante HTTP.
 *
 * Para interactuar en el chat usando un control remoto, cuenta con un área especial navegable
 * con el D-Pad. Al presionarse ("OK"), invoca el [TvChatInputDialog] que levanta el teclado en pantalla.
 *
 * @param currentNavIndex Índice de la opción de navegación actualmente seleccionada.
 * @param onNavSelect Callback para cambiar de pantalla al usar el menú lateral.
 */
@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun TvLiveStreamScreen(
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit
) {
    var showChatDialog by remember { mutableStateOf(false) }
    var isPlayingStream by remember { mutableStateOf(true) }
    val chatFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    // Cliente HTTP para enviar y consultar mensajes al backend
    val api = remember { mx.utng.festivaltrack.shared.data.remote.FestivalApiService.create() }
    val coroutineScope = rememberCoroutineScope()

    // Lista reactiva de mensajes de chat mostrados en pantalla
    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage("Miguel Angel", "¡Qué bonita es mi tierra! Saludos desde Chicago.", "Hace 2m"),
            ChatMessage("Elena Jimenez", "La interpretación de Camino de Guanajuato espectacular.", "Hace 1m"),
            ChatMessage("Admin Festival", "¡Bienvenidos a la transmisión oficial en Smart TV!", "Hace 4m", isAdmin = true)
        )
    }

    // Diálogo de chat (D-Pad accesible)
    if (showChatDialog) {
        TvChatInputDialog(
            onDismiss = { showChatDialog = false },
            onSend = { msgText ->
                if (msgText.isNotBlank()) {
                    chatMessages.add(ChatMessage("Espectador TV", msgText, "Ahora"))
                    coroutineScope.launch {
                        try {
                            api.sendChatMessage(
                                mx.utng.festivaltrack.shared.data.remote.ChatMessageDto(
                                    eventoId = "EVT-001",
                                    usuarioNombre = "Espectador TV",
                                    mensaje = msgText
                                )
                            )
                        } catch (e: Exception) { /* red */ }
                    }
                }
            }
        )
    }
    var currentStreamUrl by remember { mutableStateOf("") }
    var isLive by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }
    val fallbackDemoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"

    // ExoPlayer — se recrea cuando cambia la URL del stream
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    coroutineScope.launch {
                        if (isLive && retryCount < 2) {
                            retryCount++
                            kotlinx.coroutines.delay(1500)
                            prepare()
                            playWhenReady = true
                        } else if (isLive && retryCount >= 2 && currentStreamUrl != fallbackDemoUrl) {
                            // Si RTSP falla por red de emuladores, cambiar a stream demo en vivo
                            currentStreamUrl = fallbackDemoUrl
                            setMediaItem(androidx.media3.common.MediaItem.fromUri(fallbackDemoUrl))
                            prepare()
                            playWhenReady = true
                        }
                    }
                }
            })
        }
    }

    // Función para cargar/cambiar la URL en el reproductor
    fun loadStream(url: String) {
        if (url.isNotBlank()) {
            retryCount = 0
            try {
                if (url.startsWith("rtsp://")) {
                    val mediaSource = androidx.media3.exoplayer.rtsp.RtspMediaSource.Factory()
                        .setForceUseRtpTcp(true)
                        .createMediaSource(androidx.media3.common.MediaItem.fromUri(url))
                    exoPlayer.setMediaSource(mediaSource)
                } else {
                    val mediaItem = androidx.media3.common.MediaItem.fromUri(url)
                    exoPlayer.setMediaItem(mediaItem)
                }
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } catch (e: Exception) {
                val mediaItem = androidx.media3.common.MediaItem.fromUri(url)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        } else {
            exoPlayer.stop()
        }
    }

    // Liberar recursos del reproductor al abandonar la pantalla
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Polling del stream: cada 3s consulta si el admin inició el live y mantiene reproduciendo
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val status = api.getStreamStatus()
                val urlParaTV = if (status.emulatorUrl.isNotBlank()) status.emulatorUrl else status.streamUrl
                
                isLive = status.isLive
                
                if (status.isLive && urlParaTV.isNotBlank()) {
                    val isNewStream = urlParaTV != currentStreamUrl
                    val isIdleOrError = exoPlayer.playbackState == androidx.media3.common.Player.STATE_IDLE || exoPlayer.playerError != null
                    
                    if (isNewStream || isIdleOrError) {
                        currentStreamUrl = urlParaTV
                        loadStream(currentStreamUrl)
                    }
                } else if (!status.isLive) {
                    currentStreamUrl = ""
                    if (exoPlayer.isPlaying) {
                        exoPlayer.stop()
                    }
                }
            } catch (e: Exception) {
                // Falla silenciosa
            }
            kotlinx.coroutines.delay(3000)
        }
    }

    // Polling del chat: cada 3s carga mensajes reales del backend
    LaunchedEffect(Unit) {
        while (true) {
            try {
                // Intentar obtener el eventoId del primer evento activo, o usar EVT-001 por defecto
                val msgs = api.getChatMessages("EVT-001")
                if (msgs.isNotEmpty()) {
                    chatMessages.clear()
                    msgs.forEach { m ->
                        chatMessages.add(
                            ChatMessage(
                                user = m.usuarioNombre,
                                text = m.mensaje,
                                time = "En vivo",
                                isAdmin = m.esAdmin
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Falla silenciosa en polling de chat
            }
            kotlinx.coroutines.delay(3000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
    ) {
        // SIDEBAR MENÚ LATERAL (Navegación con D-Pad)
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .background(FestivalSidebarBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Festival 2024",
                    color = FestivalGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                val navItems = listOf(
                    "Inicio" to Icons.Default.Home,
                    "Galería Histórica" to Icons.Default.Collections,
                    "Transmisión En Vivo" to Icons.Default.Tv,
                    "Programación" to Icons.Default.Event,
                    "Ajustes" to Icons.Default.Settings
                )

                navItems.forEachIndexed { index, (label, icon) ->
                    SidebarMenuItem(
                        label = label,
                        icon = icon,
                        isSelected = currentNavIndex == index,
                        onClick = { onNavSelect(index) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        // ÁREA DEL REPRODUCTOR EN VIVO (68% del ancho)
        Column(
            modifier = Modifier
                .weight(0.68f)
                .fillMaxHeight()
                .padding(20.dp)
        ) {
            // Reproductor ExoPlayer de Android
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false // Controles personalizados en Compose
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay: "Sin señal" cuando no hay stream activo
                if (!isLive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A0A0A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(72.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Sin transmisión activa", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("El administrador aún no ha iniciado el live", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                            Surface(
                                color = Color(0xFF1A2118),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "⏳ Actualizando cada 5 segundos...",
                                    color = FestivalGold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Festival José Alfredo Jiménez",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = FestivalBadgeLive,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("LIVE NOW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("1251 PERSONAS VIENDO", color = Color.White, fontSize = 10.sp)
                        }
                    }

                    // Título y botones de acción
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Serenata de Gala: Mausoleo Dolores Hidalgo",
                            color = FestivalGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "En vivo desde la Cuna de la Independencia Nacional. Un tributo al Rey de la Canción Ranchera.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    isPlayingStream = !isPlayingStream
                                    if (isPlayingStream) exoPlayer.play() else exoPlayer.pause()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(if (isPlayingStream) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPlayingStream) "PAUSAR EN VIVO" else "REANUDAR", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("VER PROGRAMA", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // PANEL DE CHAT LATERAL (32% del ancho)
        Column(
            modifier = Modifier
                .weight(0.32f)
                .fillMaxHeight()
                .background(FestivalSidebarBg)
                .padding(20.dp)
        ) {
            Text(
                text = "Comunidad en Vivo",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Escribe un mensaje para participar en la pantalla", color = FestivalTextSecondary, fontSize = 10.sp)

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de mensajes enviada por usuarios
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(chatMessages) { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isAdmin) Color(0xFF2E2415) else FestivalCardBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(msg.user, color = FestivalGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(msg.time, color = FestivalTextSecondary, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(msg.text, color = Color.White, fontSize = 11.sp, lineHeight = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Entrada de Chat adaptada a Smart TV (abrir diálogo con D-Pad)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(chatFocusRequester)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showChatDialog = true }
                        .background(FestivalCardBg, RoundedCornerShape(8.dp))
                        .border(1.dp, FestivalGold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Pulsa OK para escribir un mensaje...",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showChatDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FestivalGold)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.Black)
                }
            }
        }
    }
}

/**
 * Diálogo modal para escribir texto en Smart TV ([TvChatInputDialog]).
 * Despliega un [OutlinedTextField] que solicita el foco del teclado inmediatamente al abrirse.
 *
 * @param onDismiss Callback para cerrar el diálogo.
 * @param onSend Callback ejecutado al presionar Enviar con el mensaje escrito.
 */
@Composable
fun TvChatInputDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A2118),
        title = {
            Text("Escribe tu mensaje", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Tu mensaje aquí...", color = Color.Gray, fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0D1210),
                    unfocusedContainerColor = Color(0xFF0D1210),
                    focusedBorderColor = FestivalGold,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = FestivalGold
                ),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSend(text)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FestivalGold,
                    contentColor = Color.Black
                )
            ) {
                Text("Enviar", fontWeight = FontWeight.Bold)
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

---

## Paso 9: Pantalla de Galería Histórica `TvGalleryScreen.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.R
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * Elemento de datos que representa una imagen en la galería histórica.
 *
 * @property id Identificador único.
 * @property title Título de la obra o fotografía.
 * @property category Categoría (ej. "Primeros Años", "Época de Oro").
 * @property drawableId Recurso estático de la imagen local.
 * @property isHighlighted Indica si el elemento debe ocupar más espacio/destacar.
 */
data class GalleryItem(
    val id: String,
    val title: String,
    val category: String,
    val drawableId: Int,
    val isHighlighted: Boolean = false
)

/**
 * Pantalla que muestra una galería de fotos e historia en un formato de grilla.
 * Permite filtrar por categorías (pestañas) usando los botones superiores.
 * Cada elemento de la grilla es navegable usando las flechas direccionales del control remoto.
 *
 * @param currentNavIndex Índice actual de la navegación en el menú lateral.
 * @param onNavSelect Callback para manejar la navegación del sidebar.
 */
@Composable
fun TvGalleryScreen(
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var activeDialogText by remember { mutableStateOf<String?>(null) }
    val tabs = listOf("Todo", "Primeros Años", "Época de Oro", "Legado")

    val allGalleryItems = remember {
        listOf(
            GalleryItem(
                "1",
                "Concierto en la Cuna de la Independencia",
                "Legado",
                R.drawable.hero_dolores_hidalgo,
                isHighlighted = true
            ),
            GalleryItem(
                "2",
                "El Atuendo Charro",
                "Época de Oro",
                R.drawable.jose_alfredo_portrait
            ),
            GalleryItem(
                "3",
                "La Voz del Pueblo",
                "Primeros Años",
                R.drawable.mariachi_gala_stage
            ),
            GalleryItem(
                "4",
                "Composición & Guitarra",
                "Época de Oro",
                R.drawable.ranchera_guitar
            ),
            GalleryItem(
                "5",
                "Dolores Hidalgo, 1954",
                "Primeros Años",
                R.drawable.hero_dolores_hidalgo
            ),
            GalleryItem(
                "6",
                "La Guitarra del Rey",
                "Legado",
                R.drawable.ranchera_guitar
            )
        )
    }

    val filteredItems = remember(selectedTab) {
        if (selectedTab == 0) allGalleryItems
        else allGalleryItems.filter { it.category == tabs[selectedTab] }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
    ) {
        // SIDEBAR
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(FestivalSidebarBg)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Festival 2024", color = FestivalGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("DOLORES HIDALGO", color = FestivalTextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(32.dp))

                val navItems = listOf(
                    "Inicio" to Icons.Default.Home,
                    "Galería Histórica" to Icons.Default.Collections,
                    "Transmisión En Vivo" to Icons.Default.LiveTv,
                    "Programación" to Icons.Default.Event,
                    "Ajustes" to Icons.Default.Settings
                )

                navItems.forEachIndexed { index, (label, icon) ->
                    SidebarMenuItem(
                        label = label,
                        icon = icon,
                        isSelected = currentNavIndex == index,
                        onClick = { onNavSelect(index) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // MAIN CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
        ) {
            // Header & Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Galería Histórica",
                    color = FestivalGold,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEachIndexed { idx, tabTitle ->
                        val isSelected = selectedTab == idx
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTab = idx },
                            label = { Text(tabTitle) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FestivalGold,
                                selectedLabelColor = Color.Black,
                                containerColor = FestivalCardBg,
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Grid of Gallery Items with Local Drawables
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems) { item ->
                    GalleryCard(item = item, onClick = { activeDialogText = "Visualizando pieza histórica: '${item.title}' (${item.category})" })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action Card (Explora el Legado)
            Card(
                colors = CardDefaults.cardColors(containerColor = FestivalCardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Explora el Legado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Usa el control para navegar por las fotografías históricas", color = FestivalTextSecondary, fontSize = 11.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { activeDialogText = "Reproduciendo Documental Histórico de José Alfredo Jiménez en Smart TV." },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ver Documental", color = Color.White)
                        }

                        Button(
                            onClick = { activeDialogText = "Pase VIP Activado para la gala en Dolores Hidalgo." },
                            colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Obtener Pase VIP", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (activeDialogText != null) {
        AlertDialog(
            onDismissRequest = { activeDialogText = null },
            title = { Text("Galería Histórica Smart TV", color = FestivalGold, fontWeight = FontWeight.Bold) },
            text = { Text(activeDialogText!!, color = Color.White) },
            confirmButton = {
                Button(
                    onClick = { activeDialogText = null },
                    colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = FestivalCardBg
        )
    }
}

/**
 * Componente interactivo (focusable) para mostrar una imagen de la galería.
 *
 * Cuando obtiene el foco ([Modifier.onFocusChanged]), se añade un borde dorado
 * para que el usuario sepa dónde está ubicado.
 *
 * @param item El [GalleryItem] que provee datos y la imagen a mostrar.
 * @param onClick Acción que se ejecuta al pulsar el botón principal sobre la tarjeta.
 */
@Composable
fun GalleryCard(item: GalleryItem, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF2E3D30) else FestivalCardBg
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(if (item.isHighlighted) 160.dp else 130.dp)
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) FestivalGold else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Local Drawable Background Image
            Image(
                painter = painterResource(id = item.drawableId),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark Gradient Overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    if (item.isHighlighted) {
                        Surface(
                            color = FestivalGold,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "DESTACADO",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.category,
                        color = FestivalGold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
```

---

## Paso 10: Pantalla de Horarios y Cartelera `TvScheduleScreen.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * Representa un evento en la programación del festival.
 *
 * @property time Hora del evento.
 * @property title Nombre del evento.
 * @property stage Escenario o ubicación.
 * @property artist Artista o banda.
 * @property isCurrent Indica si el evento está sucediendo en este momento.
 */
data class ScheduleItem(val time: String, val title: String, val stage: String, val artist: String, val isCurrent: Boolean = false)

/**
 * Pantalla que muestra la programación completa (horarios) del festival.
 * Utiliza un `LazyColumn` para listar los eventos y es completamente navegable vía D-Pad.
 *
 * @param eventos Lista de eventos obtenida del [TvViewModel] proveniente de Room.
 * @param currentNavIndex Índice actual de la navegación en el menú lateral.
 * @param onNavSelect Callback para manejar la navegación del sidebar.
 */
@Composable
fun TvScheduleScreen(
    eventos: List<mx.utng.festivaltrack.shared.data.local.entity.EventoEntity> = emptyList(),
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit
) {
    var activeDialogText by remember { mutableStateOf<String?>(null) }

    val scheduleList = remember(eventos) {
        if (eventos.isNotEmpty()) {
            eventos.mapIndexed { index, e ->
                val formattedTime = if (e.fechaHora.contains("T")) {
                    e.fechaHora.substringAfter("T").take(5) + " HRS"
                } else {
                    e.fechaHora
                }
                ScheduleItem(
                    time = formattedTime,
                    title = e.nombre,
                    stage = e.escenario ?: e.ubicacion,
                    artist = e.artistaNombre ?: "Mariachi & Artistas Invitados",
                    isCurrent = index == 0
                )
            }
        } else {
            listOf(
                ScheduleItem("18:00 HRS", "Serenata de Bienvenida", "Mausoleo José Alfredo", "Mariachi Femenil", isCurrent = false),
                ScheduleItem("19:30 HRS", "Gran Gala Mariachi", "Escenario Principal", "Mariachi Sol de México", isCurrent = true),
                ScheduleItem("21:00 HRS", "Homenaje Cuerdas de Dolores", "Teatro del Pueblo", "Orquesta Guanajuato", isCurrent = false),
                ScheduleItem("22:30 HRS", "Cierre Estelar: El Rey", "Escenario Principal", "Voces Magistrales", isCurrent = false)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
    ) {
        // SIDEBAR
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(FestivalSidebarBg)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Festival 2024", color = FestivalGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("DOLORES HIDALGO", color = FestivalTextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(32.dp))

                val navItems = listOf(
                    "Inicio" to Icons.Default.Home,
                    "Galería Histórica" to Icons.Default.Collections,
                    "Transmisión En Vivo" to Icons.Default.LiveTv,
                    "Programación" to Icons.Default.Event,
                    "Ajustes" to Icons.Default.Settings
                )

                navItems.forEachIndexed { index, (label, icon) ->
                    SidebarMenuItem(
                        label = label,
                        icon = icon,
                        isSelected = currentNavIndex == index,
                        onClick = { onNavSelect(index) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // MAIN PROGRAMMING CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Programación del Festival",
                        color = FestivalGold,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Cartelera de Actos y Horarios en Dolores Hidalgo", color = FestivalTextSecondary, fontSize = 12.sp)
                }

                Surface(
                    color = FestivalCardBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("VIERNES 23 DE NOVIEMBRE", color = FestivalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Schedule Timeline List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(scheduleList) { item ->
                    ScheduleCard(item = item, onClick = { activeDialogText = "Detalles del Acto:\n'${item.title}' por ${item.artist}\nEscenario: ${item.stage}\nHora: ${item.time}" })
                }
            }
        }
    }

    if (activeDialogText != null) {
        AlertDialog(
            onDismissRequest = { activeDialogText = null },
            title = { Text("Programación Smart TV", color = FestivalGold, fontWeight = FontWeight.Bold) },
            text = { Text(activeDialogText!!, color = Color.White) },
            confirmButton = {
                Button(
                    onClick = { activeDialogText = null },
                    colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = FestivalCardBg
        )
    }
}

/**
 * Tarjeta individual para mostrar un evento del horario en formato de línea de tiempo.
 * Se redibuja dependiendo de si el elemento está enfocado ([Modifier.onFocusChanged]) o es el evento en curso.
 *
 * @param item El [ScheduleItem] a mostrar.
 * @param onClick Acción al presionar "OK" en la tarjeta.
 */
@Composable
fun ScheduleCard(item: ScheduleItem, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCurrent) Color(0xFF2C2214) else if (isFocused) Color(0xFF2E3D30) else FestivalCardBg
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isFocused) 3.dp else if (item.isCurrent) 1.dp else 0.dp,
                color = if (isFocused) FestivalGold else if (item.isCurrent) FestivalGold.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (item.isCurrent) FestivalGold else FestivalSidebarBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.time,
                        color = if (item.isCurrent) Color.Black else FestivalGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (item.isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                                Text("EN CURSO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("${item.artist} • ${item.stage}", color = FestivalTextSecondary, fontSize = 12.sp)
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = FestivalGold)
        }
    }
}
```

---

## Paso 11: Pantalla de Autenticación Smart TV `TvLoginScreen.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * Pantalla de inicio de sesión para Android TV.
 * Renderiza una vista dividida:
 * - A la izquierda: un código QR dinámico para permitir el inicio de sesión desde un móvil.
 * - A la derecha: un formulario tradicional con usuario y contraseña (accesible vía D-Pad).
 *
 * @param onLoginSuccess Callback que se ejecuta cuando el usuario inicia sesión exitosamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvLoginScreen(
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2C2114), FestivalDarkBg),
                    radius = 1200f
                )
            )
            .padding(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT CARD (QR & Welcome)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "FESTIVAL JOSÉ ALFREDO JIMÉNEZ",
                    color = FestivalGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bienvenido al\nFestival",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 42.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Vive la magia de José Alfredo Jiménez desde la comodidad de tu hogar. Accede a conciertos exclusivos y contenido inédito.",
                    color = FestivalTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(28.dp))

                // QR Box Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = FestivalCardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(300.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DynamicQrCode(
                            content = "https://festivaljosealfredo.mx/tv-auth",
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Inicia con tu móvil", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("ESCANEA EL CÓDIGO QR", color = FestivalGold, fontSize = 10.sp, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            // RIGHT CARD (Login Form)
            Card(
                colors = CardDefaults.cardColors(containerColor = FestivalSidebarBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(420.dp)
                    .border(1.dp, FestivalGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("CORREO ELECTRÓNICO") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = FestivalGold) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FestivalGold,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = FestivalGold,
                            unfocusedLabelColor = Color.Gray,
                            focusedContainerColor = FestivalCardBg,
                            unfocusedContainerColor = FestivalCardBg
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("CONTRASEÑA") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = FestivalGold) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FestivalGold,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = FestivalGold,
                            unfocusedLabelColor = Color.Gray,
                            focusedContainerColor = FestivalCardBg,
                            unfocusedContainerColor = FestivalCardBg
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onLoginSuccess,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FestivalGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Iniciar Sesión →", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recuperar contraseña", color = FestivalTextSecondary, fontSize = 11.sp)
                        Text("Crear cuenta", color = FestivalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("• Dolores Hidalgo 2024 • Conectado •", color = FestivalTextSecondary, fontSize = 10.sp)
                }
            }
        }

        // BOTTOM TV D-PAD NAVIGATION HELP BAR
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("(OK) SELECCIONAR", color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("(▲/▼) NAVEGAR", color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("(BACK) REGRESAR", color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
```

---

## Paso 12: Pantalla de Ajustes `TvSettingsScreen.kt`

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.FestivalCardBg
import mx.utng.festivaltrack.tv.ui.theme.FestivalDarkBg
import mx.utng.festivaltrack.tv.ui.theme.FestivalGold

/**
 * Pantalla de configuración para la app en Android TV.
 * Muestra información del servidor, estado de transmisión, y permite cerrar sesión.
 * Incluye un menú lateral de navegación controlado por D-Pad.
 *
 * @param currentNavIndex Índice activo actualmente en el menú de navegación lateral.
 * @param onNavSelect Callback disparado al elegir una opción en el menú lateral.
 * @param onLogout Callback disparado al hacer clic en el botón de cerrar sesión.
 */
@Composable
fun TvSettingsScreen(
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit,
    onLogout: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
    ) {
        // SIDEBAR
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(Color(0xFF141A17))
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "FESTIVAL",
                    color = FestivalGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "José Alfredo Jiménez",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(32.dp))

                val navItems = listOf(
                    "Inicio" to Icons.Default.Home,
                    "Galería Histórica" to Icons.Default.Collections,
                    "Transmisión En Vivo" to Icons.Default.LiveTv,
                    "Programación" to Icons.Default.Event,
                    "Ajustes" to Icons.Default.Settings
                )

                navItems.forEachIndexed { index, (label, icon) ->
                    val isSelected = currentNavIndex == index
                    SidebarMenuItem(
                        label = label,
                        icon = icon,
                        isSelected = isSelected,
                        onClick = { onNavSelect(index) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // MAIN CONTENT AREA
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Ajustes del Dispositivo Smart TV", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsCard(
                    icon = Icons.Default.Dns,
                    title = "Servidor Backend & API",
                    subtitle = "http://10.0.2.2:3001/api/v1 (Conectado / Saludable)"
                )
                SettingsCard(
                    icon = Icons.Default.HighQuality,
                    title = "Calidad de Transmisión RTSP",
                    subtitle = "Automática (1080p 60fps / H.264 ExoPlayer)"
                )
                SettingsCard(
                    icon = Icons.Default.Tv,
                    title = "Dispositivo Smart TV",
                    subtitle = "Android TV Leanback OS (Conexión Emulador 10.0.2.2:1935)"
                )
                SettingsCard(
                    icon = Icons.Default.VolumeUp,
                    title = "Modo de Audio",
                    subtitle = "Estéreo En Vivo / Dolby Digital Surround"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC51111),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.4f)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión en Smart TV", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Tarjeta para mostrar una opción o información de configuración.
 *
 * @param icon Ícono [ImageVector] que representa la opción.
 * @param title Título principal de la configuración.
 * @param subtitle Texto secundario o descripción del estado actual.
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

/** Color de fondo principal oscuro para la aplicación de Android TV. */
val FestivalDarkBg = Color(0xFF0F1410)
/** Color de fondo para la barra lateral (sidebar) en Android TV. */
val FestivalSidebarBg = Color(0xFF141A15)
/** Color de fondo para las tarjetas (cards) en Android TV. */
val FestivalCardBg = Color(0xFF1E2720)
/** Color dorado primario del festival. */
val FestivalGold = Color(0xFFE6C27A)
/** Color dorado secundario o sombreado del festival. */
val FestivalGoldDark = Color(0xFFC4A059)
/** Color de texto principal (blanco). */
val FestivalTextPrimary = Color(0xFFFFFFFF)
/** Color de texto secundario (gris claro). */
val FestivalTextSecondary = Color(0xFFAAAAAA)
/** Color de insignia o botón para estado "En Vivo" (rojo). */
val FestivalBadgeLive = Color(0xFFE53935)
```

**`Theme.kt`:**
```kotlin
package mx.utng.festivaltrack.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema de colores oscuros personalizado para el módulo de Android TV.
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

/**
 * Tema principal para la aplicación de Android TV de FestivalTrack.
 *
 * @param content El contenido [Composable] que será renderizado usando este tema.
 */
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
 * Renderiza un código QR simulado basado en el contenido proporcionado.
 * Útil para Android TV al mostrar un QR de inicio de sesión que el usuario puede escanear con su dispositivo móvil.
 *
 * @param content El texto o URL que el QR representaría.
 * @param modifier Modificador de Compose para aplicar a la vista.
 * @param foregroundColor El color principal de los bloques del código QR.
 * @param backgroundColor El color de fondo sobre el cual se dibuja el código QR.
 */
@Composable
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

## Paso 15: Redirección ADB Puerto RTSP 1935 y Compilación

Para que el emulador de TV reciba la transmisión enviada por el móvil a la computadora anfitriona en el puerto 1935:

```bash
adb forward tcp:1935 tcp:1935
```

Para compilar y correr en el emulador de TV:
```bash
./gradlew :tv:installDebug
```

---

## Solución de Problemas

- **El video RTSP no carga:** Asegúrate de ejecutar el comando `adb forward tcp:1935 tcp:1935` mientras la app móvil está transmitiendo.
- **La interfaz no responde a las flechas del teclado:** Haz click dentro de la ventana del emulador de TV para que capture los eventos de teclado como si fueran del control remoto.
- **Falta dependencias Media3:** Revisa que hayas incluido las librerías `media3-exoplayer`, `media3-exoplayer-rtsp` y `media3-ui` en `build.gradle.kts`.
- **No aparece el teclado del chat:** En Android TV debes presionar "OK" sobre el botón *"Pulsa OK para escribir un mensaje..."* para que el cuadro modal de texto reciba el foco y despierte el teclado nativo del sistema.

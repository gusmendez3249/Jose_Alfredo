# 📺 Módulo `tv` — Aplicación Android TV (FestivalTrack)

## Descripción Técnica y Paradigma de Android TV

Desarrollar para Android TV es un paradigma radicalmente diferente al desarrollo móvil tradicional. Las principales diferencias son:
- **Navegación basada en D-Pad:** A diferencia de los móviles que dependen del "touch", en Android TV el usuario utiliza un control remoto con flechas direccionales (Arriba, Abajo, Izquierda, Derecha) y un botón de selección (OK/Enter). Esto requiere un manejo exhaustivo del "foco" en los elementos de la interfaz.
- **Pantallas grandes (10-foot UI):** Los elementos visuales, tipografías y el espaciado deben estar optimizados para ser leídos desde una distancia aproximada de 3 metros (10 pies).
- **Interacción indirecta:** No existen gestos de deslizamiento directo sobre componentes. Si un menú lateral o un cuadro de texto necesita interacción, este debe solicitar proactivamente el foco o mostrar diálogos accesibles.

## Estructura de Directorios Completa

```
tv/src/main/java/mx/utng/festivaltrack/tv/
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

## Paso 1: Configuración del Emulador Android TV (1080p, API 30+)

1. Abre el **Device Manager** en Android Studio.
2. Selecciona **Create Device** -> Categoría **TV**.
3. Selecciona el perfil **Television (1080p)**.
4. Elige una imagen del sistema con **API 30 o superior** (Android 11+).
5. Inicia el emulador y asegúrate de que responde correctamente a los botones de flecha del teclado (simulando el D-Pad).

## Paso 2: Configuración del `build.gradle.kts`

Asegúrate de tener las dependencias correctas para el módulo de TV, incluyendo Media3 para ExoPlayer y Compose Material 3:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "mx.utng.festivaltrack.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "mx.utng.festivaltrack.tv"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    
    // Compose para TV / UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // ExoPlayer Media3
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-exoplayer-rtsp:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
}
```

## Paso 3: Navegación con D-Pad - `onFocusChanged` y `FocusRequester`

Para la navegación por D-Pad utilizamos modificadores como `.onFocusChanged` y `.focusable()`.

```kotlin
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
fun SidebarMenuItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
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
        // ... (contenido interno)
    }
}
```

## Paso 4: `TvViewModel` - Sincronización periódica (Polling con Room)

Mantener los datos de la TV actualizados requiere sincronización en segundo plano con la API y Room:

```kotlin
/**
 * ViewModel central para la app en Android TV.
 * Gestiona el estado y la sincronización de los eventos mostrados en el televisor.
 *
 * @property eventos Flujo continuo con la lista de eventos actualizados.
 * @constructor Crea el ViewModel iniciando la base de datos de Room y empezando el polling periódico.
 */
class TvViewModel(application: Application) : AndroidViewModel(application) {
    
    // ... setup
    
    init {
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
}
```

## Paso 5: `TvMainScreen` - Layout Sidebar (260dp) y Contenido Principal

Se divide la pantalla horizontalmente: la barra lateral toma un ancho fijo de 260dp y el resto de la pantalla usa `weight(1f)`.

```kotlin
/**
 * Pantalla principal (Home) de la aplicación para Android TV.
 *
 * Muestra el panel lateral (sidebar) izquierdo para navegación principal y
 * un área central (main content) con un hero banner y un carrusel (`LazyRow`) de próximos eventos.
 * Todo el layout está optimizado para navegación con D-Pad, usando [onFocusChanged] en las tarjetas
 * para resaltar visualmente el elemento actual.
 */
@Composable
fun TvMainScreen(eventos: List<EventoEntity>, currentNavIndex: Int, onNavSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxSize().background(FestivalDarkBg)) {
        // ------------------ LEFT SIDEBAR ------------------
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(FestivalSidebarBg)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ... (elementos del menú)
        }
        
        // ------------------ MAIN CONTENT AREA ------------------
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
        ) {
             // ... (Hero banner y carrusel de eventos)
        }
    }
}
```

## Paso 6: `TvLiveStreamScreen` - ExoPlayer RTSP + Chat Polling (68% / 32%)

Aquí implementamos un layout distribuido (68% para video, 32% para chat). El chat usa una entrada de diálogo modal (`TvChatInputDialog`) porque los TextField nativos no son amigables para el D-Pad de la TV.

```kotlin
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
 */
@Composable
fun TvLiveStreamScreen(...) {
    // ... (Configuración de ExoPlayer para RTSP y Polling)
    
    Row(modifier = Modifier.fillMaxSize().background(FestivalDarkBg)) {
        // SIDEBAR MENÚ LATERAL
        Column(modifier = Modifier.width(200.dp)) { /*...*/ }
        
        // ÁREA DEL REPRODUCTOR EN VIVO (68% del ancho)
        Column(modifier = Modifier.weight(0.68f)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                }
            )
        }
        
        // PANEL DE CHAT LATERAL (32% del ancho)
        Column(modifier = Modifier.weight(0.32f)) {
            // ... LazyColumn con mensajes actualizados cada 3s
            
            // Botón interactivo para abrir TvChatInputDialog
            Box(
                modifier = Modifier
                    .focusRequester(chatFocusRequester)
                    .clickable { showChatDialog = true }
                    // ... (bordes y colores)
            ) {
                Text("Pulsa OK para escribir un mensaje...")
            }
        }
    }
}
```

## Paso 7: `TvGalleryScreen` - Galería Navegable (D-Pad)

La galería emplea `LazyVerticalGrid` e incluye `onFocusChanged` en cada imagen para crear un recuadro dorado y saber en qué foto está situado el control remoto.

```kotlin
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
        // ...
    }
}
```

## Paso 8: Redirección ADB Puerto RTSP 1935

Para que el emulador de TV reciba la transmisión enviada por el móvil a la computadora anfitriona en el puerto 1935:

```bash
adb forward tcp:1935 tcp:1935
```

## Paso 9: Compilación y Ejecución

Selecciona el módulo **`tv`** en Android Studio y ejecuta la aplicación (▶️). Alternativamente, usa:
```bash
./gradlew :tv:installDebug
```

## Solución de Problemas

- **El video RTSP no carga:** Asegúrate de ejecutar el comando `adb forward tcp:1935 tcp:1935` mientras la app móvil está transmitiendo.
- **La interfaz no responde a las flechas del teclado:** Haz click dentro de la ventana del emulador de TV para que capture los eventos de teclado como si fueran del control remoto.
- **Falta dependencias Media3:** Revisa que hayas incluido las librerías `media3-exoplayer`, `media3-exoplayer-rtsp` y `media3-ui` en `build.gradle.kts`.
- **No aparece el teclado del chat:** En Android TV debes presionar "OK" sobre el botón *"Pulsa OK para escribir un mensaje..."* para que el cuadro modal de texto reciba el foco y despierte el teclado nativo del sistema.

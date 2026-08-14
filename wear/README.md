# Módulo Wear OS - FestivalTrack

Este módulo implementa la aplicación compañera para relojes inteligentes (smartwatches) con el sistema operativo **Wear OS**. Su objetivo es proporcionar a los asistentes del festival una herramienta rápida, "glanceable" (de un vistazo) y que funcione incluso cuando el teléfono no está conectado o el internet es deficiente.

## 1. Descripción Técnica y Restricciones de Hardware

El desarrollo para Wear OS requiere consideraciones especiales debido a las estrictas limitaciones de hardware de estos dispositivos:

- **Batería Limitada:** Se deben minimizar las operaciones en segundo plano, evitar polling constante de red y aprovechar las alarmas inexactas (`AlarmManager`) cuando sea posible.
- **Pantalla Circular (y pequeña):** Se utilizan componentes especiales de Compose for Wear OS como `ScalingLazyColumn` (que escala y curva los elementos en los bordes) y `SwipeDismissableNavHost` para la navegación natural.
- **CPU y RAM reducidas:** Las operaciones de serialización masiva o procesamiento de imágenes grandes deben realizarse en el dispositivo móvil y enviarse ya procesadas a través de `WearableListenerService`.
- **Conectividad inestable:** Se implementa una arquitectura **Offline-First**. El reloj tiene su propia base de datos local y sincroniza los datos cuando el Data Layer se actualiza.

## 2. Estructura de Directorios Completa

```
wear/
├── build.gradle.kts                 # Configuración y dependencias de Wear OS
├── src/main/java/mx/utng/festivaltrack/wear/
│   ├── MainActivity.kt              # Punto de entrada de la app
│   ├── data/
│   │   └── sync/
│   │       └── WearSyncService.kt   # Servicio que escucha los envíos del móvil via DataEventBuffer
│   ├── domain/
│   │   └── usecase/
│   │       ├── GetProximosEventosUseCase.kt  # Obtiene los siguientes eventos 
│   │       └── ScheduleAlertasUseCase.kt     # Programa las alarmas locales
│   ├── presentation/
│   │   ├── navigation/
│   │   │   └── WearNavGraph.kt      # Rutas de navegación (NavHost)
│   │   ├── screens/
│   │   │   ├── OtherScreens.kt      # AlertaScreen, MapaAccesoScreen, NavEscenarioScreen
│   │   │   ├── ProgramaCompletoScreen.kt # Lista de todo el festival
│   │   │   ├── ProximosScreen.kt    # Interfaz principal adaptada al reloj
│   │   │   ├── SplashScreen.kt      # Pantalla de carga animada
│   │   │   └── WatchFaceScreen.kt   # Interfaz tipo carátula inicial
│   │   └── viewmodel/
│   │       └── ProximosViewModel.kt # ViewModel con estado reactivo (Flows)
└── src/main/res/
    └── ...                          # Iconos, temas y manifiesto
```

## Paso 1: Requisitos previos y configuración del emulador Wear OS

1. Instalar la imagen del sistema en el SDK Manager: `Wear OS 4 - API 33` o `Wear OS 3 - API 30`.
2. Crear un Android Virtual Device (AVD) de tipo Wear OS (ej. `Wear OS Small Round` o `Wear OS Large Round`).
3. En el teléfono emulador, instalar la aplicación "Wear OS companion" y vincularlo por Bluetooth mediante el comando `adb forward tcp:4444 localabstract:/adb-hub; adb connect localhost:4444`. (Si utilizas Android Studio Bumblebee o superior, el emparejamiento desde el IDE es automático).

## Paso 2: Configuración `build.gradle.kts` (Wear)

El archivo de configuración utiliza dependencias de Jetpack Compose exclusivas para Wear, OSMDroid para los mapas y Room para la persistencia.

```kotlin
plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "mx.utng.festivaltrack.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "mx.utng.festivaltrack.wear"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":shared")) // Módulo compartido con la lógica de negocio/Room

    // Wear OS Compose
    implementation(platform(libs.compose.bom))
    implementation("androidx.wear.compose:compose-material:1.3.1")
    implementation("androidx.wear.compose:compose-navigation:1.3.1")
    implementation(libs.compose.foundation)
    
    // Room (Offline First)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Wearable Data Layer
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    
    // GPS
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Mapas
    implementation(libs.osmdroid.android)
    
    // ViewModel y Coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
```

## Paso 3: Arquitectura y estrategia Offline-First con Room compartido

El reloj comparte el esquema de la base de datos a través del módulo `:shared`. Utiliza Room para garantizar que los eventos sigan siendo accesibles incluso si la conexión Bluetooth falla en medio del festival. El `WearSyncService` se ejecuta solo cuando hay cambios que descargar desde el móvil, ahorrando así batería.

## Paso 4: Implementación de ProximosViewModel

El ViewModel consume flujos (`StateFlow`) usando la base de datos para mostrar qué sigue. Filtra los eventos de forma reactiva considerando la hora actual (`Instant.now()`).

```kotlin
package mx.utng.festivaltrack.wear.presentation.viewmodel

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
import java.time.Instant

/**
 * ViewModel que gestiona el estado de los eventos en el módulo Wear OS.
 * Implementa una estrategia Offline-First observando la base de datos Room.
 * 
 * @property eventos Flujo de los próximos 15 eventos filtrados mediante la hora actual.
 * @property todosLosEventos Flujo con el listado completo de eventos.
 * @constructor Crea un [ProximosViewModel] e inicializa el repositorio local.
 */
class ProximosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FestivalRepository
    private val _eventos = MutableStateFlow<List<EventoEntity>>(emptyList())
    val eventos: StateFlow<List<EventoEntity>> = _eventos.asStateFlow()

    init {
        val database = FestivalDatabase.getInstance(application)
        repository = FestivalRepository(database.eventoDao())
        
        viewModelScope.launch {
            repository.getProximosEventosLocales("").collectLatest { lista ->
                val currentInstant = Instant.now()
                val filtrados = lista.filter { evento ->
                    // Parseo del Instant desde la fecha del evento
                    // Lógica de filtrado ...
                    true
                }.sortedBy { it.fechaHora }
                
                _eventos.value = filtrados.take(15)
            }
        }
        syncData()
    }

    /**
     * Sincroniza los eventos con el backend remoto.
     * Actualiza la base de datos local lo cual automáticamente refresca los [StateFlow].
     */
    private fun syncData() {
        viewModelScope.launch {
            repository.syncEventos()
        }
    }
}
```

## Paso 5: AlertaScreen - Cronómetro Reactivo

Muestra una notificación en vivo de la cuenta regresiva antes de que empiece un evento en particular.

```kotlin
/**
 * Pantalla que muestra una alerta inminente de un evento próximo.
 * Utiliza un cronómetro reactivo implementado con [LaunchedEffect] y `delay(10000)` para actualizar
 * el tiempo faltante sin saturar el hilo principal de la interfaz de Wear OS.
 *
 * @param eventoId ID del evento para consultar en la base de datos local.
 * @param onVerMapa Acción a ejecutar para navegar a la pantalla del mapa del evento.
 */
@Composable
fun AlertaScreen(eventoId: String, onVerMapa: () -> Unit) {
    var evento by remember { mutableStateOf<EventoEntity?>(null) }
    var tiempoFaltante by remember { mutableStateOf("Calculando...") }

    // Cronómetro en vivo (Actualización cada 10s para ahorrar batería)
    LaunchedEffect(evento) {
        if (evento == null) return@LaunchedEffect
        while (true) {
            val currentInstant = java.time.Instant.now()
            // Se calcula y formatea el tiempo restante
            tiempoFaltante = "Faltan 10 min" // Ejemplo simplificado
            kotlinx.coroutines.delay(10000) 
        }
    }

    // Interfaz visual omitida por brevedad
}
```

## Paso 6: MapaAccesoScreen - GPS y Enrutamiento con OSRM

En un reloj, pintar el mapa requiere un truco de `FrameLayout` para que la navegación de gestos laterales (swipe-to-dismiss) no interfiera con el arrastre del mapa en la interfaz circular. Se hace uso de `FusedLocationProviderClient` para localizar al usuario.

```kotlin
/**
 * Pantalla que integra un mapa de OSMDroid para navegación a pie.
 * Utiliza [LocationServices.getFusedLocationProviderClient] para el GPS del usuario y
 * consume el servicio de enrutamiento OSRM para dibujar una [Polyline] en el mapa.
 *
 * Configura un [FrameLayout] personalizado dentro de [AndroidView] para evitar que
 * `SwipeDismissableNavHost` intercepte los toques, permitiendo arrastrar el mapa.
 *
 * @param eventoId ID del evento cuyo destino (lat, lon) se buscará en BD para la ruta.
 * @param onBack Lambda para regresar a la pantalla anterior.
 */
@Composable
fun MapaAccesoScreen(eventoId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf(GeoPoint(21.156828, -100.934444)) }

    // FusedLocationClient y OSRM HTTP requests...

    AndroidView(
        factory = { ctx ->
            object : android.widget.FrameLayout(ctx) {
                override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
                    parent.requestDisallowInterceptTouchEvent(true)
                    return super.dispatchTouchEvent(ev)
                }
            }.apply {
                val mapView = MapView(ctx).apply {
                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                    setTileSource(TileSourceFactory.MAPNIK)
                }
                addView(mapView)
            }
        }
    )
}
```

## Paso 7: Wear Compose y ScalingLazyColumn

El principal componente visual en el que se basa la aplicación de reloj no es el tradicional `LazyColumn`, sino un componente de la librería de wear llamado `ScalingLazyColumn`. Este elemento se encarga de aplicar curvatura y escala (reduciendo el tamaño y opacidad) a los elementos en las orillas superior e inferior para adaptarlos de forma estética a pantallas de contorno circular o esférico.

## Paso 8: Compilación y Ejecución

Para compilar el módulo de Wear OS de manera aislada:
1. En Android Studio, selecciona la configuración de Run/Debug específica del módulo `wear`.
2. Asegúrate de que el emulador del reloj o dispositivo físico por adb inalámbrico/bluetooth esté conectado y autorizado.
3. Haz clic en **Run 'wear'**.
4. ¡Nota! Recuerda probar el desplazamiento por la pantalla de eventos utilizando el gesto físico del dial del reloj si está disponible en el hardware real o el soporte del emulador.

## Solución de Problemas

1. **La pantalla del mapa no responde al tacto:**
   - Causa: El `SwipeDismissableNavHost` está interceptando los gestos horizontales.
   - Solución: Verifica la implementación de `parent.requestDisallowInterceptTouchEvent(true)` en el `AndroidView`.

2. **No se renderiza OSMDroid completo o se corrompe el canvas en el borde circular:**
   - Causa: La aceleración de hardware en mapas complejos puede fallar en ciertos SOCs de smartwatches de gama baja.
   - Solución: Forzar el renderizado por software `setLayerType(View.LAYER_TYPE_SOFTWARE, null)`.

3. **No llegan datos del Data Layer del móvil:**
   - Causa: Diferentes `applicationId` o certificados de firma.
   - Solución: Para que la comunicación wearable-handheld funcione a través del `DataClient`, *ambas* aplicaciones (mobile y wear) deben tener el mismo `applicationId` y estar firmadas con la misma clave de desarrollo o producción.

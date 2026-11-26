# 📺 Módulo `tv` — Aplicación Android TV (FestivalTrack)

> Guía paso a paso del módulo de Android TV con el **código completo y actual** de cada archivo.
> Última actualización: 26 de noviembre de 2026 (TV con datos reales, sesión guardada, QR de la web en Render, imagen por concierto y mapa con las ubicaciones oficiales).

## Índice

- [Descripción y Arquitectura General](#descripción-y-arquitectura-general)
- [Estructura de Directorios del Módulo tv](#estructura-de-directorios-del-módulo-tv)
- [Paso 1: Configuración del Emulador Android TV](#paso-1-configuración-del-emulador-android-tv)
- [Paso 2: AndroidManifest.xml y seguridad de red](#paso-2-androidmanifestxml-y-seguridad-de-red)
- [Paso 3: build.gradle.kts y a qué servidor se conecta](#paso-3-buildgradlekts-y-a-qué-servidor-se-conecta)
- [Paso 4: Tema y Colores (Color.kt y Theme.kt)](#paso-4-tema-y-colores-colorkt-y-themekt)
- [Paso 5: MainActivity.kt](#paso-5-mainactivitykt)
- [Paso 6: ViewModel TvViewModel.kt](#paso-6-viewmodel-tvviewmodelkt)
- [Paso 7: Reproductor de música ReproductorTv.kt](#paso-7-reproductor-de-música-reproductortvkt)
- [Paso 8: Sesión guardada (TokenJwt.kt y SesionGuardadaTv.kt)](#paso-8-sesión-guardada-tokenjwtkt-y-sesionguardadatvkt)
- [Paso 9: Menú lateral (SidebarMenuItem.kt y TvSidebar.kt)](#paso-9-menú-lateral-sidebarmenuitemkt-y-tvsidebarkt)
- [Paso 10: Pantalla de inicio de sesión TvLoginScreen.kt](#paso-10-pantalla-de-inicio-de-sesión-tvloginscreenkt)
- [Paso 11: Pantalla principal TvMainScreen.kt e imagen de cada concierto PortadaConcierto.kt](#paso-11-pantalla-principal-tvmainscreenkt-e-imagen-de-cada-concierto-portadaconciertokt)
- [Paso 12: Detalle de concierto TvDetalleConcierto.kt](#paso-12-detalle-de-concierto-tvdetalleconciertokt)
- [Paso 13: Compra de boletos con QR (TvComprarBoletos.kt y DynamicQrCode.kt)](#paso-13-compra-de-boletos-con-qr-tvcomprarboletoskt-y-dynamicqrcodekt)
- [Paso 14: Programación TvScheduleScreen.kt](#paso-14-programación-tvschedulescreenkt)
- [Paso 15: Transmisión en vivo y chat TvLiveStreamScreen.kt](#paso-15-transmisión-en-vivo-y-chat-tvlivestreamscreenkt)
- [Paso 16: Biografía y música (TvBiografiaScreen.kt y TvMusica.kt)](#paso-16-biografía-y-música-tvbiografiascreenkt-y-tvmusicakt)
- [Paso 17: Mapa del festival TvMapaScreen.kt](#paso-17-mapa-del-festival-tvmapascreenkt)
- [Paso 18: Galería TvGalleryScreen.kt](#paso-18-galería-tvgalleryscreenkt)
- [Paso 19: Comunidad TvComunidadScreen.kt](#paso-19-comunidad-tvcomunidadscreenkt)
- [Paso 20: Mis boletos TvBoletosScreen.kt](#paso-20-mis-boletos-tvboletosscreenkt)
- [Paso 21: Ajustes TvSettingsScreen.kt](#paso-21-ajustes-tvsettingsscreenkt)
- [Paso 22: Pruebas unitarias](#paso-22-pruebas-unitarias)
- [Paso 23: Compilar, instalar y probar](#paso-23-compilar-instalar-y-probar)
- [Solución de Problemas](#solución-de-problemas)
- [Pendientes conocidos](#pendientes-conocidos)

## Descripción y Arquitectura General

El módulo `tv` es la aplicación nativa del Festival José Alfredo Jiménez para **Android TV / Google TV**, construida con **Jetpack Compose**, **Media3 (ExoPlayer con RTSP)**, **OSMDroid** y **Coil**. Está pensada como interfaz de 3 metros (*10-foot UI*): todo se maneja con el **control remoto (D-Pad)** y lo que es incómodo con el control (pagar, crear cuenta, ver la ruta) se resuelve con un **QR real** que se escanea con el celular.

Usa el mismo backend que el móvil y la web (NestJS en `/api/v1`) y comparte con ellos el módulo `shared` (Retrofit, Room y utilidades).

### Características Principales
1. **Inicio:** el próximo concierto (o «EN VIVO AHORA» si hay transmisión), la hora de Dolores Hidalgo y un carrusel de próximos conciertos, cada uno con su imagen (`bannerUrl` del servidor o una foto del festival según el lugar).
2. **Programación:** los conciertos reales agrupados por día en hora de México, con «EN CURSO» y «EN VIVO».
3. **Detalle de concierto:** su imagen, fecha, lugar, repertorio con demos de 30 s y QR para comprar boletos.
4. **Compra de boletos con QR:** se elige el concierto con el control y el QR abre su compra en la web desde el celular.
5. **Transmisión en vivo RTSP:** recibe el video del teléfono del administrador (prueba su IP real y luego la del emulador) y muestra el chat del concierto.
6. **Biografía y música:** vida y obra de José Alfredo, cita célebre, discografía y catálogo de canciones con reproductor de demos.
7. **Mapa:** las sedes con las coordenadas oficiales de OpenStreetMap y un QR con la ruta a pie para el celular.
8. **Galería y Comunidad:** fotos reales del servidor y publicaciones de los asistentes. En la TV la comunidad es **de solo lectura a propósito**: publicar, dar like o comentar se hace desde el celular.
9. **Mis boletos:** los boletos de la cuenta con su QR real, el mismo que valida el escáner de la entrada.
10. **Inicio de sesión real:** correo y contraseña contra el backend, o «Continuar sin cuenta». La sesión **se guarda cifrada** y al volver a abrir la app se entra directo mientras el token siga vigente.
11. **Ajustes:** servidor al que se conecta y si responde, estado de la transmisión, página de compra y cuenta.

### Cómo fluye la información

```text
┌──────────────┐   cada 5 s    ┌──────────────────┐   Flow    ┌─────────────┐
│ Backend REST │ ────────────▶ │ Room (shared)    │ ────────▶ │ TvViewModel │ ──▶ Pantallas
│  /api/v1     │  eventos      │ EventoEntity     │           │  StateFlow  │     Compose
└──────────────┘               └──────────────────┘           └─────────────┘
       │  stream/status cada 3 s, galería, biografía, canciones, foro, boletos, sitios  ▲
       └────────────────────────────────────────────────────────────────────────────────┘
Teléfono del admin ── RTSP :1935 ──▶ ExoPlayer de la TV (TvLiveStreamScreen)
TV ── QR ──▶ celular: compra / registro en la web, ruta a pie en Google Maps
```

- `TvViewModel` es el único ViewModel: sincroniza la cartelera, consulta la transmisión, guarda la sesión y expone todo como `StateFlow`.
- `MainActivity` dibuja el menú lateral único (`TvSidebar`) y la sección elegida; no hay `NavHost`, la sección es un estado (`SeccionTv`).
- Utilidades de `shared` que usa la TV: `HorarioFestival` (hora de México y próximos conciertos), `CodigoQr` (matriz QR con ZXing) y `EnlacesWeb` (URL de compra y registro).

---

## Estructura de Directorios del Módulo `tv`

```text
tv/
├── build.gradle.kts
├── README.md
└── src/
    ├── debug/res/xml/network_security_config.xml   # HTTP permitido en depuración (backend local)
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── res/
    │   │   ├── drawable/                           # fotos del festival (hero, portadas, galería)
    │   │   └── xml/network_security_config.xml     # release: solo HTTPS salvo emulador
    │   └── java/mx/utng/festivaltrack/tv/
    │       ├── MainActivity.kt
    │       ├── data/
    │       │   ├── SesionGuardadaTv.kt             # sesión cifrada con el Keystore
    │       │   └── TokenJwt.kt                     # vencimiento del JWT
    │       ├── presentation/
    │       │   ├── components/
    │       │   │   ├── PortadaConcierto.kt         # imagen de cada concierto
    │       │   │   ├── SidebarMenuItem.kt
    │       │   │   ├── TvComprarBoletos.kt         # compra con QR
    │       │   │   ├── TvDetalleConcierto.kt
    │       │   │   ├── TvMusica.kt                 # barra del reproductor y componentes comunes
    │       │   │   └── TvSidebar.kt                # menú lateral único
    │       │   ├── screens/
    │       │   │   ├── TvBiografiaScreen.kt
    │       │   │   ├── TvBoletosScreen.kt
    │       │   │   ├── TvComunidadScreen.kt
    │       │   │   ├── TvGalleryScreen.kt
    │       │   │   ├── TvLiveStreamScreen.kt
    │       │   │   ├── TvLoginScreen.kt
    │       │   │   ├── TvMainScreen.kt
    │       │   │   ├── TvMapaScreen.kt
    │       │   │   ├── TvScheduleScreen.kt
    │       │   │   └── TvSettingsScreen.kt
    │       │   └── viewmodel/
    │       │       ├── ReproductorTv.kt            # demos de 30 s con Media3
    │       │       └── TvViewModel.kt
    │       └── ui/
    │           ├── theme/ (Color.kt, Theme.kt)
    │           └── utils/DynamicQrCode.kt          # QR real dibujado en Compose
    └── test/java/mx/utng/festivaltrack/tv/         # pruebas unitarias (JUnit)
        ├── data/TokenJwtTest.kt
        └── presentation/
            ├── components/PortadaConciertoTest.kt
            └── screens/TvMapaTest.kt
```

---

## Paso 1: Configuración del Emulador Android TV

1. Abre **Android Studio** → **Device Manager** → **Create Device**.
2. Elige la categoría **TV** y el perfil **Television (1080p)** o **Television (720p)**.
3. Instala una imagen **Android 11 (API 30) o superior** con Google TV, x86_64 (el módulo pide `minSdk = 30`).
4. Ponle un nombre (por ejemplo `Android_TV_1080p`) y termina.

> **Ojo con el tamaño:** en 720p la TV mide solo unos **540 dp de alto**. Todas las pantallas están pensadas para caber ahí sin recortarse; si agregas algo, revísalo también en 720p.

---

## Paso 2: `AndroidManifest.xml` y seguridad de red

Declara la app como de Android TV (`leanback` obligatorio y pantalla táctil opcional) y la abre desde el lanzador de la TV (`LEANBACK_LAUNCHER`). `allowBackup="false"` evita que la sesión guardada se copie a otro dispositivo.

**`src/main/AndroidManifest.xml`**

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
        android:allowBackup="false"
        android:label="Festival Track TV"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar"
        android:networkSecurityConfig="@xml/network_security_config">
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

En **release** solo se permite HTTPS, salvo el emulador y el propio dispositivo:

**`src/main/res/xml/network_security_config.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  Configuración de red para builds de release: todo el tráfico va por HTTPS (backend en Railway).
  Solo se permite HTTP hacia el emulador (10.0.2.2) y el propio dispositivo, que nunca salen a internet.
  Los builds de depuración usan src/debug/res/xml/network_security_config.xml.
-->
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">localhost</domain>
        <domain includeSubdomains="false">127.0.0.1</domain>
    </domain-config>
</network-security-config>
```

En **debug** se permite HTTP para probar con el backend en la PC de la red local:

**`src/debug/res/xml/network_security_config.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  Solo builds de depuración: permite HTTP para hablar con el backend local en la IP de la PC
  (celular físico en la misma red Wi-Fi), cuya IP cambia y no se puede listar aquí.
  El build de release usa src/main/res/xml/network_security_config.xml, que exige HTTPS.
-->
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

---

## Paso 3: `build.gradle.kts` y a qué servidor se conecta

Dependencias del módulo: Compose, Coil (imágenes), Room y Retrofit (vía `shared`), OSMDroid (mapa), Media3 (RTSP y música) y JUnit para las pruebas.

**`build.gradle.kts`**

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
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.1")

    // Navigation & Lifecycle
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Image Loading (Coil)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    kapt("androidx.room:room-compiler:2.8.5")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    
    // Mapa de sedes (misma librería que el móvil)
    implementation(libs.osmdroid.android)

    // Media3 (ExoPlayer)
    val media3Version = "1.2.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    // Pruebas unitarias (JVM)
    testImplementation("junit:junit:4.13.2")
}
```

El servidor y la página web se configuran en el módulo `shared` (ver `shared/build.gradle.kts`):

| Propiedad | Dónde | Para qué | Si no se define |
|---|---|---|---|
| `festivaltrackApiUrl` | `-P` al compilar | URL fija del backend, p. ej. `https://festivaltrack-backend.onrender.com/api/v1` | Emulador: `http://10.0.2.2:3001`; dispositivo físico: `festivaltrack.deviceHost` |
| `festivaltrack.deviceHost` | `local.properties` | IP de la PC con el backend, para una TV física en la misma red | `10.0.2.2` |
| `festivaltrackWebUrl` / `festivaltrack.webUrl` | `-P` o `local.properties` | Página web que abren los QR de compra y registro | Si la API es la de Render, `https://festivaltrack-web.onrender.com`; si no, `http://<PC>:5173` |

La regla de la página web está en `shared/.../util/EnlacesWeb.kt` (`baseDesde`): primero la URL configurada; si la API está en Render (`<nombre>-backend.onrender.com`), la web publicada a su lado (`<nombre>-web.onrender.com`); si no, la PC del backend en el puerto 5173 de Vite.

---

## Paso 4: Tema y Colores (`Color.kt` y `Theme.kt`)

Paleta oscura con dorado del festival. Los colores se usan directamente (`FestivalGold`, `FestivalDarkBg`, `FestivalCardBg`…) en todas las pantallas.

**`src/main/java/mx/utng/festivaltrack/tv/ui/theme/Color.kt`**

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

**`src/main/java/mx/utng/festivaltrack/tv/ui/theme/Theme.kt`**

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

## Paso 5: `MainActivity.kt`

Punto de entrada. Junta todos los estados del ViewModel y decide qué se ve:

1. **Login** al abrir, salvo que haya una sesión guardada y vigente: entonces entra directo.
2. **Menú lateral** (`TvSidebar`) y la **sección** elegida (`SeccionTv`), guardada con `rememberSaveable`.
3. Encima de cualquier sección: el **detalle de un concierto**, la **compra con QR** y la **barra de música**.
4. El botón **Atrás** regresa al Inicio antes de salir; en la transmisión se pausa la música.

**`src/main/java/mx/utng/festivaltrack/tv/MainActivity.kt`**

```kotlin
package mx.utng.festivaltrack.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import mx.utng.festivaltrack.tv.presentation.components.SeccionTv
import mx.utng.festivaltrack.tv.presentation.components.TvBarraMusica
import mx.utng.festivaltrack.tv.presentation.components.TvDetalleConcierto
import mx.utng.festivaltrack.tv.presentation.screens.ORIGEN_DISCOGRAFIA
import mx.utng.festivaltrack.tv.presentation.screens.TvBiografiaScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvBoletosScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvComunidadScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvMapaScreen
import mx.utng.festivaltrack.tv.presentation.viewmodel.Cargable
import mx.utng.festivaltrack.tv.presentation.components.TvComprarBoletosDialog
import mx.utng.festivaltrack.tv.presentation.components.TvSidebar
import mx.utng.festivaltrack.tv.presentation.screens.TvGalleryScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvLiveStreamScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvLoginScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvMainScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvScheduleScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvSettingsScreen
import mx.utng.festivaltrack.tv.presentation.viewmodel.TvViewModel
import mx.utng.festivaltrack.tv.ui.theme.FestivalDarkBg
import mx.utng.festivaltrack.tv.ui.theme.FestivalTrackTvTheme

/**
 * Punto de entrada de la app de Android TV.
 *
 * Al abrir se muestra el inicio de sesión (con opción de seguir como invitado), salvo que haya
 * una sesión guardada y vigente: entonces se entra directo con esa cuenta. Después, un
 * menú lateral único y la sección elegida. "Comprar boletos" abre, desde cualquier sección, el
 * QR para pagar en el celular. El botón Atrás del control regresa al Inicio antes de salir.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FestivalTrackTvTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = FestivalDarkBg) {
                    val proximos by viewModel.proximos.collectAsState()
                    val stream by viewModel.stream.collectAsState()
                    val ahora by viewModel.ahora.collectAsState()
                    val sesion by viewModel.sesion.collectAsState()
                    val galeria by viewModel.galeria.collectAsState()
                    val servidorConectado by viewModel.servidorConectado.collectAsState()
                    val musica by viewModel.musica.collectAsState()
                    val biografia by viewModel.biografia.collectAsState()
                    val canciones by viewModel.canciones.collectAsState()
                    val sitios by viewModel.sitios.collectAsState()
                    val comunidad by viewModel.comunidad.collectAsState()
                    val boletos by viewModel.boletos.collectAsState()

                    // Con una sesión guardada y vigente se entra directo; si no, se pide iniciar sesión.
                    var mostrarLogin by rememberSaveable { mutableStateOf(viewModel.sesion.value == null) }
                    var yaEntro by rememberSaveable { mutableStateOf(viewModel.sesion.value != null) }
                    var seccion by rememberSaveable { mutableStateOf(SeccionTv.INICIO) }
                    // Compra con QR: null = cerrada; "" = sin concierto preseleccionado.
                    var comprar by remember { mutableStateOf<String?>(null) }
                    // Detalle de concierto abierto (id) o null.
                    var detalle by remember { mutableStateOf<String?>(null) }

                    // En la transmisión se pausa la música para que se escuche el concierto (como en el móvil).
                    LaunchedEffect(seccion) { if (seccion == SeccionTv.EN_VIVO) viewModel.reproductor.pausar() }

                    val reproducirCatalogo: (Int) -> Unit = { i ->
                        (canciones as? Cargable.Listo)?.datos?.takeIf { it.isNotEmpty() }
                            ?.let { viewModel.reproductor.reproducir(it, i, ORIGEN_DISCOGRAFIA) }
                    }

                    if (mostrarLogin) {
                        // Si se abrió desde dentro (chat o Ajustes), Atrás regresa a donde estaba.
                        BackHandler(enabled = yaEntro) { mostrarLogin = false }
                        TvLoginScreen(
                            iniciarSesion = viewModel::iniciarSesion,
                            onEntrar = { mostrarLogin = false; yaEntro = true },
                            onContinuarSinCuenta = { mostrarLogin = false; yaEntro = true }
                        )
                        return@Surface
                    }

                    BackHandler(enabled = seccion != SeccionTv.INICIO) { seccion = SeccionTv.INICIO }

                    Row(Modifier.fillMaxSize()) {
                        TvSidebar(
                            seccion = seccion,
                            onSeccion = { seccion = it },
                            enVivo = stream?.isLive == true,
                            usuario = sesion?.nombre,
                            onComprarBoletos = { comprar = "" }
                        )
                        Column(Modifier.weight(1f)) {
                          Box(Modifier.weight(1f)) {
                            when (seccion) {
                                SeccionTv.INICIO -> TvMainScreen(
                                    proximos = proximos,
                                    stream = stream,
                                    ahora = ahora,
                                    usuario = sesion?.nombre,
                                    onVerEnVivo = { seccion = SeccionTv.EN_VIVO },
                                    onVerProgramacion = { seccion = SeccionTv.PROGRAMACION },
                                    onComprar = { comprar = it ?: "" },
                                    onAbrirConcierto = { detalle = it },
                                    onEscucharMusica = { reproducirCatalogo(0) }
                                )
                                SeccionTv.BIOGRAFIA -> TvBiografiaScreen(
                                    biografia = biografia,
                                    canciones = canciones,
                                    musica = musica,
                                    onReproducir = { lista, i -> viewModel.reproductor.reproducir(lista, i, ORIGEN_DISCOGRAFIA) },
                                    onReintentar = viewModel::cargarBiografiaYMusica
                                )
                                SeccionTv.MAPA -> TvMapaScreen(proximos = proximos, sitios = sitios, ahora = ahora)
                                SeccionTv.COMUNIDAD -> TvComunidadScreen(comunidad = comunidad, onRecargar = viewModel::cargarComunidad)
                                SeccionTv.BOLETOS -> TvBoletosScreen(
                                    sesion = sesion,
                                    boletos = boletos,
                                    ahora = ahora,
                                    onCargar = viewModel::cargarBoletos,
                                    onIniciarSesion = { mostrarLogin = true },
                                    onComprar = { comprar = "" }
                                )
                                SeccionTv.GALERIA -> TvGalleryScreen(estado = galeria, onReintentar = viewModel::cargarGaleria)
                                SeccionTv.EN_VIVO -> TvLiveStreamScreen(
                                    stream = stream,
                                    proximos = proximos,
                                    ahora = ahora,
                                    sesion = sesion,
                                    enviarMensaje = viewModel::enviarMensajeChat,
                                    onIniciarSesion = { mostrarLogin = true },
                                    onVerProgramacion = { seccion = SeccionTv.PROGRAMACION }
                                )
                                SeccionTv.PROGRAMACION -> TvScheduleScreen(
                                    proximos = proximos,
                                    stream = stream,
                                    ahora = ahora,
                                    onAbrir = { detalle = it },
                                    onVerEnVivo = { seccion = SeccionTv.EN_VIVO }
                                )
                                SeccionTv.AJUSTES -> TvSettingsScreen(
                                    servidorConectado = servidorConectado,
                                    stream = stream,
                                    sesion = sesion,
                                    onIniciarSesion = { mostrarLogin = true },
                                    onCerrarSesion = viewModel::cerrarSesion
                                )
                            }
                          }
                          // Barra del reproductor: visible en cualquier sección mientras hay música.
                          if (musica.actual != null && seccion != SeccionTv.EN_VIVO) {
                              Box(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)) {
                                  TvBarraMusica(
                                      estado = musica,
                                      onAnterior = viewModel.reproductor::anterior,
                                      onAlternar = viewModel.reproductor::alternar,
                                      onSiguiente = { viewModel.reproductor.siguiente() },
                                      onCerrar = viewModel.reproductor::detener
                                  )
                              }
                          }
                        }
                    }

                    detalle?.let { id ->
                        val evento = proximos.firstOrNull { it.id == id }
                        if (evento == null) detalle = null
                        else TvDetalleConcierto(
                            evento = evento,
                            ahora = ahora,
                            enVivo = stream?.isLive == true && stream?.eventoId == evento.id,
                            musica = musica,
                            cargarRepertorio = viewModel::repertorio,
                            onReproducir = { lista, i -> viewModel.reproductor.reproducir(lista, i, evento.nombre) },
                            onVerEnVivo = { detalle = null; seccion = SeccionTv.EN_VIVO },
                            onCerrar = { detalle = null }
                        )
                    }

                    comprar?.let { preseleccionado ->
                        TvComprarBoletosDialog(
                            conciertos = proximos,
                            ahora = ahora,
                            preseleccionado = preseleccionado.ifEmpty { null },
                            onCerrar = { comprar = null }
                        )
                    }
                }
            }
        }
    }
}
```

---

## Paso 6: ViewModel `TvViewModel.kt`

Concentra los datos de toda la app:

- **Cartelera:** sincroniza con el backend cada 5 s y guarda en Room; `proximos` aplica las mismas reglas que el móvil (sin cancelados ni pasados, en hora de México).
- **Transmisión:** consulta `stream/status` cada 3 s; también sirve para saber si el servidor responde.
- **Sesión:** inicia sesión contra el backend y la guarda con `SesionGuardadaTv`. Si el servidor rechaza el token (401) en boletos, comunidad o al escribir en el chat, la sesión se cierra sola (`cerrarSesionSiSigue` evita cerrar una sesión nueva que se abrió mientras tanto).
- **Chat:** `enviarMensajeChat` envía los mensajes de la transmisión con la sesión actual y regresa el texto del error para mostrarlo.
- **Biografía, canciones, galería, comunidad, sitios y boletos:** cada uno como `Cargable` (`Cargando`, `Listo` o `SinConexion`).

**`src/main/java/mx/utng/festivaltrack/tv/presentation/viewmodel/TvViewModel.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.local.FestivalDatabase
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.BiografiaDto
import mx.utng.festivaltrack.shared.data.remote.BoletoDto
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.ChatMessageDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.PublicacionDto
import mx.utng.festivaltrack.shared.data.remote.SitioArDto
import mx.utng.festivaltrack.shared.data.remote.GaleriaDto
import mx.utng.festivaltrack.shared.data.remote.LoginDto
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.data.DatosSesion
import mx.utng.festivaltrack.tv.data.SesionGuardadaTv
import retrofit2.HttpException
import java.time.Instant

/**
 * Cuenta con la que se inició sesión en la TV. Se guarda cifrada ([SesionGuardadaTv]) y se
 * restaura al abrir la app mientras el token siga vigente.
 */
data class SesionTv(val nombre: String, val correo: String, val token: String)

/** Estado de la galería traída del servidor. */
sealed class EstadoGaleria {
    object Cargando : EstadoGaleria()
    data class Lista(val galerias: List<GaleriaDto>) : EstadoGaleria()
    object SinConexion : EstadoGaleria()
}

/**
 * ViewModel central de la app de Android TV.
 *
 * - **Cartelera**: eventos de Room sincronizados con el backend cada 5 s; [proximos] aplica las
 *   mismas reglas que el móvil (sin cancelados ni pasados, ordenados, hora de México).
 * - **Transmisión**: consulta `stream/status` cada 3 s; [stream] es null si el servidor no responde.
 * - **Sesión**: login real contra el backend; [sesion] es null para ver como invitado. Se guarda
 *   en la TV y, si el servidor rechaza el token (401) en boletos, comunidad o chat, se cierra sola.
 * - **Reloj**: [ahora] avanza cada 30 s para que "Hoy", "En curso" y la hora se actualicen solos.
 */
class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FestivalRepository
    private val api = FestivalApiService.create()

    private val _eventos = MutableStateFlow<List<EventoEntity>>(emptyList())
    val eventos: StateFlow<List<EventoEntity>> = _eventos.asStateFlow()

    private val _ahora = MutableStateFlow(Instant.now())
    val ahora: StateFlow<Instant> = _ahora.asStateFlow()

    /** Conciertos que todavía no terminan, del más próximo al más lejano. */
    val proximos: StateFlow<List<EventoEntity>> = combine(_eventos, _ahora) { lista, ahora ->
        HorarioFestival.proximos(lista, ahora)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _stream = MutableStateFlow<StreamStatusDto?>(null)
    /** Estado de la transmisión; null mientras no hay respuesta del servidor. */
    val stream: StateFlow<StreamStatusDto?> = _stream.asStateFlow()

    private val _servidorConectado = MutableStateFlow<Boolean?>(null)
    /** null = todavía no se sabe; true/false según la última consulta al backend. */
    val servidorConectado: StateFlow<Boolean?> = _servidorConectado.asStateFlow()

    private val _galeria = MutableStateFlow<EstadoGaleria>(EstadoGaleria.Cargando)
    val galeria: StateFlow<EstadoGaleria> = _galeria.asStateFlow()

    private val sesionGuardada = SesionGuardadaTv(application)

    private val _sesion = MutableStateFlow(sesionGuardada.leer()?.let { SesionTv(it.nombre, it.correo, it.token) })
    /** Sesión actual; ya viene restaurada al crear el ViewModel si había una guardada y vigente. */
    val sesion: StateFlow<SesionTv?> = _sesion.asStateFlow()

    /** Música: demos de 30 s, como en el móvil. */
    val reproductor = ReproductorTv(application, viewModelScope)
    val musica: StateFlow<EstadoMusica> = reproductor.estado

    private val _biografia = MutableStateFlow<Cargable<BiografiaDto?>>(Cargable.Cargando)
    val biografia: StateFlow<Cargable<BiografiaDto?>> = _biografia.asStateFlow()

    private val _canciones = MutableStateFlow<Cargable<List<CancionDto>>>(Cargable.Cargando)
    /** Catálogo de canciones del artista (`GET /canciones`). */
    val canciones: StateFlow<Cargable<List<CancionDto>>> = _canciones.asStateFlow()

    private val _sitios = MutableStateFlow<List<SitioArDto>>(emptyList())
    val sitios: StateFlow<List<SitioArDto>> = _sitios.asStateFlow()

    private val _comunidad = MutableStateFlow<Cargable<List<PublicacionDto>>>(Cargable.Cargando)
    val comunidad: StateFlow<Cargable<List<PublicacionDto>>> = _comunidad.asStateFlow()

    private val _boletos = MutableStateFlow<Cargable<List<BoletoDto>>>(Cargable.Cargando)
    /** Boletos de la cuenta con la que se inició sesión. */
    val boletos: StateFlow<Cargable<List<BoletoDto>>> = _boletos.asStateFlow()

    init {
        val database = FestivalDatabase.getInstance(application)
        repository = FestivalRepository(database.eventoDao())

        viewModelScope.launch {
            repository.getEventosLocales().collectLatest { list -> _eventos.value = list }
        }

        // Cartelera: sincroniza con el backend al abrir y cada 5 s.
        viewModelScope.launch {
            while (true) {
                runCatching { repository.syncEventos() }
                delay(5_000)
            }
        }

        // Transmisión en vivo y estado del servidor: cada 3 s.
        viewModelScope.launch {
            while (true) {
                val estado = runCatching { api.getStreamStatus() }.getOrNull()
                _stream.value = estado
                _servidorConectado.value = estado != null
                delay(3_000)
            }
        }

        viewModelScope.launch {
            while (true) {
                _ahora.value = Instant.now()
                delay(30_000)
            }
        }

        // Sesión restaurada: se confirma con el servidor (si ya no acepta el token, se cierra).
        _sesion.value?.let { restaurada ->
            viewModelScope.launch {
                runCatching { api.getMisBoletos("Bearer ${restaurada.token}") }
                    .onFailure { if (esTokenRechazado(it)) cerrarSesionSiSigue(restaurada.token) }
            }
        }

        cargarGaleria()
        cargarBiografiaYMusica()
        cargarComunidad()
        viewModelScope.launch { runCatching { api.getSitiosAr() }.onSuccess { _sitios.value = it } }
    }

    /** Vuelve a pedir las galerías al servidor. */
    fun cargarGaleria() {
        _galeria.value = EstadoGaleria.Cargando
        viewModelScope.launch {
            _galeria.value = runCatching { api.getGalerias() }
                .map { EstadoGaleria.Lista(it) }
                .getOrDefault(EstadoGaleria.SinConexion)
        }
    }

    /**
     * Inicia sesión contra el backend. Regresa null si entró o el mensaje de error para mostrar
     * (los mismos textos que el móvil).
     */
    suspend fun iniciarSesion(correo: String, contrasena: String): String? {
        if (correo.isBlank() || contrasena.isBlank()) return "Escribe tu correo y tu contraseña"
        return try {
            val respuesta = api.login(LoginDto(correo.trim(), contrasena))
            _sesion.value = SesionTv(respuesta.usuario.nombre, respuesta.usuario.correo, respuesta.accessToken)
            sesionGuardada.guardar(DatosSesion(respuesta.usuario.nombre, respuesta.usuario.correo, respuesta.accessToken))
            null
        } catch (e: HttpException) {
            when (e.code()) {
                400, 401 -> "Correo o contraseña incorrectos"
                429 -> "Demasiados intentos. Espera un minuto e intenta de nuevo."
                else -> "El servidor no respondió. Intenta de nuevo."
            }
        } catch (e: Exception) {
            "Sin conexión con el servidor. Revisa la red de la TV."
        }
    }

    fun cerrarSesion() {
        sesionGuardada.borrar()
        _sesion.value = null
        _boletos.value = Cargable.Cargando
    }

    /** El servidor ya no acepta el token (venció, se cambió la llave o se borró la cuenta). */
    private fun esTokenRechazado(error: Throwable) = (error as? HttpException)?.code() == 401

    /** Cierra la sesión por un token rechazado, salvo que mientras tanto se haya entrado con otra. */
    private fun cerrarSesionSiSigue(token: String) {
        if (_sesion.value?.token == token) cerrarSesion()
    }

    /** Biografía del artista y catálogo de canciones. */
    fun cargarBiografiaYMusica() {
        _biografia.value = Cargable.Cargando
        _canciones.value = Cargable.Cargando
        viewModelScope.launch {
            _biografia.value = runCatching { api.getBiografias().firstOrNull() }
                .map { Cargable.Listo(it) }.getOrDefault(Cargable.SinConexion)
        }
        viewModelScope.launch {
            _canciones.value = runCatching { api.getCanciones() }
                .map { Cargable.Listo(it) }.getOrDefault(Cargable.SinConexion)
        }
    }

    /** Publicaciones de la comunidad (las mismas que se ven en la app del celular). */
    fun cargarComunidad() {
        _comunidad.value = Cargable.Cargando
        viewModelScope.launch {
            val actual = _sesion.value?.token
            val token = actual?.let { "Bearer $it" }
            var resultado = runCatching { api.getForo(token = token, limit = 30) }
            // Con un token que el servidor ya no acepta, se cierra la sesión y se lee como invitado.
            if (actual != null && resultado.exceptionOrNull()?.let(::esTokenRechazado) == true) {
                cerrarSesionSiSigue(actual)
                resultado = runCatching { api.getForo(token = null, limit = 30) }
            }
            _comunidad.value = resultado.map { Cargable.Listo(it) }.getOrDefault(Cargable.SinConexion)
        }
    }

    /** Boletos de la sesión actual; sin sesión se queda en [Cargable.Cargando] (la pantalla pide entrar). */
    fun cargarBoletos() {
        val sesion = _sesion.value ?: return
        _boletos.value = Cargable.Cargando
        viewModelScope.launch {
            val resultado = runCatching { api.getMisBoletos("Bearer ${sesion.token}") }
            if (resultado.exceptionOrNull()?.let(::esTokenRechazado) == true) {
                cerrarSesionSiSigue(sesion.token) // la pantalla vuelve a pedir que se inicie sesión
                return@launch
            }
            _boletos.value = resultado.map { Cargable.Listo(it) }.getOrDefault(Cargable.SinConexion)
        }
    }

    /**
     * Envía un mensaje al chat del concierto con la sesión actual. Regresa null si se envió o el
     * texto del error. Si el servidor ya no acepta el token (401), cierra la sesión como en
     * boletos y comunidad, y pide volver a entrar.
     */
    suspend fun enviarMensajeChat(eventoId: String, texto: String): String? {
        val sesion = _sesion.value ?: return "Inicia sesión para escribir."
        return try {
            api.sendChatMessage("Bearer ${sesion.token}", ChatMessageDto(eventoId = eventoId, usuarioNombre = sesion.nombre, mensaje = texto))
            null
        } catch (e: HttpException) {
            if (esTokenRechazado(e)) {
                cerrarSesionSiSigue(sesion.token)
                "Tu sesión venció. Inicia sesión de nuevo para escribir."
            } else {
                "No se pudo enviar el mensaje. Intenta de nuevo."
            }
        } catch (e: Exception) {
            "No se pudo enviar el mensaje. Revisa la conexión."
        }
    }

    /** Repertorio de un concierto para escuchar sus demos. */
    suspend fun repertorio(eventoId: String): List<CancionDto>? =
        runCatching { api.getCancionesEvento(eventoId) }.getOrNull()

    override fun onCleared() {
        reproductor.liberar()
        super.onCleared()
    }
}

/** Datos que se piden al servidor: cargando, listos o sin conexión. */
sealed class Cargable<out T> {
    object Cargando : Cargable<Nothing>()
    data class Listo<T>(val datos: T) : Cargable<T>()
    object SinConexion : Cargable<Nothing>()
}
```

---

## Paso 7: Reproductor de música `ReproductorTv.kt`

Igual que el del móvil: cada canción suena como demo de hasta 30 s y pasa sola a la siguiente. Lo usan la biografía (catálogo y cita célebre) y el detalle de cada concierto (su repertorio).

**`src/main/java/mx/utng/festivaltrack/tv/presentation/viewmodel/ReproductorTv.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

/** Lo que está sonando en la TV. */
data class EstadoMusica(
    val cola: List<CancionDto> = emptyList(),
    val indice: Int = 0,
    val sonando: Boolean = false,
    /** De dónde viene la cola: "Discografía de José Alfredo" o el nombre del concierto. */
    val origen: String = "",
    val posicionMs: Long = 0,
    val duracionMs: Long = ReproductorTv.DEMO_MS,
    val error: String? = null
) {
    val actual: CancionDto? get() = cola.getOrNull(indice)
}

/**
 * Reproductor de música de la TV, igual que el del móvil: cada canción suena como demo de hasta
 * [DEMO_MS] y al terminar pasa sola a la siguiente. Lo usan la biografía (catálogo y cita
 * célebre) y el detalle de cada concierto (su repertorio).
 */
class ReproductorTv(context: Context, private val scope: CoroutineScope) {

    companion object {
        /** Igual que en el móvil: fragmentos de 30 s. */
        const val DEMO_MS = 30_000L

        /** Ruta del servidor (`/uploads/x.mp3`) → URL completa; deja intactas las URL completas. */
        fun url(ruta: String): String = if (ruta.startsWith("/")) FestivalApiService.SERVER_URL + ruta else ruta
    }

    private val _estado = MutableStateFlow(EstadoMusica())
    val estado: StateFlow<EstadoMusica> = _estado.asStateFlow()

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _estado.value = _estado.value.copy(sonando = isPlaying)
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) siguiente(automatico = true)
                if (state == Player.STATE_READY) {
                    val total = duration.takeIf { it > 0 }?.coerceAtMost(DEMO_MS) ?: DEMO_MS
                    _estado.value = _estado.value.copy(duracionMs = total)
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                val titulo = _estado.value.actual?.titulo ?: "la canción"
                _estado.value = _estado.value.copy(error = "No se pudo reproducir «$titulo».", sonando = false)
                // Si hay más canciones, pasa a la siguiente como en el móvil.
                if (_estado.value.indice < _estado.value.cola.lastIndex) siguiente(automatico = true)
            }
        })
    }
    private var avance: Job? = null

    /** Pone [canciones] en la cola y empieza por [inicio]. Si esa misma ya suena, la pausa o reanuda. */
    fun reproducir(canciones: List<CancionDto>, inicio: Int = 0, origen: String) {
        if (canciones.isEmpty()) return
        val e = _estado.value
        val misma = e.origen == origen && e.actual?.id == canciones.getOrNull(inicio)?.id
        if (misma) { alternar(); return }
        _estado.value = EstadoMusica(cola = canciones, indice = inicio.coerceIn(0, canciones.lastIndex), origen = origen)
        cargar()
    }

    fun alternar() {
        if (_estado.value.cola.isEmpty()) return
        if (player.isPlaying) player.pause() else {
            if (player.playbackState == Player.STATE_ENDED || player.playbackState == Player.STATE_IDLE) cargar() else player.play()
        }
    }

    fun siguiente(automatico: Boolean = false) {
        val e = _estado.value
        if (e.indice < e.cola.lastIndex) {
            _estado.value = e.copy(indice = e.indice + 1, error = if (automatico) e.error else null)
            cargar()
        } else if (automatico) {
            // Terminó la última: la cola se queda lista para volver a escucharla.
            player.pause()
            player.seekTo(0)
        }
    }

    fun anterior() {
        val e = _estado.value
        if (player.currentPosition > 3_000 || e.indice == 0) player.seekTo(0)
        else { _estado.value = e.copy(indice = e.indice - 1, error = null); cargar() }
    }

    fun detener() {
        player.stop()
        avance?.cancel()
        _estado.value = EstadoMusica()
    }

    /** Pausa sin cerrar el reproductor (por ejemplo al abrir la transmisión en vivo). */
    fun pausar() { if (player.isPlaying) player.pause() }

    fun liberar() {
        avance?.cancel()
        player.release()
    }

    private fun cargar() {
        val cancion = _estado.value.actual ?: return
        val item = MediaItem.Builder()
            .setUri(Uri.parse(url(cancion.archivoUrl)))
            .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().setEndPositionMs(DEMO_MS).build())
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.play()
        _estado.value = _estado.value.copy(posicionMs = 0, duracionMs = DEMO_MS)
        avance?.cancel()
        avance = scope.launch {
            while (isActive) {
                _estado.value = _estado.value.copy(posicionMs = player.currentPosition.coerceAtLeast(0))
                delay(500)
            }
        }
    }
}
```

---

## Paso 8: Sesión guardada (`TokenJwt.kt` y `SesionGuardadaTv.kt`)

Para no escribir el correo y la contraseña con el control cada vez que se abre la app:

1. Al iniciar sesión se guardan el nombre, el correo y el **JWT cifrado con AES-256-GCM** usando una llave del **Android Keystore** (no se puede sacar del dispositivo), igual que `TokenManager` del móvil.
2. Al abrir la app, `TokenJwt` lee el vencimiento (`exp`) del token; si ya venció (o vence en menos de un minuto) se borra y se pide el login.
3. Con un token vigente se entra directo y el ViewModel lo confirma con el servidor: si responde 401 (al abrir, en Mis boletos, en Comunidad o al escribir en el chat), se cierra la sesión.
4. «Cerrar sesión» en Ajustes borra todo lo guardado.

**`src/main/java/mx/utng/festivaltrack/tv/data/TokenJwt.kt`**

```kotlin
package mx.utng.festivaltrack.tv.data

import java.time.Instant
import java.util.Base64

/**
 * Lectura local del JWT que entrega el backend (`exp` en segundos desde 1970).
 *
 * No valida la firma (eso lo hace el servidor en cada petición): solo sirve para no restaurar
 * al abrir la app una sesión que ya venció.
 */
object TokenJwt {

    /** Momento en que vence el token, o null si no es un JWT o no trae `exp`. */
    fun vencimiento(token: String): Instant? {
        val partes = token.split('.')
        if (partes.size != 3) return null
        val json = runCatching { String(Base64.getUrlDecoder().decode(partes[1].trimEnd('=')), Charsets.UTF_8) }
            .getOrNull() ?: return null
        val exp = Regex(""""exp"\s*:\s*(\d+)""").find(json)?.groupValues?.get(1)?.toLongOrNull() ?: return null
        return Instant.ofEpochSecond(exp)
    }

    /**
     * true si el token todavía sirve en [ahora], con un margen de un minuto para no entrar con
     * uno que vence enseguida. Un token sin `exp` se da por vigente (el servidor decide).
     */
    fun vigente(token: String, ahora: Instant = Instant.now()): Boolean {
        if (token.isBlank()) return false
        val vence = vencimiento(token) ?: return true
        return vence.isAfter(ahora.plusSeconds(60))
    }
}
```

**`src/main/java/mx/utng/festivaltrack/tv/data/SesionGuardadaTv.kt`**

```kotlin
package mx.utng.festivaltrack.tv.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Datos de la cuenta que se guardan en la TV. */
data class DatosSesion(val nombre: String, val correo: String, val token: String)

/**
 * Guarda la sesión de la TV para no pedir el correo y la contraseña cada vez que se abre la app.
 *
 * Igual que `TokenManager` del móvil: el JWT se cifra con AES-256-GCM con una llave del Android
 * Keystore (no se puede sacar del dispositivo). Si el token venció o no se puede descifrar, se
 * borra y se vuelve a pedir el inicio de sesión.
 */
class SesionGuardadaTv(context: Context) {

    private val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    /** La sesión guardada si sigue vigente; si no hay o ya venció, null (y se borra lo que hubiera). */
    fun leer(): DatosSesion? {
        val cifrado = prefs.getString(KEY_TOKEN, null) ?: return null
        val token = descifrar(cifrado)
        if (token == null || !TokenJwt.vigente(token)) {
            borrar()
            return null
        }
        return DatosSesion(
            nombre = prefs.getString(KEY_NOMBRE, null).orEmpty(),
            correo = prefs.getString(KEY_CORREO, null).orEmpty(),
            token = token
        )
    }

    fun guardar(datos: DatosSesion) {
        val cifrado = runCatching { cifrar(datos.token) }.getOrNull() ?: return
        prefs.edit()
            .putString(KEY_TOKEN, cifrado)
            .putString(KEY_NOMBRE, datos.nombre)
            .putString(KEY_CORREO, datos.correo)
            .apply()
    }

    fun borrar() {
        prefs.edit().clear().apply()
    }

    private fun cifrar(texto: String): String {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.ENCRYPT_MODE, llave())
        val datos = cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + datos, Base64.NO_WRAP)
    }

    private fun descifrar(guardado: String): String? = try {
        val bytes = Base64.decode(guardado, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(TAG_BITS, bytes.copyOfRange(0, IV_BYTES)))
        String(cipher.doFinal(bytes.copyOfRange(IV_BYTES, bytes.size)), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }

    private fun llave(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS_LLAVE, null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS_LLAVE, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    private companion object {
        const val ARCHIVO = "festivaltrack_tv_sesion"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS_LLAVE = "festivaltrack_tv_sesion"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val KEY_TOKEN = "jwt_cifrado"
        const val KEY_NOMBRE = "nombre"
        const val KEY_CORREO = "correo"
    }
}
```

---

## Paso 9: Menú lateral (`SidebarMenuItem.kt` y `TvSidebar.kt`)

Un solo menú para toda la app (antes cada pantalla tenía su copia). Cada opción se resalta al enfocarla con el control; marca «EN VIVO» junto a la transmisión cuando de verdad hay una y abajo muestra la cuenta (o «Invitado») y el botón de comprar boletos.

**`src/main/java/mx/utng/festivaltrack/tv/presentation/components/SidebarMenuItem.kt`**

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
import androidx.compose.ui.unit.Dp
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
    onClick: () -> Unit,
    alto: Dp = 44.dp
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) FestivalGold else if (isFocused) Color.White.copy(alpha = 0.1f) else Color.Transparent,
        contentColor = if (isSelected) Color.Black else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(alto)
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

**`src/main/java/mx/utng/festivaltrack/tv/presentation/components/TvSidebar.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.ui.theme.*

/** Secciones de la app de TV, en el orden del menú. */
enum class SeccionTv(val titulo: String) {
    INICIO("Inicio"),
    PROGRAMACION("Programación"),
    EN_VIVO("Transmisión en vivo"),
    BIOGRAFIA("Biografía y música"),
    MAPA("Mapa"),
    GALERIA("Galería"),
    COMUNIDAD("Comunidad"),
    BOLETOS("Mis boletos"),
    AJUSTES("Ajustes")
}

/**
 * Menú lateral único para todas las pantallas de la TV (antes cada pantalla tenía su copia).
 *
 * @param seccion Sección activa.
 * @param enVivo Marca "EN VIVO" junto a la transmisión cuando de verdad hay una.
 * @param usuario Nombre de la sesión; null = invitado.
 * @param onComprarBoletos Abre el QR para comprar boletos desde el celular.
 */
@Composable
fun TvSidebar(
    seccion: SeccionTv,
    onSeccion: (SeccionTv) -> Unit,
    enVivo: Boolean,
    usuario: String?,
    onComprarBoletos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(FestivalSidebarBg)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text("Festival José Alfredo Jiménez", color = FestivalGold, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
        Text("Dolores Hidalgo, Gto.", color = FestivalTextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(10.dp))

        // Las secciones ocupan el espacio libre y se desplazan si la pantalla es baja:
        // el usuario y "Comprar boletos" siempre quedan visibles abajo.
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            val iconos = mapOf(
                SeccionTv.INICIO to Icons.Default.Home,
                SeccionTv.PROGRAMACION to Icons.Default.Event,
                SeccionTv.EN_VIVO to Icons.Default.LiveTv,
                SeccionTv.BIOGRAFIA to Icons.Default.LibraryMusic,
                SeccionTv.MAPA to Icons.Default.Map,
                SeccionTv.GALERIA to Icons.Default.Collections,
                SeccionTv.COMUNIDAD to Icons.Default.Forum,
                SeccionTv.BOLETOS to Icons.Default.ConfirmationNumber,
                SeccionTv.AJUSTES to Icons.Default.Settings
            )
            SeccionTv.entries.forEach { item ->
                Box {
                    SidebarMenuItem(
                        label = item.titulo,
                        icon = iconos.getValue(item),
                        isSelected = seccion == item,
                        onClick = { onSeccion(item) },
                        alto = 38.dp
                    )
                    if (item == SeccionTv.EN_VIVO && enVivo) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 10.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(FestivalBadgeLive)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    usuario ?: "Invitado",
                    color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onComprarBoletos,
                colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("COMPRAR BOLETOS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
```

---

## Paso 10: Pantalla de inicio de sesión `TvLoginScreen.kt`

Entra de verdad contra el backend con los mismos mensajes que el móvil. «Continuar sin cuenta» deja ver toda la TV como invitado (la cuenta solo hace falta para el chat y Mis boletos). Para crear cuenta, un QR abre el registro de la web en el celular.

**`src/main/java/mx/utng/festivaltrack/tv/presentation/screens/TvLoginScreen.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.util.EnlacesWeb
import mx.utng.festivaltrack.tv.ui.theme.*
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode

/**
 * Inicio de sesión en la TV.
 *
 * - Entra de verdad contra el backend (antes cualquier dato dejaba pasar) y muestra los mismos
 *   mensajes que el móvil: "Correo o contraseña incorrectos", "Sin conexión…".
 * - "Continuar sin cuenta": la TV se puede ver completa como invitado; la cuenta solo hace
 *   falta para escribir en el chat.
 * - Crear cuenta: con el control es incómodo, así que un QR real abre el registro en el celular.
 *
 * @param iniciarSesion Intenta entrar; regresa null si entró o el mensaje de error.
 */
@Composable
fun TvLoginScreen(
    iniciarSesion: suspend (correo: String, contrasena: String) -> String?,
    onEntrar: () -> Unit,
    onContinuarSinCuenta: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var ayudaContrasena by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val camposColores = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = FestivalGold,
        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
        focusedLabelColor = FestivalGold,
        unfocusedLabelColor = Color.Gray,
        focusedContainerColor = FestivalCardBg,
        unfocusedContainerColor = FestivalCardBg,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = FestivalGold
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF2C2114), FestivalDarkBg), radius = 1200f))
            .padding(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bienvenida y QR para crear cuenta
            Column(modifier = Modifier.weight(1f).padding(end = 32.dp), verticalArrangement = Arrangement.Center) {
                Text("FESTIVAL JOSÉ ALFREDO JIMÉNEZ", color = FestivalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text("Bienvenido al\nFestival", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 42.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Mira la programación, la galería y las transmisiones en vivo desde Dolores Hidalgo.",
                    color = FestivalTextSecondary, fontSize = 13.sp
                )
                Spacer(Modifier.height(28.dp))
                Card(colors = CardDefaults.cardColors(containerColor = FestivalCardBg), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        DynamicQrCode(content = EnlacesWeb.registro(), modifier = Modifier.size(120.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("¿No tienes cuenta?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Escanea con tu celular para crearla", color = FestivalGold, fontSize = 12.sp)
                            Text(EnlacesWeb.registro(), color = FestivalTextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Formulario
            Card(
                colors = CardDefaults.cardColors(containerColor = FestivalSidebarBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.width(440.dp).border(1.dp, FestivalGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Iniciar sesión", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = null },
                        label = { Text("CORREO ELECTRÓNICO") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = FestivalGold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        colors = camposColores,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text("CONTRASEÑA") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = FestivalGold) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = camposColores,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = FestivalBadgeLive, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            cargando = true
                            scope.launch {
                                val resultado = iniciarSesion(email, password)
                                cargando = false
                                if (resultado == null) onEntrar() else error = resultado
                            }
                        },
                        enabled = !cargando,
                        colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (cargando) CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        else Text("Iniciar sesión", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onContinuarSinCuenta,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Continuar sin cuenta", color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { ayudaContrasena = true }) {
                        Text("¿Olvidaste tu contraseña?", color = FestivalTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("(OK) SELECCIONAR", color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("(▲/▼) NAVEGAR", color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("(BACK) REGRESAR", color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (ayudaContrasena) {
        AlertDialog(
            onDismissRequest = { ayudaContrasena = false },
            containerColor = FestivalCardBg,
            title = { Text("¿Olvidaste tu contraseña?", color = FestivalGold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Pide al equipo del festival que restablezca tu acceso desde el panel de administración, o crea una cuenta nueva con otro correo.",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = { ayudaContrasena = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)
                ) { Text("Entendido", fontWeight = FontWeight.Bold) }
            }
        )
    }
}
```

---

## Paso 11: Pantalla principal `TvMainScreen.kt` e imagen de cada concierto `PortadaConcierto.kt`

El Inicio muestra el próximo concierto (o la transmisión en curso) y un carrusel con los próximos conciertos. Cada tarjeta (y el encabezado del detalle del concierto) usa `PortadaConcierto`:

1. Si el concierto tiene `bannerUrl` (ruta `/uploads/...` del backend o URL https), se descarga con Coil.
2. Mientras carga, si falla o si no tiene, se usa una foto del festival elegida por `fondoDe`: primero por el nombre (homenaje a «El Rey», mariachi, bohemia) y luego por el lugar (Mausoleo, Casa Museo, Plaza, Escenario Principal).

**`src/main/java/mx/utng/festivaltrack/tv/presentation/screens/TvMainScreen.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.R
import mx.utng.festivaltrack.tv.presentation.components.PortadaConcierto
import mx.utng.festivaltrack.tv.ui.theme.*
import java.time.Instant

/**
 * Inicio de la TV.
 *
 * - Banner: si de verdad hay transmisión, "EN VIVO AHORA: <concierto>" con su botón; si no,
 *   el próximo concierto con su fecha y hora de México y el botón para comprar boletos.
 * - Carrusel con los próximos conciertos reales (sin cancelados ni pasados).
 * - Arriba, la hora actual en Dolores Hidalgo.
 *
 * @param onComprar Abre la compra con QR; recibe el concierto a preseleccionar (o null).
 */
@Composable
fun TvMainScreen(
    proximos: List<EventoEntity>,
    stream: StreamStatusDto?,
    ahora: Instant,
    usuario: String?,
    onVerEnVivo: () -> Unit,
    onVerProgramacion: () -> Unit,
    onComprar: (String?) -> Unit,
    onAbrirConcierto: (String) -> Unit,
    onEscucharMusica: () -> Unit
) {
    val enVivo = stream?.isLive == true
    val siguiente = proximos.firstOrNull()

    // Se desplaza (con el foco del control) cuando la barra de música le quita espacio.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        // Barra superior: saludo y hora real
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (usuario != null) "Hola, $usuario" else "Bienvenido al festival",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Text(
                "Dolores Hidalgo · ${HorarioFestival.fechaActual(ahora)} · ${HorarioFestival.horaActual(ahora)}",
                color = FestivalTextSecondary, fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Banner principal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.hero_dolores_hidalgo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.95f), Color.Black.copy(alpha = 0.45f))))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        if (enVivo) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(FestivalBadgeLive))
                                Spacer(Modifier.width(8.dp))
                                Text("EN VIVO AHORA", color = FestivalBadgeLive, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stream?.eventoNombre ?: "Transmisión del festival",
                                color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold
                            )
                            Text("Míralo en esta pantalla y platica en el chat.", color = FestivalTextSecondary, fontSize = 13.sp)
                        } else if (siguiente != null) {
                            Text(
                                if (HorarioFestival.enCurso(siguiente, ahora)) "EN CURSO" else "PRÓXIMO CONCIERTO",
                                color = FestivalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(siguiente.nombre, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${HorarioFestival.cuando(siguiente, ahora)} • ${siguiente.escenario ?: siguiente.ubicacion}",
                                color = FestivalTextSecondary, fontSize = 14.sp
                            )
                        } else {
                            Text("FESTIVAL JOSÉ ALFREDO JIMÉNEZ", color = FestivalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("El Rey vive en su tierra", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text("Todavía no hay conciertos programados.", color = FestivalTextSecondary, fontSize = 14.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (enVivo) {
                            Button(
                                onClick = onVerEnVivo,
                                colors = ButtonDefaults.buttonColors(containerColor = FestivalBadgeLive, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Ver en vivo", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (siguiente != null) {
                            Button(
                                onClick = { onComprar(siguiente.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Comprar boletos", fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(onClick = onVerProgramacion, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Ver programación", color = Color.White)
                        }
                        // Como la tarjeta "Audio" del inicio del móvil: suena el catálogo del artista.
                        OutlinedButton(onClick = onEscucharMusica, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Escuchar música", color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("PRÓXIMOS EN EL ESCENARIO", color = FestivalGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(12.dp))

        if (proximos.isEmpty()) {
            Text(
                "Cuando haya conciertos programados aparecerán aquí.",
                color = FestivalTextSecondary, fontSize = 14.sp
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                items(proximos, key = { it.id }) { evento ->
                    TvEventCard(evento = evento, ahora = ahora, enVivo = enVivo && stream?.eventoId == evento.id, onClick = {
                        if (enVivo && stream?.eventoId == evento.id) onVerEnVivo() else onAbrirConcierto(evento.id)
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Elige un concierto y pulsa OK para escuchar su repertorio y comprar boletos",
            color = FestivalTextSecondary, fontSize = 11.sp, modifier = Modifier.align(Alignment.End)
        )
    }
}

/**
 * Tarjeta de concierto del carrusel con su imagen ([PortadaConcierto]); se resalta con borde
 * dorado al enfocarla con el control.
 */
@Composable
fun TvEventCard(evento: EventoEntity, ahora: Instant, enVivo: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        color = if (isFocused) Color(0xFF2E3D30) else FestivalCardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(240.dp)
            .height(140.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) FestivalGold else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PortadaConcierto(evento, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)))))
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = when {
                        enVivo -> FestivalBadgeLive
                        else -> FestivalGold
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (enVivo) "EN VIVO" else "${HorarioFestival.fechaCorta(evento)} · ${HorarioFestival.hora(evento)}",
                        color = if (enVivo) Color.White else Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Column {
                    Text(evento.nombre, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(evento.escenario ?: evento.ubicacion, color = FestivalTextSecondary, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}
```

**`src/main/java/mx/utng/festivaltrack/tv/presentation/components/PortadaConcierto.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.tv.R
import java.text.Normalizer

/** Fotos del festival incluidas en la app para los conciertos que no tienen imagen propia. */
enum class FondoConcierto(@DrawableRes val imagen: Int) {
    MARIACHI(R.drawable.mariachi_gala_stage),
    EL_REY(R.drawable.gal_el_rey_escenario),
    BOHEMIA(R.drawable.gal_servilleta_cantina),
    PLAZA(R.drawable.gal_plaza_dolores),
    MAUSOLEO(R.drawable.gal_mausoleo_sarape),
    CASA_MUSEO(R.drawable.gal_casa_museo),
    GUITARRA(R.drawable.ranchera_guitar),
    FESTIVAL(R.drawable.gal_banner_festival)
}

private fun normalizar(texto: String?): String =
    Normalizer.normalize(texto.orEmpty(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()

private fun contiene(texto: String, vararg palabras: String) =
    palabras.any { Regex("\\b$it\\b").containsMatchIn(texto) }

/**
 * Elige la foto local de un concierto: primero por el tipo de concierto que dice su nombre
 * (homenaje a "El Rey", mariachi, bohemia) y luego por el lugar (Mausoleo, Casa Museo, Plaza,
 * Escenario Principal). Si nada coincide, una de las fotos generales, siempre la misma para el
 * mismo concierto (según su id), para que las tarjetas no se vean todas iguales.
 */
fun fondoDe(id: String, nombre: String, escenario: String?, ubicacion: String?): FondoConcierto {
    val n = normalizar(nombre)
    when {
        contiene(n, "rey") -> return FondoConcierto.EL_REY
        contiene(n, "mariachi", "mariachis", "gala") -> return FondoConcierto.MARIACHI
        contiene(n, "bohemia", "cantina") -> return FondoConcierto.BOHEMIA
    }
    val lugar = normalizar(listOfNotNull(escenario, ubicacion, nombre).joinToString(" "))
    return when {
        contiene(lugar, "mausoleo", "panteon") -> FondoConcierto.MAUSOLEO
        lugar.contains("casa museo") -> FondoConcierto.CASA_MUSEO
        contiene(lugar, "plaza", "jardin") -> FondoConcierto.PLAZA
        contiene(lugar, "escenario") -> FondoConcierto.MARIACHI
        else -> GENERALES[Math.floorMod(id.hashCode(), GENERALES.size)]
    }
}

private val GENERALES = listOf(FondoConcierto.FESTIVAL, FondoConcierto.GUITARRA, FondoConcierto.PLAZA)

/** URL completa de la imagen del concierto, o null si no tiene (las rutas `/uploads/...` son del backend). */
fun urlBanner(bannerUrl: String?, servidor: String): String? {
    val ruta = bannerUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (ruta.startsWith("/")) servidor.removeSuffix("/") + ruta else ruta
}

/**
 * Imagen de un concierto: su `bannerUrl` si el administrador le puso una; mientras carga, si
 * falla o si no tiene, la foto local de [fondoDe].
 */
@Composable
fun PortadaConcierto(evento: EventoEntity, modifier: Modifier = Modifier) {
    val local = painterResource(fondoDe(evento.id, evento.nombre, evento.escenario, evento.ubicacion).imagen)
    AsyncImage(
        model = urlBanner(evento.bannerUrl, FestivalApiService.SERVER_URL),
        contentDescription = evento.nombre,
        placeholder = local,
        error = local,
        fallback = local,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
```

---

## Paso 12: Detalle de concierto `TvDetalleConcierto.kt`

Se abre al pulsar OK sobre un concierto. El encabezado (nombre, fecha y lugar en hora de México) va sobre la imagen del concierto, la misma de su tarjeta, oscurecida con un degradado para que se lea. Debajo, su repertorio con demos para escuchar (hasta 200 dp de alto, para que todo quepa en una TV de 720p) y a la derecha el QR para comprar boletos.

**`src/main/java/mx/utng/festivaltrack/tv/presentation/components/TvDetalleConcierto.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.util.EnlacesWeb
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.presentation.viewmodel.EstadoMusica
import mx.utng.festivaltrack.tv.ui.theme.*
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode
import java.time.Instant

/**
 * Detalle de un concierto, como en el móvil: su imagen ([PortadaConcierto]) de fondo en el
 * encabezado con la fecha y el lugar en hora de México, su repertorio con demos de 30 s para
 * escuchar y el QR para comprar boletos desde el celular.
 *
 * @param cargarRepertorio Pide al servidor las canciones del concierto (null = sin conexión).
 */
@Composable
fun TvDetalleConcierto(
    evento: EventoEntity,
    ahora: Instant,
    enVivo: Boolean,
    musica: EstadoMusica,
    cargarRepertorio: suspend (String) -> List<CancionDto>?,
    onReproducir: (List<CancionDto>, Int) -> Unit,
    onVerEnVivo: () -> Unit,
    onCerrar: () -> Unit
) {
    var repertorio by remember(evento.id) { mutableStateOf<List<CancionDto>?>(null) }
    var sinConexion by remember(evento.id) { mutableStateOf(false) }
    val primerFoco = remember { FocusRequester() }
    val url = EnlacesWeb.comprar(evento.id)

    LaunchedEffect(evento.id) {
        val lista = cargarRepertorio(evento.id)
        sinConexion = lista == null
        repertorio = lista.orEmpty()
    }
    LaunchedEffect(Unit) { runCatching { primerFoco.requestFocus() } }

    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = FestivalSidebarBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .border(1.dp, FestivalGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f)) {
                    // Encabezado sobre la imagen del concierto (la misma de su tarjeta en el Inicio),
                    // oscurecida a la izquierda para que el texto se lea.
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
                        PortadaConcierto(evento, Modifier.matchParentSize())
                        Box(
                            Modifier.matchParentSize().background(
                                Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.88f), Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.2f)))
                            )
                        )
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                if (enVivo) "EN VIVO AHORA" else if (HorarioFestival.enCurso(evento, ahora)) "EN CURSO" else "CONCIERTO DEL FESTIVAL",
                                color = if (enVivo) FestivalBadgeLive else FestivalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
                            )
                            Text(evento.nombre, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(HorarioFestival.cuando(evento, ahora), color = Color.White, fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(listOfNotNull(evento.escenario, evento.ubicacion).distinct().joinToString(" · "), color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Repertorio", color = FestivalGold, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        val lista = repertorio.orEmpty()
                        if (lista.isNotEmpty()) {
                            TvBoton(
                                texto = if (musica.origen == evento.nombre && musica.sonando) "Pausar" else "Escuchar todo",
                                icono = if (musica.origen == evento.nombre && musica.sonando) Icons.Default.Pause else Icons.Default.PlayArrow,
                                onClick = { onReproducir(lista, 0) },
                                modifier = Modifier.focusRequester(primerFoco)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    when {
                        repertorio == null -> CircularProgressIndicator(color = FestivalGold, modifier = Modifier.size(28.dp))
                        sinConexion -> Text("No se pudo cargar el repertorio. Revisa la conexión.", color = FestivalTextSecondary, fontSize = 13.sp)
                        repertorio!!.isEmpty() -> Text("Este concierto todavía no tiene canciones para escuchar.", color = FestivalTextSecondary, fontSize = 13.sp)
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 200.dp)) {
                            itemsIndexed(repertorio!!, key = { _, c -> c.id }) { i, cancion ->
                                TvFilaCancion(
                                    numero = i + 1,
                                    cancion = cancion,
                                    sonando = musica.origen == evento.nombre && musica.sonando && musica.actual?.id == cancion.id,
                                    onClick = { onReproducir(repertorio!!, i) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (enVivo) TvBoton("Ver en vivo", Icons.Default.LiveTv, onVerEnVivo)
                        TvBoton("Cerrar", null, onCerrar, principal = false)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(240.dp)) {
                    Text("Compra tus boletos", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Escanea con tu celular", color = FestivalTextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    DynamicQrCode(content = url, modifier = Modifier.size(220.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(url, color = FestivalTextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
```

---

## Paso 13: Compra de boletos con QR (`TvComprarBoletos.kt` y `DynamicQrCode.kt`)

Con el control no se puede pagar: se elige el concierto y el QR abre `EnlacesWeb.comprar(id)` (`/checkout/<id>` de la web) en el celular. `DynamicQrCode` dibuja un QR real con la matriz de ZXing de `CodigoQr` (la misma de los boletos del móvil).

**`src/main/java/mx/utng/festivaltrack/tv/presentation/components/TvComprarBoletos.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.PreciosBoleto
import mx.utng.festivaltrack.shared.util.EnlacesWeb
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.ui.theme.*
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode
import java.text.NumberFormat
import java.time.Instant
import java.util.Locale

/**
 * Comprar boletos desde la TV: con el control no se puede pagar, así que se elige el concierto y
 * se escanea un QR real que abre su compra en la web desde el celular.
 *
 * @param conciertos Próximos conciertos (ya filtrados y ordenados).
 * @param preseleccionado Concierto que se elige al abrir (por ejemplo desde su tarjeta).
 */
@Composable
fun TvComprarBoletosDialog(
    conciertos: List<EventoEntity>,
    ahora: Instant,
    preseleccionado: String? = null,
    onCerrar: () -> Unit
) {
    var elegidoId by remember { mutableStateOf(preseleccionado ?: conciertos.firstOrNull()?.id) }
    val elegido = conciertos.firstOrNull { it.id == elegidoId } ?: conciertos.firstOrNull()
    val url = elegido?.let { EnlacesWeb.comprar(it.id) } ?: EnlacesWeb.cartelera()
    val primerFoco = remember { FocusRequester() }
    val pesos = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")).apply { maximumFractionDigits = 0 } }

    LaunchedEffect(Unit) { runCatching { primerFoco.requestFocus() } }

    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = FestivalSidebarBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .border(1.dp, FestivalGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                // Lista de conciertos
                Column(modifier = Modifier.weight(1f)) {
                    Text("Comprar boletos", color = FestivalGold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Elige el concierto con el control y escanea el código con tu celular para pagar ahí.",
                        color = FestivalTextSecondary, fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (conciertos.isEmpty()) {
                        Text("No hay conciertos próximos a la venta.", color = Color.White, fontSize = 15.sp)
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 300.dp)) {
                        items(conciertos, key = { it.id }) { concierto ->
                            var enfocado by remember { mutableStateOf(false) }
                            val esElegido = concierto.id == elegido?.id
                            Surface(
                                onClick = { elegidoId = concierto.id },
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    esElegido -> Color(0xFF2C2214)
                                    enfocado -> Color(0xFF2E3D30)
                                    else -> FestivalCardBg
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (concierto.id == conciertos.first().id) Modifier.focusRequester(primerFoco) else Modifier)
                                    .onFocusChanged {
                                        enfocado = it.isFocused
                                        // Al moverse con las flechas el QR sigue al concierto enfocado.
                                        if (it.isFocused) elegidoId = concierto.id
                                    }
                                    .border(
                                        if (enfocado || esElegido) 2.dp else 0.dp,
                                        if (enfocado || esElegido) FestivalGold else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(concierto.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        "${HorarioFestival.cuando(concierto, ahora)} • ${concierto.escenario ?: concierto.ubicacion}",
                                        color = FestivalTextSecondary, fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "VIP ${pesos.format(PreciosBoleto.VIP)} · General ${pesos.format(PreciosBoleto.GENERAL)}",
                            color = FestivalGold, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = onCerrar, shape = RoundedCornerShape(8.dp)) {
                            Text("Cerrar", color = Color.White)
                        }
                    }
                }

                // QR del concierto elegido
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(260.dp)
                ) {
                    DynamicQrCode(content = url, modifier = Modifier.size(240.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        elegido?.nombre ?: "Cartelera del festival",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center
                    )
                    Text(url, color = FestivalTextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/**
 * Diálogo con un QR y un texto, para acciones que se hacen en el celular (crear cuenta, etc.).
 */
@Composable
fun TvDialogoQr(titulo: String, texto: String, url: String, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = FestivalCardBg,
        title = { Text(titulo, color = FestivalGold, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(texto, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                DynamicQrCode(content = url, modifier = Modifier.size(200.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(url, color = FestivalTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(onClick = onCerrar, colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)) {
                Text("Listo", fontWeight = FontWeight.Bold)
            }
        }
    )
}
```

**`src/main/java/mx/utng/festivaltrack/tv/ui/utils/DynamicQrCode.kt`**

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mx.utng.festivaltrack.shared.util.CodigoQr

/**
 * Código QR real del [content] para escanear con el celular desde el sillón
 * (por ejemplo la página de compra de un concierto).
 *
 * Antes se dibujaba un patrón parecido a un QR que ningún lector podía leer; ahora la matriz
 * sale de [CodigoQr] (ZXing), la misma que usan los boletos del móvil.
 */
@Composable
fun DynamicQrCode(
    content: String,
    modifier: Modifier = Modifier,
    foregroundColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val matriz = remember(content) { runCatching { CodigoQr.matriz(content, margen = 1) }.getOrNull() }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(10.dp)
    ) {
        if (matriz == null) return@Box
        Canvas(modifier = Modifier.fillMaxSize()) {
            val celda = size.width / matriz.size
            for (fila in matriz.indices) {
                for (columna in matriz[fila].indices) {
                    if (matriz[fila][columna]) {
                        drawRect(
                            color = foregroundColor,
                            topLeft = Offset(columna * celda, fila * celda),
                            size = Size(celda + 0.5f, celda + 0.5f)
                        )
                    }
                }
            }
        }
    }
}
```

---

## Paso 14: Programación `TvScheduleScreen.kt`

Los próximos conciertos agrupados por día («Hoy», «Mañana», fecha), con «EN CURSO» y «EN VIVO». OK sobre uno abre su detalle (o la transmisión, si está en vivo).

**`src/main/java/mx/utng/festivaltrack/tv/presentation/screens/TvScheduleScreen.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.ui.theme.*
import java.time.Instant

/**
 * Programación del festival: los próximos conciertos reales agrupados por día, con la hora
 * de México. Marca "EN CURSO" el que ya empezó y "EN VIVO" el que se está transmitiendo.
 * Al pulsar OK sobre uno se abre su compra con QR (o la transmisión, si está en vivo).
 */
@Composable
fun TvScheduleScreen(
    proximos: List<EventoEntity>,
    stream: StreamStatusDto?,
    ahora: Instant,
    onAbrir: (String) -> Unit,
    onVerEnVivo: () -> Unit
) {
    val porDia = remember(proximos, ahora) { proximos.groupBy { HorarioFestival.dia(it, ahora) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
            .padding(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Programación del festival", color = FestivalGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Horarios del centro de México · Dolores Hidalgo", color = FestivalTextSecondary, fontSize = 12.sp)
            }
            Surface(color = FestivalCardBg, shape = RoundedCornerShape(8.dp)) {
                Text(
                    "${HorarioFestival.fechaActual(ahora).uppercase()} · ${HorarioFestival.horaActual(ahora)}",
                    color = FestivalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (proximos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.EventBusy, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("Aún no hay conciertos programados", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Cuando se publiquen aparecerán aquí con su horario.", color = FestivalTextSecondary, fontSize = 14.sp)
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            porDia.forEach { (dia, conciertos) ->
                item(key = "dia-$dia") {
                    Text(
                        dia.replaceFirstChar { it.uppercase() },
                        color = FestivalGold, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(conciertos, key = { it.id }) { evento ->
                    val transmitiendo = stream?.isLive == true && stream.eventoId == evento.id
                    ScheduleCard(
                        evento = evento,
                        enCurso = HorarioFestival.enCurso(evento, ahora),
                        enVivo = transmitiendo,
                        onClick = { if (transmitiendo) onVerEnVivo() else onAbrir(evento.id) }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de un concierto en la programación; se resalta al enfocarla con el control.
 */
@Composable
fun ScheduleCard(evento: EventoEntity, enCurso: Boolean, enVivo: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val destacado = enCurso || enVivo

    Surface(
        onClick = onClick,
        color = when {
            isFocused -> Color(0xFF2E3D30)
            destacado -> Color(0xFF2C2214)
            else -> FestivalCardBg
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 3.dp else if (destacado) 1.dp else 0.dp,
                color = if (isFocused) FestivalGold else if (destacado) FestivalGold.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(color = if (destacado) FestivalGold else FestivalSidebarBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        "${HorarioFestival.hora(evento)} HRS",
                        color = if (destacado) Color.Black else FestivalGold,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(evento.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (enVivo || enCurso) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(color = FestivalBadgeLive, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    if (enVivo) "EN VIVO" else "EN CURSO",
                                    color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        listOfNotNull(evento.artistaNombre, evento.escenario ?: evento.ubicacion).joinToString(" • "),
                        color = FestivalTextSecondary, fontSize = 12.sp
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (enVivo) Icons.Default.LiveTv else Icons.Default.ConfirmationNumber,
                    contentDescription = null, tint = FestivalGold, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (enVivo) "Ver en vivo" else "Ver detalle", color = FestivalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
```

---

## Paso 15: Transmisión en vivo y chat `TvLiveStreamScreen.kt`

1. El teléfono del administrador transmite por RTSP (puerto 1935) y avisa al backend (`stream/status`) con su IP real (`streamUrl`) y la del emulador (`emulatorUrl`).
2. La TV prueba esas direcciones en orden; si una falla o se queda 12 s sin imagen, pasa a la siguiente y vuelve a empezar mientras la transmisión siga activa.
3. Sin transmisión, explica cuál es el próximo concierto y cuándo empieza.
4. El chat es el del concierto que se transmite (se actualiza cada 3 s). Para escribir hace falta iniciar sesión; el envío lo hace el ViewModel (`enviarMensajeChat`), que cierra la sesión si el servidor ya no acepta el token y muestra «Tu sesión venció. Inicia sesión de nuevo para escribir.».

**`src/main/java/mx/utng/festivaltrack/tv/presentation/screens/TvLiveStreamScreen.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.ChatMessageDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.presentation.viewmodel.SesionTv
import mx.utng.festivaltrack.tv.ui.theme.*
import java.time.Instant

/**
 * Transmisión en vivo en la TV, con el mismo comportamiento que el móvil:
 *
 * - Prueba primero la IP real del teléfono del administrador y luego la del emulador; si una
 *   falla pasa a la siguiente y reintenta solo mientras la transmisión siga activa. Ya no se
 *   cambia a un video de demostración: si no llega la señal se dice.
 * - Sin transmisión, explica cuál es el próximo concierto y cuándo empieza.
 * - El chat es el del concierto que se transmite. Para escribir hace falta iniciar sesión
 *   (el servidor lo exige); sin sesión se ofrece entrar.
 *
 * @param enviarMensaje Envía (eventoId, texto) con la sesión actual; regresa null si se envió o
 *   el texto del error. Lo hace el ViewModel para cerrar la sesión si el token ya no sirve.
 */
@Composable
fun TvLiveStreamScreen(
    stream: StreamStatusDto?,
    proximos: List<EventoEntity>,
    ahora: Instant,
    sesion: SesionTv?,
    enviarMensaje: suspend (String, String) -> String?,
    onIniciarSesion: () -> Unit,
    onVerProgramacion: () -> Unit
) {
    val enVivo = stream?.isLive == true
    val urls = remember(stream?.streamUrl, stream?.emulatorUrl, enVivo) {
        if (!enVivo) emptyList()
        else listOfNotNull(stream?.streamUrl, stream?.emulatorUrl).filter { it.isNotBlank() }.distinct()
    }
    val eventoChat = if (enVivo) stream?.eventoId else null

    Row(modifier = Modifier.fillMaxSize().background(FestivalDarkBg)) {
        // Video (68 %)
        Column(modifier = Modifier.weight(0.68f).fillMaxHeight().padding(20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                when {
                    stream == null -> AvisoSinVideo(
                        icono = Icons.Default.CloudOff,
                        titulo = "Sin conexión con el servidor",
                        texto = "Revisa que la TV esté conectada a la red. Se vuelve a intentar sola."
                    )
                    !enVivo || urls.isEmpty() -> {
                        val siguiente = proximos.firstOrNull()
                        AvisoSinVideo(
                            icono = Icons.Default.LiveTv,
                            titulo = "No hay transmisión en este momento",
                            texto = if (siguiente != null)
                                "Próximo concierto: ${siguiente.nombre}\n${HorarioFestival.cuando(siguiente, ahora)} · ${siguiente.escenario ?: siguiente.ubicacion}\n\nEl video aparecerá aquí solo en cuanto empiece."
                            else "El video aparecerá aquí solo en cuanto el festival transmita un concierto."
                        )
                    }
                    else -> ReproductorTv(urls = urls)
                }

                if (enVivo) {
                    // Fondo oscuro para que el nombre se lea aunque el video sea claro.
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = FestivalBadgeLive, shape = RoundedCornerShape(4.dp)) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                                Spacer(Modifier.width(6.dp))
                                Text("EN VIVO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stream?.eventoNombre ?: "Festival José Alfredo Jiménez",
                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onVerProgramacion, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ver programación", color = Color.White)
                }
            }
        }

        // Chat (32 %)
        ChatTv(
            eventoId = eventoChat,
            sesion = sesion,
            enviarMensaje = enviarMensaje,
            onIniciarSesion = onIniciarSesion,
            modifier = Modifier.weight(0.32f).fillMaxHeight().background(FestivalSidebarBg).padding(20.dp)
        )
    }
}

@Composable
private fun AvisoSinVideo(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, texto: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icono, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(titulo, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(texto, color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
    }
}

/**
 * Reproduce el RTSP del administrador. Si una dirección falla (o se queda sin imagen 12 s)
 * prueba la siguiente; tras recorrerlas todas espera 5 s y vuelve a empezar.
 */
@OptIn(UnstableApi::class)
@Composable
private fun ReproductorTv(urls: List<String>) {
    val context = LocalContext.current
    var intento by remember(urls) { mutableIntStateOf(0) }
    var reproduciendo by remember(urls) { mutableStateOf(false) }
    val url = urls[intento % urls.size]

    val player = remember(url, intento) {
        // En vivo no hace falta acumular video: empieza con poco búfer para ir casi al tiempo real.
        val cargaEnVivo = DefaultLoadControl.Builder().setBufferDurationsMs(500, 2_000, 300, 500).build()
        ExoPlayer.Builder(context).setLoadControl(cargaEnVivo).build().apply {
            setMediaSource(
                RtspMediaSource.Factory().setForceUseRtpTcp(true).setTimeoutMs(8_000)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(url)))
            )
            playWhenReady = true
            prepare()
        }
    }
    var fallo by remember(player) { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                reproduciendo = state == Player.STATE_READY
            }
            override fun onPlayerError(error: PlaybackException) {
                reproduciendo = false
                fallo = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    // Conexión colgada sin error ni imagen: se reintenta en lugar de esperar para siempre.
    LaunchedEffect(player) {
        delay(12_000)
        if (!reproduciendo) fallo = true
    }
    LaunchedEffect(fallo) {
        if (fallo) {
            if ((intento + 1) % urls.size == 0) delay(5_000)
            intento++
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { PlayerView(it).apply { useController = false } },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )
        if (!reproduciendo) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                CircularProgressIndicator(color = FestivalGold)
                Spacer(Modifier.height(10.dp))
                Text(
                    if (intento == 0) "Conectando con la transmisión…" else "Reconectando con la transmisión…",
                    color = Color.White, fontSize = 14.sp
                )
                if (intento >= urls.size) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "No llega el video. La TV debe estar en la misma red Wi-Fi que el teléfono que transmite.",
                        color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Chat del concierto que se transmite: mensajes reales del servidor cada 3 s.
 * Escribir exige sesión; sin ella se ofrece iniciarla.
 */
@Composable
private fun ChatTv(
    eventoId: String?,
    sesion: SesionTv?,
    enviarMensaje: suspend (String, String) -> String?,
    onIniciarSesion: () -> Unit,
    modifier: Modifier
) {
    val api = remember { FestivalApiService.create() }
    val scope = rememberCoroutineScope()
    var mensajes by remember(eventoId) { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var escribiendo by remember { mutableStateOf(false) }
    var pedirSesion by remember { mutableStateOf(false) }
    val lista = rememberLazyListState()

    LaunchedEffect(eventoId) {
        if (eventoId == null) return@LaunchedEffect
        while (true) {
            runCatching { api.getChatMessages(eventoId) }.onSuccess { mensajes = it }
            delay(3_000)
        }
    }
    LaunchedEffect(mensajes.size) { if (mensajes.isNotEmpty()) lista.animateScrollToItem(mensajes.lastIndex) }

    Column(modifier = modifier) {
        Text("Chat en vivo", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            when {
                eventoId == null -> "El chat se abre cuando empieza la transmisión."
                sesion == null -> "Inicia sesión para escribir."
                else -> "Escribes como ${sesion.nombre}."
            },
            color = FestivalTextSecondary, fontSize = 11.sp
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(state = lista, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            if (eventoId != null && mensajes.isEmpty()) {
                item { Text("Aún no hay mensajes. ¡Saluda a los demás!", color = FestivalTextSecondary, fontSize = 12.sp) }
            }
            items(mensajes) { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (msg.esAdmin) Color(0xFF2E2415) else FestivalCardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            if (msg.esAdmin) "${msg.usuarioNombre} · Festival" else msg.usuarioNombre,
                            color = FestivalGold, fontWeight = FontWeight.Bold, fontSize = 11.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(msg.mensaje, color = Color.White, fontSize = 12.sp, lineHeight = 15.sp)
                    }
                }
            }
        }

        error?.let {
            Text(it, color = FestivalBadgeLive, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(10.dp))

        var enfocado by remember { mutableStateOf(false) }
        Surface(
            onClick = { if (sesion == null) pedirSesion = true else escribiendo = true },
            enabled = eventoId != null,
            color = FestivalCardBg,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { enfocado = it.isFocused }
                .border(if (enfocado) 2.dp else 1.dp, FestivalGold.copy(alpha = if (enfocado) 1f else 0.5f), RoundedCornerShape(8.dp))
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        eventoId == null -> "Sin transmisión"
                        sesion == null -> "Pulsa OK para iniciar sesión y escribir"
                        else -> "Pulsa OK para escribir un mensaje…"
                    },
                    color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.Send, contentDescription = "Escribir", tint = FestivalGold, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (escribiendo && sesion != null && eventoId != null) {
        TvChatInputDialog(
            onDismiss = { escribiendo = false },
            onSend = { texto ->
                val limpio = texto.trim()
                if (limpio.isNotEmpty()) scope.launch {
                    error = enviarMensaje(eventoId, limpio)
                    if (error == null) {
                        mensajes = mensajes + ChatMessageDto(eventoId = eventoId, usuarioNombre = sesion.nombre, mensaje = limpio)
                    }
                }
            }
        )
    }
    if (pedirSesion) {
        AlertDialog(
            onDismissRequest = { pedirSesion = false },
            containerColor = FestivalCardBg,
            title = { Text("Inicia sesión para escribir", color = FestivalGold, fontWeight = FontWeight.Bold) },
            text = { Text("El chat necesita tu cuenta del festival para saber quién escribe.", color = Color.White) },
            confirmButton = {
                Button(
                    onClick = { pedirSesion = false; onIniciarSesion() },
                    colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)
                ) { Text("Iniciar sesión", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pedirSesion = false }) { Text("Ahora no", color = Color.Gray) } }
        )
    }
}

/**
 * Diálogo para escribir con el teclado en pantalla de la TV.
 */
@Composable
fun TvChatInputDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A2118),
        title = { Text("Escribe tu mensaje", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(300) },
                placeholder = { Text("Tu mensaje aquí...", color = Color.Gray, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
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
                onClick = { onSend(text); onDismiss() },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)
            ) { Text("Enviar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } }
    )
}
```

---

## Paso 16: Biografía y música (`TvBiografiaScreen.kt` y `TvMusica.kt`)

Vida y obra, cita célebre (su botón reproduce «El Rey»), momentos clave, discografía y el catálogo completo de canciones. `TvMusica.kt` tiene la barra del reproductor que aparece abajo en cualquier sección y componentes comunes (`TvAviso`, botones con borde dorado).

**`src/main/java/mx/utng/festivaltrack/tv/presentation/screens/TvBiografiaScreen.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import mx.utng.festivaltrack.shared.data.remote.BiografiaDto
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.tv.R
import mx.utng.festivaltrack.tv.presentation.components.TvAviso
import mx.utng.festivaltrack.tv.presentation.components.TvFilaCancion
import mx.utng.festivaltrack.tv.presentation.viewmodel.Cargable
import mx.utng.festivaltrack.tv.presentation.viewmodel.EstadoMusica
import mx.utng.festivaltrack.tv.ui.theme.*

/** Nombre del origen de la cola cuando suena el catálogo del artista (igual que en el móvil). */
const val ORIGEN_DISCOGRAFIA = "Discografía de José Alfredo"

/** Lee una lista guardada como JSON (`["El Rey","Caminos de Guanajuato"]`); vacía si no se puede. */
private fun listaJson(json: String?): List<String> =
    runCatching { Gson().fromJson(json, Array<String>::class.java)?.toList() }.getOrNull().orEmpty().filter { it.isNotBlank() }

/**
 * Biografía y música de José Alfredo, como en el móvil:
 * - Vida y obra, cita célebre e hitos que vienen del servidor.
 * - El botón de la cita reproduce «El Rey»; la discografía reproduce la canción del catálogo
 *   que corresponde a cada título.
 * - El catálogo completo de canciones para escuchar (demos de 30 s).
 */
@Composable
fun TvBiografiaScreen(
    biografia: Cargable<BiografiaDto?>,
    canciones: Cargable<List<CancionDto>>,
    musica: EstadoMusica,
    onReproducir: (List<CancionDto>, Int) -> Unit,
    onReintentar: () -> Unit
) {
    val bio = (biografia as? Cargable.Listo)?.datos
    val catalogo = (canciones as? Cargable.Listo)?.datos.orEmpty()
    val sonandoCatalogo = musica.origen == ORIGEN_DISCOGRAFIA && musica.sonando

    fun reproducirTitulo(titulo: String?) {
        if (catalogo.isEmpty()) return
        val i = titulo?.let { t -> catalogo.indexOfFirst { it.titulo.contains(t, ignoreCase = true) || t.contains(it.titulo, ignoreCase = true) } }
            ?.takeIf { it >= 0 } ?: 0
        onReproducir(catalogo, i)
    }

    if (biografia is Cargable.SinConexion && canciones is Cargable.SinConexion) {
        TvAviso(Icons.Default.CloudOff, "Sin conexión con el servidor", "No se pudo cargar la biografía.", "Intentar de nuevo", onReintentar)
        return
    }

    Row(
        modifier = Modifier.fillMaxSize().background(FestivalDarkBg).padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Vida y obra
        LazyColumn(modifier = Modifier.weight(0.55f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.jose_alfredo_portrait),
                        contentDescription = "José Alfredo Jiménez",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("PATRIMONIO MUSICAL DE MÉXICO", color = FestivalGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(bio?.artista?.nombre ?: "José Alfredo Jiménez", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Dolores Hidalgo, Gto. · 1926–1973", color = FestivalTextSecondary, fontSize = 12.sp)
                    }
                }
            }
            if (biografia is Cargable.Cargando) item { LinearProgressIndicator(color = FestivalGold, modifier = Modifier.fillMaxWidth()) }

            item {
                // Cita célebre: igual que en el móvil, al pulsarla suena «El Rey».
                val sonandoRey = sonandoCatalogo && musica.actual?.titulo?.contains("El Rey", ignoreCase = true) == true
                var enfocada by remember { mutableStateOf(false) }
                Surface(
                    onClick = { reproducirTitulo("El Rey") },
                    enabled = catalogo.isNotEmpty(),
                    color = Color(0xFF2C2214),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { enfocada = it.isFocused }
                        .border(if (enfocada) 2.dp else 0.dp, if (enfocada) FestivalGold else Color.Transparent, RoundedCornerShape(14.dp))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (sonandoRey) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = null, tint = FestivalGold, modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("CITA CÉLEBRE DEL MAESTRO", color = FestivalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                bio?.citaCelebre ?: "\"No tengo trono ni reina, ni nadie que me comprenda, pero sigo siendo el Rey.\"",
                                color = Color.White, fontSize = 15.sp, fontStyle = FontStyle.Italic
                            )
                            Text(if (sonandoRey) "Sonando: «El Rey»" else "Pulsa OK para escuchar «El Rey»", color = FestivalTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            bio?.descripcion?.takeIf { it.isNotBlank() }?.let { texto ->
                item {
                    Text("Vida y obra", color = FestivalGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(texto, color = Color.White, fontSize = 14.sp, lineHeight = 21.sp)
                }
            }

            val hitos = listaJson(bio?.hitos)
            if (hitos.isNotEmpty()) {
                item { Text("Momentos clave", color = FestivalGold, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                itemsIndexed(hitos) { _, hito ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(hito, color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            val discos = listaJson(bio?.discografia)
            if (discos.isNotEmpty()) {
                item { Text("Discografía", color = FestivalGold, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        discos.forEach { disco ->
                            val sonando = sonandoCatalogo && musica.actual?.titulo?.contains(disco, ignoreCase = true) == true
                            var enfocado by remember { mutableStateOf(false) }
                            Surface(
                                onClick = { reproducirTitulo(disco) },
                                enabled = catalogo.isNotEmpty(),
                                color = if (enfocado) Color(0xFF2E3D30) else FestivalCardBg,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .onFocusChanged { enfocado = it.isFocused }
                                    .border(if (enfocado) 2.dp else 0.dp, if (enfocado) FestivalGold else Color.Transparent, RoundedCornerShape(10.dp))
                            ) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (sonando) Icons.Default.GraphicEq else Icons.Default.Album, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(disco, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Canciones del catálogo
        Column(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
            Text("Escucha sus canciones", color = FestivalGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Fragmentos de 30 s · pulsa OK para escuchar", color = FestivalTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            when (canciones) {
                Cargable.Cargando -> CircularProgressIndicator(color = FestivalGold)
                Cargable.SinConexion -> Text("No se pudieron cargar las canciones.", color = FestivalTextSecondary)
                is Cargable.Listo -> if (catalogo.isEmpty()) {
                    Text("Todavía no hay canciones en el catálogo.", color = FestivalTextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(catalogo, key = { _, c -> c.id }) { i, cancion ->
                            TvFilaCancion(
                                numero = i + 1,
                                cancion = cancion,
                                sonando = sonandoCatalogo && musica.actual?.id == cancion.id,
                                onClick = { onReproducir(catalogo, i) }
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**`src/main/java/mx/utng/festivaltrack/tv/presentation/components/TvMusica.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.tv.presentation.viewmodel.EstadoMusica
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * Barra del reproductor: aparece abajo en cualquier sección mientras hay música en la cola.
 * Todos los botones se manejan con el control (anterior, reproducir/pausar, siguiente, cerrar).
 */
@Composable
fun TvBarraMusica(
    estado: EstadoMusica,
    onAnterior: () -> Unit,
    onAlternar: () -> Unit,
    onSiguiente: () -> Unit,
    onCerrar: () -> Unit
) {
    val actual = estado.actual ?: return
    Surface(color = Color(0xFF1B2A1E), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(actual.titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        estado.error ?: "${actual.artista} · ${estado.origen} · ${estado.indice + 1} de ${estado.cola.size}",
                        color = if (estado.error != null) FestivalBadgeLive else FestivalTextSecondary,
                        fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                BotonMusica(Icons.Default.SkipPrevious, "Anterior", onAnterior)
                BotonMusica(if (estado.sonando) Icons.Default.Pause else Icons.Default.PlayArrow, if (estado.sonando) "Pausar" else "Reproducir", onAlternar, destacado = true)
                BotonMusica(Icons.Default.SkipNext, "Siguiente", onSiguiente)
                BotonMusica(Icons.Default.Close, "Cerrar reproductor", onCerrar)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (estado.posicionMs.toFloat() / estado.duracionMs.coerceAtLeast(1)).coerceIn(0f, 1f) },
                color = FestivalGold,
                trackColor = Color(0xFF2E3D30),
                modifier = Modifier.fillMaxWidth().height(3.dp)
            )
        }
    }
}

@Composable
private fun BotonMusica(icono: ImageVector, descripcion: String, onClick: () -> Unit, destacado: Boolean = false) {
    var enfocado by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = when {
            destacado -> FestivalGold
            enfocado -> Color.White.copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(44.dp)
            .onFocusChanged { enfocado = it.isFocused }
            .border(if (enfocado) 2.dp else 0.dp, if (enfocado) Color.White else Color.Transparent, RoundedCornerShape(50))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = descripcion, tint = if (destacado) Color.Black else Color.White)
        }
    }
}

/**
 * Fila de una canción: número, título, artista y duración. Pulsar OK la reproduce;
 * si es la que suena, muestra el ícono de pausa.
 */
@Composable
fun TvFilaCancion(numero: Int, cancion: CancionDto, sonando: Boolean, onClick: () -> Unit) {
    var enfocada by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        color = when {
            enfocada -> Color(0xFF2E3D30)
            sonando -> Color(0xFF2C2214)
            else -> FestivalCardBg
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { enfocada = it.isFocused }
            .border(if (enfocada) 2.dp else 0.dp, if (enfocada) FestivalGold else Color.Transparent, RoundedCornerShape(10.dp))
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                if (sonando) Icon(Icons.Default.GraphicEq, contentDescription = "Sonando", tint = FestivalGold, modifier = Modifier.size(20.dp))
                else Text("$numero", color = FestivalTextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(cancion.titulo, color = if (sonando) FestivalGold else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(cancion.artista, color = FestivalTextSecondary, fontSize = 12.sp, maxLines = 1)
            }
            Text(
                "%d:%02d".format(cancion.duracion / 60, cancion.duracion % 60),
                color = FestivalTextSecondary, fontSize = 12.sp
            )
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(22.dp))
        }
    }
}

/** Aviso a pantalla completa para "cargando", "sin conexión" o "vacío". */
@Composable
fun TvAviso(
    icono: ImageVector,
    titulo: String,
    texto: String,
    boton: String? = null,
    onBoton: () -> Unit = {},
    cargando: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (cargando) CircularProgressIndicator(color = FestivalGold)
        else Icon(icono, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(titulo, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (texto.isNotEmpty()) Text(texto, color = FestivalTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (boton != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBoton, colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)) {
                Text(boton, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Botón de acción de la TV con borde dorado al enfocarlo. */
@Composable
fun TvBoton(texto: String, icono: ImageVector?, onClick: () -> Unit, principal: Boolean = true, modifier: Modifier = Modifier) {
    if (principal) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
        ) {
            if (icono != null) { Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
            Text(texto, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(8.dp), modifier = modifier) {
            if (icono != null) { Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
            Text(texto, color = Color.White)
        }
    }
}
```

---

## Paso 17: Mapa del festival `TvMapaScreen.kt`

1. `construirSedes` arma una sede por escenario de los próximos conciertos y agrega los sitios del catálogo (`ar/sitios`) que no estén a menos de 50 m de una sede ya listada. Las coordenadas **vienen del servidor**, que usa las ubicaciones oficiales de OpenStreetMap de `festivaltrack-backend/prisma/ubicaciones.ts`.
2. La sede elegida se recuerda **por nombre** (`elegirSede`): la cartelera se sincroniza cada 5 s y, si se guardara la sede completa, la selección regresaría a la primera en cada sincronización.
3. El mapa (OSMDroid) no toma el foco: se maneja desde la lista con el control. Solo se mueve cuando se elige otra sede.
4. Como la TV no se mueve, en lugar de GPS hay un QR con la ruta a pie en Google Maps para el celular.

> Si una base de datos se sembró antes del 26/11/2026, corre `npm run seed:ubicaciones` en `festivaltrack-backend` para corregir sus coordenadas (el Mausoleo estaba ~850 m fuera de lugar).

**`src/main/java/mx/utng/festivaltrack/tv/presentation/screens/TvMapaScreen.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.SitioArDto
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.presentation.components.TvAviso
import mx.utng.festivaltrack.tv.ui.theme.*
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Una sede del festival en el mapa, con su próximo concierto si tiene. */
data class SedeTv(val nombre: String, val detalle: String, val lat: Double, val lon: Double, val proximo: EventoEntity?)

private fun distanciaKm(a: Double, b: Double, c: Double, d: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(c - a)
    val dLon = Math.toRadians(d - b)
    val h = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(a)) * cos(Math.toRadians(c)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * atan2(sqrt(h), sqrt(1 - h))
}

/**
 * Sedes con sus coordenadas reales: un lugar por escenario de los próximos conciertos (con el
 * más cercano en el tiempo) y los sitios del catálogo que no coinciden con uno ya listado
 * (a menos de 50 m se toma como el mismo lugar). Mismas reglas que el móvil.
 */
fun construirSedes(proximos: List<EventoEntity>, sitios: List<SitioArDto>): List<SedeTv> {
    val sedes = mutableListOf<SedeTv>()
    proximos.filter { it.latitud != null && it.longitud != null }
        .groupBy { it.escenario ?: it.ubicacion }
        .forEach { (lugar, eventos) ->
            val e = eventos.first()
            sedes += SedeTv(lugar, e.ubicacion, e.latitud!!, e.longitud!!, e)
        }
    sitios.forEach { s ->
        val lat = s.latitud ?: return@forEach
        val lon = s.longitud ?: return@forEach
        if (sedes.none { distanciaKm(it.lat, it.lon, lat, lon) < 0.05 }) sedes += SedeTv(s.nombre, s.ubicacion, lat, lon, null)
    }
    return sedes
}

/** La sede con ese nombre; si ya no está en la lista (o no se ha elegido ninguna), la primera. */
fun elegirSede(sedes: List<SedeTv>, nombre: String?): SedeTv? =
    sedes.firstOrNull { it.nombre == nombre } ?: sedes.firstOrNull()

/** Ruta a pie en el mapa del celular (Google Maps abre la app de mapas del teléfono). */
fun urlRuta(sede: SedeTv) = "https://www.google.com/maps/dir/?api=1&destination=${sede.lat},${sede.lon}&travelmode=walking"

/**
 * Mapa del festival en la TV: las sedes reales con su próximo concierto. La TV no se mueve, así
 * que en lugar de la ruta con GPS se muestra un QR que abre la ruta a pie en el celular.
 */
@Composable
fun TvMapaScreen(proximos: List<EventoEntity>, sitios: List<SitioArDto>, ahora: Instant) {
    val sedes = remember(proximos, sitios) { construirSedes(proximos, sitios) }
    // Se recuerda el nombre y no la sede: la cartelera se sincroniza cada 5 s y cada vez llegan
    // eventos nuevos (con otro updatedAt), lo que antes regresaba la selección a la primera sede.
    var elegidoNombre by rememberSaveable { mutableStateOf<String?>(null) }
    val elegida = elegirSede(sedes, elegidoNombre)

    if (sedes.isEmpty()) {
        TvAviso(Icons.Default.Place, "Cargando las sedes del festival…", "Si no aparecen, revisa la conexión con el servidor.", cargando = true)
        return
    }

    Row(Modifier.fillMaxSize().background(FestivalDarkBg).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        // Lista de sedes (se maneja con el control)
        Column(Modifier.width(250.dp)) {
            Text("Mapa del festival", color = FestivalGold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Elige una sede con el control", color = FestivalTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(sedes, key = { it.nombre }) { sede ->
                    var enfocada by remember { mutableStateOf(false) }
                    val esElegida = sede.nombre == elegida?.nombre
                    Surface(
                        onClick = { elegidoNombre = sede.nombre },
                        color = when { esElegida -> Color(0xFF2C2214); enfocada -> Color(0xFF2E3D30); else -> FestivalCardBg },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { enfocada = it.isFocused; if (it.isFocused) elegidoNombre = sede.nombre }
                            .border(if (enfocada || esElegida) 2.dp else 0.dp, if (enfocada || esElegida) FestivalGold else Color.Transparent, RoundedCornerShape(10.dp))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(sede.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                sede.proximo?.let { "${it.nombre} · ${HorarioFestival.cuando(it, ahora)}" } ?: sede.detalle,
                                color = FestivalTextSecondary, fontSize = 11.sp, maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // Mapa grande y, debajo, la sede elegida con el QR para la ruta a pie
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
                MapaSedes(sedes = sedes, elegida = elegida)
            }
            elegida?.let { sede ->
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(sede.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(sede.detalle, color = FestivalTextSecondary, fontSize = 11.sp)
                        sede.proximo?.let {
                            Text("Próximo: ${it.nombre} · ${HorarioFestival.cuando(it, ahora)}", color = FestivalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Escanea el código para ver la ruta a pie en tu celular", color = FestivalTextSecondary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    DynamicQrCode(content = urlRuta(sede), modifier = Modifier.size(120.dp))
                }
            }
        }
    }
}

@Composable
private fun MapaSedes(sedes: List<SedeTv>, elegida: SedeTv?) {
    // Sede a la que ya se movió el mapa: solo se anima cuando se elige otra, no en cada sincronización.
    val centradaEn = remember { arrayOfNulls<String>(1) }
    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(false)
                // El mapa no toma el foco: se maneja desde la lista de sedes con el control.
                isFocusable = false
                controller.setZoom(16.0)
            }
        },
        update = { mapa ->
            mapa.overlays.clear()
            sedes.forEach { sede ->
                mapa.overlays.add(Marker(mapa).apply {
                    position = GeoPoint(sede.lat, sede.lon)
                    title = sede.nombre
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    if (sede.nombre == elegida?.nombre) showInfoWindow()
                })
            }
            if (elegida != null && elegida.nombre != centradaEn[0]) {
                mapa.controller.animateTo(GeoPoint(elegida.lat, elegida.lon))
                centradaEn[0] = elegida.nombre
            }
            mapa.invalidate()
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

---

## Paso 18: Galería `TvGalleryScreen.kt`

Fotos reales del servidor (las que administra el panel), filtrables por galería. OK sobre una la abre en grande.

**`src/main/java/mx/utng/festivaltrack/tv/presentation/screens/TvGalleryScreen.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.tv.presentation.viewmodel.EstadoGaleria
import mx.utng.festivaltrack.tv.ui.theme.*

/** Foto de la galería lista para mostrarse. */
private data class FotoTv(val id: String, val url: String, val titulo: String, val galeria: String)

/** Convierte una ruta del servidor (`/uploads/x.jpg`) en URL completa; deja intactas las URL completas. */
private fun urlMedia(ruta: String): String = if (ruta.startsWith("/")) FestivalApiService.SERVER_URL + ruta else ruta

/**
 * Galería con las fotos reales del servidor (las que administra el panel), filtrables por
 * galería. Pulsar OK sobre una la abre en grande.
 */
@Composable
fun TvGalleryScreen(estado: EstadoGaleria, onReintentar: () -> Unit) {
    var filtro by remember { mutableStateOf<String?>(null) }
    var abierta by remember { mutableStateOf<FotoTv?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
            .padding(32.dp)
    ) {
        when (estado) {
            EstadoGaleria.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FestivalGold)
            }
            EstadoGaleria.SinConexion -> AvisoGaleria(
                icono = Icons.Default.CloudOff,
                titulo = "Sin conexión con el servidor",
                texto = "No se pudieron cargar las fotos.",
                boton = "Intentar de nuevo",
                onBoton = onReintentar
            )
            is EstadoGaleria.Lista -> {
                val fotos = estado.galerias.flatMap { g ->
                    g.imagenes.sortedBy { it.orden }.map { FotoTv(it.id, urlMedia(it.url), it.titulo ?: g.nombre, g.nombre) }
                }
                val galerias = estado.galerias.filter { it.imagenes.isNotEmpty() }.map { it.nombre }.distinct()
                val visibles = if (filtro == null) fotos else fotos.filter { it.galeria == filtro }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Galería del festival", color = FestivalGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("${fotos.size} fotos", color = FestivalTextSecondary, fontSize = 12.sp)
                    }
                    if (galerias.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (listOf<String?>(null) + galerias).forEach { nombre ->
                                FilterChip(
                                    selected = filtro == nombre,
                                    onClick = { filtro = nombre },
                                    label = { Text(nombre ?: "Todo") },
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
                }
                Spacer(Modifier.height(20.dp))

                if (fotos.isEmpty()) {
                    AvisoGaleria(
                        icono = Icons.Default.PhotoLibrary,
                        titulo = "Todavía no hay fotos",
                        texto = "Cuando el equipo del festival suba fotos aparecerán aquí."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(visibles, key = { it.id }) { foto -> FotoCard(foto) { abierta = foto } }
                    }
                }
            }
        }
    }

    abierta?.let { foto ->
        Dialog(onDismissRequest = { abierta = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                onClick = { abierta = null },
                color = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.85f)
            ) {
                Box {
                    AsyncImage(
                        model = foto.url,
                        contentDescription = foto.titulo,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                            .padding(20.dp)
                    ) {
                        Text(foto.titulo, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("${foto.galeria} · Pulsa OK o Atrás para cerrar", color = FestivalGold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FotoCard(foto: FotoTv, onClick: () -> Unit) {
    var enfocada by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        color = FestivalCardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .onFocusChanged { enfocada = it.isFocused }
            .border(if (enfocada) 3.dp else 0.dp, if (enfocada) FestivalGold else Color.Transparent, RoundedCornerShape(12.dp))
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = foto.url,
                contentDescription = foto.titulo,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(foto.titulo, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(foto.galeria, color = FestivalGold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AvisoGaleria(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    texto: String,
    boton: String? = null,
    onBoton: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icono, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(titulo, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(texto, color = FestivalTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (boton != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBoton, colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)) {
                Text(boton, fontWeight = FontWeight.Bold)
            }
        }
    }
}
```

---

## Paso 19: Comunidad `TvComunidadScreen.kt`

Experiencias, recomendaciones y fotos de los asistentes, las mismas del móvil, filtrables por tipo. En la TV **solo se leen**, por decisión del equipo: escribir con el control es incómodo, así que publicar, dar like o comentar se hace desde el celular.

**`src/main/java/mx/utng/festivaltrack/tv/presentation/screens/TvComunidadScreen.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.PublicacionDto
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.presentation.components.TvAviso
import mx.utng.festivaltrack.tv.presentation.viewmodel.Cargable
import mx.utng.festivaltrack.tv.ui.theme.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIPOS = listOf(null to "Todo", "EXPERIENCIA" to "Experiencias", "RECOMENDACION" to "Recomendaciones", "FOTO" to "Fotos")
private val TIPO_SINGULAR = mapOf("EXPERIENCIA" to "Experiencia", "RECOMENDACION" to "Recomendación", "FOTO" to "Foto")
private val FECHA = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("es", "MX")).withZone(HorarioFestival.ZONA)

private fun urlMedia(ruta: String): String = if (ruta.startsWith("/")) FestivalApiService.SERVER_URL + ruta else ruta

/**
 * Comunidad del festival (la "Galería" de publicaciones del móvil): experiencias,
 * recomendaciones y fotos de los asistentes. En la TV se leen; para publicar o comentar se usa
 * la app del celular.
 */
@Composable
fun TvComunidadScreen(comunidad: Cargable<List<PublicacionDto>>, onRecargar: () -> Unit) {
    var tipo by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(FestivalDarkBg).padding(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Comunidad del festival", color = FestivalGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Fotos, recomendaciones y anécdotas de los asistentes · publica desde la app en tu celular", color = FestivalTextSecondary, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onRecargar, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Actualizar", color = Color.White)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TIPOS.forEach { (valor, nombre) ->
                FilterChip(
                    selected = tipo == valor,
                    onClick = { tipo = valor },
                    label = { Text(nombre) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FestivalGold, selectedLabelColor = Color.Black,
                        containerColor = FestivalCardBg, labelColor = Color.White
                    )
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        when (comunidad) {
            Cargable.Cargando -> TvAviso(Icons.Default.Forum, "Cargando publicaciones…", "", cargando = true)
            Cargable.SinConexion -> TvAviso(Icons.Default.CloudOff, "Sin conexión con el servidor", "No se pudieron cargar las publicaciones.", "Intentar de nuevo", onRecargar)
            is Cargable.Listo -> {
                val visibles = comunidad.datos.filter { tipo == null || it.tipo.equals(tipo, ignoreCase = true) }
                if (visibles.isEmpty()) {
                    TvAviso(Icons.Default.Forum, "Todavía no hay publicaciones", "Sé el primero en compartir tu experiencia desde la app del celular.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(visibles, key = { it.id }) { TarjetaPublicacion(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaPublicacion(p: PublicacionDto) {
    var enfocada by remember { mutableStateOf(false) }
    Surface(
        onClick = {},
        color = if (enfocada) Color(0xFF2E3D30) else FestivalCardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { enfocada = it.isFocused }
            .border(if (enfocada) 2.dp else 0.dp, if (enfocada) FestivalGold else Color.Transparent, RoundedCornerShape(12.dp))
    ) {
        Row(Modifier.padding(14.dp)) {
            p.imagenUrl?.takeIf { it.isNotBlank() }?.let { url ->
                AsyncImage(
                    model = urlMedia(url), contentDescription = p.titulo ?: "Foto", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(110.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = FestivalSidebarBg, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            TIPO_SINGULAR[p.tipo.uppercase()] ?: p.tipo,
                            color = FestivalGold, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(p.autorNombre, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                p.titulo?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(p.contenido, color = Color.LightGray, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = FestivalBadgeLive, modifier = Modifier.size(14.dp))
                    Text(" ${p.likes}", color = FestivalTextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.ChatBubble, contentDescription = null, tint = FestivalTextSecondary, modifier = Modifier.size(14.dp))
                    Text(" ${p.comentarios}", color = FestivalTextSecondary, fontSize = 12.sp)
                    p.lugar?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Default.Place, contentDescription = null, tint = FestivalTextSecondary, modifier = Modifier.size(14.dp))
                        Text(" $it", color = FestivalTextSecondary, fontSize = 12.sp, maxLines = 1)
                    }
                    Spacer(Modifier.weight(1f))
                    runCatching { FECHA.format(Instant.parse(p.createdAt)) }.getOrNull()?.let {
                        Text(it, color = FestivalTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
```

---

## Paso 20: Mis boletos `TvBoletosScreen.kt`

Con sesión iniciada lista los boletos de la cuenta; al elegir uno muestra su QR real, el mismo que valida el escáner de la entrada.

**`src/main/java/mx/utng/festivaltrack/tv/presentation/screens/TvBoletosScreen.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.data.remote.BoletoDto
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.presentation.components.TvAviso
import mx.utng.festivaltrack.tv.presentation.viewmodel.Cargable
import mx.utng.festivaltrack.tv.presentation.viewmodel.SesionTv
import mx.utng.festivaltrack.tv.ui.theme.*
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode
import java.time.Instant

/**
 * Mis boletos (el Perfil del móvil): con sesión iniciada lista los boletos de la cuenta y, al
 * elegir uno, muestra su QR real, el mismo que valida el escáner de la entrada.
 */
@Composable
fun TvBoletosScreen(
    sesion: SesionTv?,
    boletos: Cargable<List<BoletoDto>>,
    ahora: Instant,
    onCargar: () -> Unit,
    onIniciarSesion: () -> Unit,
    onComprar: () -> Unit
) {
    LaunchedEffect(sesion) { if (sesion != null) onCargar() }

    Column(Modifier.fillMaxSize().background(FestivalDarkBg).padding(28.dp)) {
        Text("Mis boletos", color = FestivalGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            sesion?.let { "Hola, ${it.nombre}. Muestra el QR en la entrada desde tu celular o desde aquí." } ?: "Inicia sesión para ver tus boletos.",
            color = FestivalTextSecondary, fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))

        if (sesion == null) {
            TvAviso(Icons.Default.ConfirmationNumber, "Tus boletos aparecen aquí", "Entra con la misma cuenta con la que compraste.", "Iniciar sesión", onIniciarSesion)
            return@Column
        }
        when (boletos) {
            Cargable.Cargando -> TvAviso(Icons.Default.ConfirmationNumber, "Cargando tus boletos…", "", cargando = true)
            Cargable.SinConexion -> TvAviso(Icons.Default.CloudOff, "Sin conexión con el servidor", "No se pudieron cargar tus boletos.", "Intentar de nuevo", onCargar)
            is Cargable.Listo -> {
                val lista = boletos.datos.sortedBy { it.evento?.fechaHora ?: "" }
                if (lista.isEmpty()) {
                    TvAviso(Icons.Default.ConfirmationNumber, "Todavía no tienes boletos", "Compra desde tu celular escaneando el QR.", "Comprar boletos", onComprar)
                    return@Column
                }
                var elegidoId by remember(lista) { mutableStateOf(lista.first().id) }
                val elegido = lista.firstOrNull { it.id == elegidoId } ?: lista.first()
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxSize()) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        items(lista, key = { it.id }) { boleto ->
                            FilaBoleto(boleto, ahora, elegido = boleto.id == elegido.id) { elegidoId = boleto.id }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(300.dp)) {
                        DynamicQrCode(content = elegido.codigoQR, modifier = Modifier.size(260.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(elegido.evento?.nombre ?: "Festival", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
                        Text("Boleto ${elegido.categoria} · ${estadoLegible(elegido.estado)}", color = FestivalGold, fontSize = 13.sp)
                        Text("ID: ${elegido.id}", color = FestivalTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun estadoLegible(estado: String) = when (estado.uppercase()) {
    "ACTIVO" -> "Activo"
    "USADO" -> "Ya usado"
    "CANCELADO" -> "Cancelado"
    else -> estado
}

@Composable
private fun FilaBoleto(boleto: BoletoDto, ahora: Instant, elegido: Boolean, onElegir: () -> Unit) {
    var enfocado by remember { mutableStateOf(false) }
    val evento = boleto.evento?.toEntity()
    Surface(
        onClick = onElegir,
        color = when {
            elegido -> Color(0xFF2C2214)
            enfocado -> Color(0xFF2E3D30)
            else -> FestivalCardBg
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { enfocado = it.isFocused; if (it.isFocused) onElegir() }
            .border(if (enfocado || elegido) 2.dp else 0.dp, if (enfocado || elegido) FestivalGold else Color.Transparent, RoundedCornerShape(12.dp))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.QrCode2, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(boleto.evento?.nombre ?: "Concierto del festival", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    listOfNotNull(evento?.let { HorarioFestival.cuando(it, ahora) }?.takeIf { it.isNotEmpty() }, evento?.escenario ?: evento?.ubicacion).joinToString(" · "),
                    color = FestivalTextSecondary, fontSize = 12.sp
                )
            }
            Surface(color = if (boleto.categoria.uppercase() == "VIP") FestivalGold else FestivalSidebarBg, shape = RoundedCornerShape(6.dp)) {
                Text(
                    boleto.categoria, color = if (boleto.categoria.uppercase() == "VIP") Color.Black else FestivalGold,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            if (boleto.estado.uppercase() != "ACTIVO") {
                Spacer(Modifier.width(8.dp))
                Text(estadoLegible(boleto.estado), color = FestivalTextSecondary, fontSize = 11.sp)
            }
        }
    }
}
```

---

## Paso 21: Ajustes `TvSettingsScreen.kt`

Servidor al que se conecta y si responde, estado de la transmisión, página donde se compran boletos y la cuenta (iniciar o cerrar sesión).

**`src/main/java/mx/utng/festivaltrack/tv/presentation/screens/TvSettingsScreen.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import mx.utng.festivaltrack.shared.util.EnlacesWeb
import mx.utng.festivaltrack.tv.presentation.viewmodel.SesionTv
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * Ajustes de la TV con datos reales: a qué servidor se conecta y si responde, el estado de la
 * transmisión, la página donde se compran boletos y la cuenta (iniciar o cerrar sesión).
 */
@Composable
fun TvSettingsScreen(
    servidorConectado: Boolean?,
    stream: StreamStatusDto?,
    sesion: SesionTv?,
    onIniciarSesion: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }
    var confirmarSalida by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
            .padding(32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text("Ajustes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Versión $version", color = FestivalTextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(20.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            AjusteCard(
                icon = Icons.Default.Person,
                title = "Cuenta",
                subtitle = sesion?.let { "${it.nombre} · ${it.correo}" } ?: "Invitado: puedes ver todo; para escribir en el chat inicia sesión"
            ) {
                if (sesion != null) {
                    Button(
                        onClick = { confirmarSalida = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC51111), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cerrar sesión", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onIniciarSesion,
                        colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Iniciar sesión", fontWeight = FontWeight.Bold)
                    }
                }
            }
            AjusteCard(
                icon = Icons.Default.Dns,
                title = "Servidor del festival",
                subtitle = EnlacesWeb.servidor,
                estado = when (servidorConectado) {
                    true -> "Conectado" to Color(0xFF66BB6A)
                    false -> "Sin conexión" to FestivalBadgeLive
                    null -> "Comprobando…" to FestivalTextSecondary
                }
            )
            AjusteCard(
                icon = Icons.Default.LiveTv,
                title = "Transmisión en vivo",
                subtitle = when {
                    stream == null -> "No se sabe: el servidor no responde"
                    stream.isLive -> "En vivo: ${stream.eventoNombre ?: "concierto del festival"}"
                    else -> "Sin transmisión en este momento"
                }
            )
            AjusteCard(
                icon = Icons.Default.QrCode,
                title = "Compra de boletos con el celular",
                subtitle = "Los códigos QR abren ${EnlacesWeb.base}"
            )
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            containerColor = FestivalCardBg,
            title = { Text("¿Cerrar sesión?", color = FestivalGold, fontWeight = FontWeight.Bold) },
            text = { Text("La TV seguirá funcionando como invitado.", color = Color.White) },
            confirmButton = {
                Button(
                    onClick = { confirmarSalida = false; onCerrarSesion() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC51111), contentColor = Color.White)
                ) { Text("Cerrar sesión", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar", color = Color.Gray) } }
        )
    }
}

@Composable
private fun AjusteCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    estado: Pair<String, Color>? = null,
    accion: (@Composable () -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FestivalCardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color.Gray, fontSize = 13.sp)
            }
            estado?.let { (texto, color) ->
                Text(texto, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            accion?.invoke()
        }
    }
}
```

---

## Paso 22: Pruebas unitarias

Se corren en la JVM, sin emulador:

```bash
./gradlew :tv:testDebugUnitTest
```

| Prueba | Qué revisa |
|---|---|
| `TokenJwtTest` | Lectura del vencimiento del JWT y cuándo una sesión guardada se puede restaurar |
| `PortadaConciertoTest` | Que cada concierto de la cartelera tenga su foto y la URL completa del banner |
| `TvMapaTest` | Sedes con las coordenadas oficiales, unión de lugares a menos de 50 m, que la selección sobreviva a la sincronización y la URL de la ruta a pie |

**`src/test/java/mx/utng/festivaltrack/tv/data/TokenJwtTest.kt`**

```kotlin
package mx.utng.festivaltrack.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Base64

class TokenJwtTest {

    private fun jwt(payload: String): String {
        val b64 = { s: String -> Base64.getUrlEncoder().withoutPadding().encodeToString(s.toByteArray()) }
        return "${b64("""{"alg":"HS256","typ":"JWT"}""")}.${b64(payload)}.firma"
    }

    private val ahora = Instant.parse("2026-11-26T18:00:00Z")

    @Test
    fun `lee el vencimiento del token`() {
        val token = jwt("""{"sub":"u1","rol":"USUARIO","exp":${ahora.epochSecond + 3600}}""")
        assertEquals(ahora.plusSeconds(3600), TokenJwt.vencimiento(token))
    }

    @Test
    fun `un token que vence despues sigue vigente`() {
        assertTrue(TokenJwt.vigente(jwt("""{"exp":${ahora.epochSecond + 7 * 24 * 3600}}"""), ahora))
    }

    @Test
    fun `un token vencido o por vencer en menos de un minuto no se restaura`() {
        assertFalse(TokenJwt.vigente(jwt("""{"exp":${ahora.epochSecond - 10}}"""), ahora))
        assertFalse(TokenJwt.vigente(jwt("""{"exp":${ahora.epochSecond + 30}}"""), ahora))
    }

    @Test
    fun `sin exp se deja decidir al servidor`() {
        assertNull(TokenJwt.vencimiento(jwt("""{"sub":"u1"}""")))
        assertTrue(TokenJwt.vigente(jwt("""{"sub":"u1"}"""), ahora))
    }

    @Test
    fun `un texto que no es JWT no trae vencimiento y uno vacio no sirve`() {
        assertNull(TokenJwt.vencimiento("no-es-un-token"))
        assertNull(TokenJwt.vencimiento("a.%%%.c"))
        assertFalse(TokenJwt.vigente("", ahora))
    }
}
```

**`src/test/java/mx/utng/festivaltrack/tv/presentation/components/PortadaConciertoTest.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortadaConciertoTest {

    @Test
    fun `los conciertos de la cartelera tienen cada uno su foto`() {
        // Los cinco conciertos de seed-cartelera (los mismos que hay en Render).
        val fondos = listOf(
            fondoDe("1", "Noche de Bohemia en la Plaza", "Plaza Principal", "Jardín Principal"),
            fondoDe("2", "Serenata en el Mausoleo", "Mausoleo de José Alfredo Jiménez", "Panteón Municipal"),
            fondoDe("3", "Recorrido Casa Museo José Alfredo", "Casa Museo", "Calle Guanajuato 13"),
            fondoDe("4", "Gran Gala Mariachi", "Escenario Principal", "Jardín Principal"),
            fondoDe("5", "Homenaje: El Rey", "Escenario Principal", "Jardín Principal")
        )
        assertEquals(
            listOf(
                FondoConcierto.BOHEMIA, FondoConcierto.MAUSOLEO, FondoConcierto.CASA_MUSEO,
                FondoConcierto.MARIACHI, FondoConcierto.EL_REY
            ),
            fondos
        )
    }

    @Test
    fun `sin palabras clave se usa el lugar sin importar acentos ni mayusculas`() {
        assertEquals(FondoConcierto.MAUSOLEO, fondoDe("x", "Serenata", null, "PANTEÓN MUNICIPAL"))
        assertEquals(FondoConcierto.PLAZA, fondoDe("x", "Concierto de la tarde", null, "Jardín Principal"))
        assertEquals(FondoConcierto.MARIACHI, fondoDe("x", "Concierto", "Escenario Principal", "Centro"))
    }

    @Test
    fun `rey solo cuenta como palabra completa`() {
        assertEquals(FondoConcierto.PLAZA, fondoDe("x", "Reyes de la canción", "Plaza Principal", ""))
    }

    @Test
    fun `un concierto sin pistas siempre recibe la misma foto general`() {
        val primera = fondoDe("EVT-777", "Concierto especial", null, "Teatro")
        repeat(3) { assertEquals(primera, fondoDe("EVT-777", "Concierto especial", null, "Teatro")) }
    }

    @Test
    fun `la url del banner se completa con el servidor cuando es relativa`() {
        assertEquals(
            "https://festivaltrack-backend.onrender.com/uploads/gala.jpg",
            urlBanner("/uploads/gala.jpg", "https://festivaltrack-backend.onrender.com/")
        )
        assertEquals("https://cdn.mx/a.jpg", urlBanner(" https://cdn.mx/a.jpg ", "http://10.0.2.2:3001"))
        assertNull(urlBanner(null, "http://10.0.2.2:3001"))
        assertNull(urlBanner("  ", "http://10.0.2.2:3001"))
    }
}
```

**`src/test/java/mx/utng/festivaltrack/tv/presentation/screens/TvMapaTest.kt`**

```kotlin
package mx.utng.festivaltrack.tv.presentation.screens

import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.SitioArDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvMapaTest {

    // Coordenadas oficiales de festivaltrack-backend/prisma/ubicaciones.ts
    private fun evento(id: String, escenario: String, lat: Double, lon: Double, sync: Long) = EventoEntity(
        id = id, nombre = "Concierto $id", fechaHora = "2026-11-28T03:00:00.000Z", ubicacion = "Dolores Hidalgo",
        escenario = escenario, bannerUrl = null, estado = "PUBLICADO", artistaId = null, artistaNombre = null,
        latitud = lat, longitud = lon, updatedAt = sync
    )

    private fun cartelera(sync: Long) = listOf(
        evento("1", "Mausoleo de José Alfredo Jiménez", 21.156687, -100.948773, sync),
        evento("2", "Casa Museo", 21.157341, -100.93277, sync),
        evento("3", "Escenario Principal", 21.1583, -100.9341, sync)
    )

    private val sitios = listOf(
        SitioArDto("SITIO-MAUSOLEO", "Mausoleo de José Alfredo Jiménez", "Panteón Municipal", "", latitud = 21.156687, longitud = -100.948773),
        SitioArDto("SITIO-PARROQUIA", "Parroquia de Dolores", "Jardín Principal", "", latitud = 21.158601, longitud = -100.933947),
        SitioArDto("SITIO-HIDALGO", "Monumento a Miguel Hidalgo", "Jardín Principal", "", latitud = 21.157511, longitud = -100.934529)
    )

    @Test
    fun `una sede por escenario y los sitios que no estan a menos de 50 m`() {
        val sedes = construirSedes(cartelera(sync = 1), sitios)
        // La Parroquia queda a ~37 m del Escenario Principal (mismo lugar); el Monumento, a ~98 m.
        assertEquals(
            listOf("Mausoleo de José Alfredo Jiménez", "Casa Museo", "Escenario Principal", "Monumento a Miguel Hidalgo"),
            sedes.map { it.nombre }
        )
        assertEquals(21.156687, sedes[0].lat, 0.0)
        assertEquals(-100.948773, sedes[0].lon, 0.0)
    }

    @Test
    fun `la sede elegida se conserva cuando la cartelera se vuelve a sincronizar`() {
        val antes = construirSedes(cartelera(sync = 1), sitios)
        val despues = construirSedes(cartelera(sync = 5_001), sitios) // mismos datos, otro updatedAt
        assertEquals("Casa Museo", elegirSede(antes, "Casa Museo")?.nombre)
        assertEquals("Casa Museo", elegirSede(despues, "Casa Museo")?.nombre)
    }

    @Test
    fun `sin eleccion o si la sede ya no existe se muestra la primera`() {
        val sedes = construirSedes(cartelera(sync = 1), sitios)
        assertEquals("Mausoleo de José Alfredo Jiménez", elegirSede(sedes, null)?.nombre)
        assertEquals("Mausoleo de José Alfredo Jiménez", elegirSede(sedes, "Sede que ya pasó")?.nombre)
        assertNull(elegirSede(emptyList(), "Casa Museo"))
    }

    @Test
    fun `la ruta a pie usa las coordenadas de la sede`() {
        val sede = construirSedes(cartelera(sync = 1), sitios).first()
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=21.156687,-100.948773&travelmode=walking",
            urlRuta(sede)
        )
    }
}
```

---

## Paso 23: Compilar, instalar y probar

### Contra el backend local (emulador)

```bash
# 1. Backend en la PC (puerto 3001)
cd festivaltrack-backend && npm run start:dev

# 2. Compila e instala la app de TV en el emulador
./gradlew :tv:installDebug
```

El emulador llega a la PC en `10.0.2.2:3001`. Para una **TV física** en la misma Wi-Fi, pon la IP de la PC en `local.properties`:

```properties
festivaltrack.deviceHost=192.168.X.X
```

### Contra el backend de Render

```bash
./gradlew :tv:assembleDebug -PfestivaltrackApiUrl=https://festivaltrack-backend.onrender.com/api/v1
```

Así la TV usa los datos de producción y los QR de compra y registro abren `https://festivaltrack-web.onrender.com` sin configurar nada más. El plan gratuito de Render se duerme: la primera petición puede tardar hasta un minuto.

### Transmisión entre emuladores

El teléfono del administrador transmite en el puerto 1935 de **su** emulador. Para que la TV lo reciba en `rtsp://10.0.2.2:1935/...`, redirige ese puerto a la PC:

```bash
adb -s <emulador-del-admin> forward tcp:1935 tcp:1935
```

Con dispositivos reales en la misma Wi-Fi no hace falta: la TV usa la IP real del teléfono que publica el backend.

### Recorrido de prueba

1. Abrir la app: pide login; «Continuar sin cuenta» entra como invitado.
2. Iniciar sesión, cerrar la app a la fuerza y volver a abrirla: entra directo con la cuenta.
3. Inicio: cada concierto con su imagen; OK abre el detalle con su repertorio.
4. «Comprar boletos»: escanear el QR con el celular abre la compra del concierto elegido.
5. Mapa: recorrer las sedes con el control; la elegida se mantiene y su QR abre la ruta a pie.
6. Mis boletos: el QR de cada boleto se puede escanear.
7. Ajustes → «Cerrar sesión»: al reabrir vuelve a pedir el login.

---

## Solución de Problemas

- **El video RTSP no carga en el emulador:** ejecuta `adb -s <emulador-del-admin> forward tcp:1935 tcp:1935` mientras la app móvil está transmitiendo.
- **«Sin conexión con el servidor»:** revisa que el backend esté corriendo y, en una TV física, que `festivaltrack.deviceHost` tenga la IP de la PC. En Ajustes se ve a qué servidor intenta conectarse.
- **El QR de compra abre `http://...:5173` y no carga:** compilaste sin `festivaltrackApiUrl` de Render y sin `festivaltrackWebUrl`; levanta la web con `npm run dev -- --host` o configura la URL de la web.
- **La interfaz no responde a las flechas del teclado:** haz clic dentro de la ventana del emulador para que capture el teclado como si fuera el control remoto.
- **El teclado en pantalla tapa el botón «Iniciar sesión»:** pulsa Atrás una vez para cerrarlo (no sale de la app) y luego OK sobre el botón.
- **No aparece el teclado del chat:** pulsa OK sobre el campo de mensaje para abrir el cuadro de texto.
- **La app ya no recuerda la sesión:** el token venció (7 días) o el servidor cambió su llave JWT; es normal que vuelva a pedir el login.
- **Una sede del mapa sale en otro lugar:** corre `npm run seed:ubicaciones` en `festivaltrack-backend` contra esa base.

---

## Pendientes conocidos

- **Login con QR desde el celular:** `POST /auth/tv-sync` del backend sigue siendo un stub; hoy se escribe correo y contraseña con el control.
- **Pruebas en equipos reales:** falta probar en una Android TV física y la transmisión con dos teléfonos reales en la misma Wi-Fi.

# FestivalTrack - Módulo de Aplicación Móvil (App)

Este es el módulo principal de la aplicación móvil de **FestivalTrack**, desarrollado en **Kotlin** utilizando **Jetpack Compose** y una arquitectura **MVVM (Model-View-ViewModel)**. 

Este módulo permite tanto a los asistentes como a los administradores gestionar su experiencia en el festival de música, incluyendo compra de boletos, visualización de mapas, live streams, administración de usuarios y creación de eventos.

---

## 1. Arquitectura MVVM y Capas

La aplicación está diseñada usando el patrón MVVM, asegurando una separación clara de responsabilidades:
- **Model**: Entidades y Repositorios (gestionados a través de Retrofit y Room, mayormente en el módulo `shared`).
- **View**: Pantallas construidas enteramente con **Jetpack Compose** (`ui/screens`).
- **ViewModel**: Lógica de negocio y manejo del estado expuesto como `StateFlow` (`ui/viewmodels`).

### Estructura de Directorios Actualizada
```text
app/src/main/java/mx/utng/festivaltrack/app/
├── FestivalTrackApplication.kt
├── MainActivity.kt
├── data/
│   └── TokenManager.kt
├── di/
│   └── AppContainer.kt
├── ui/
│   ├── screens/
│   │   ├── AdminCreateEventScreen.kt
│   │   ├── AdminDashboardScreen.kt
│   │   ├── AdminLiveStreamScreen.kt
│   │   ├── AdminMainScreen.kt
│   │   ├── AdminManageScreen.kt
│   │   ├── AdminScannerScreen.kt
│   │   ├── AdminUploadScreen.kt
│   │   ├── AdminUsersScreen.kt
│   │   ├── AudioScreen.kt
│   │   ├── BiographyScreen.kt
│   │   ├── CheckoutScreen.kt
│   │   ├── DashboardScreen.kt
│   │   ├── GalleryScreen.kt
│   │   ├── LoginScreen.kt
│   │   ├── MainScreen.kt
│   │   ├── MapScreen.kt
│   │   ├── ProfileScreen.kt
│   │   ├── RegisterScreen.kt
│   │   ├── TicketSuccessScreen.kt
│   │   ├── TicketsScreen.kt
│   │   ├── UserLiveStreamScreen.kt
│   │   └── WelcomeScreen.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   └── Theme.kt
│   ├── utils/
│   │   └── QrCodeGenerator.kt
│   └── viewmodels/
│       ├── AdminManageViewModel.kt
│       ├── AdminUsersViewModel.kt
│       ├── ArtistViewModel.kt
│       ├── AudioViewModel.kt
│       ├── AuthViewModel.kt
│       ├── CheckoutViewModel.kt
│       ├── EventosViewModel.kt
│       ├── GalleryViewModel.kt
│       ├── LiveViewModel.kt
│       └── ProfileViewModel.kt
```

---

## 2. Paso 1: Requisitos y Configuración de `build.gradle.kts`

El módulo utiliza múltiples librerías modernas para su funcionamiento. Aquí se muestra cómo está configurado el archivo `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "mx.utng.festivaltrack.app"
    compileSdk = 35
    // ... configuraciones por defecto
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":shared"))
    
    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.6.1")
    implementation("androidx.compose.material3:material3:1.2.0")
    
    // Navigation & ViewModel
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Media3 (ExoPlayer & RTSP para Live Stream)
    val media3_version = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3_version")
    
    // Permisos y Transmisión de video (Cámara)
    implementation("com.github.pedroSG94.RootEncoder:library:2.5.0")
    implementation("com.github.pedroSG94:RTSP-Server:1.4.1")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    
    // Retrofit (Red)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
}
```

---

## 3. Paso 2: Configuración de AndroidManifest.xml

Para que las funcionalidades de mapa, red, y streaming (cámara/micrófono) funcionen, requerimos los siguientes permisos en el `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
    <application
        android:name=".FestivalTrackApplication"
        android:usesCleartextTraffic="true"
        android:theme="@style/Theme.FestivalTrack">
        <!-- ... activities ... -->
    </application>
</manifest>
```

---

## 4. Paso 3: Estructura de Navegación (NavHost)

El flujo de pantallas se maneja en el archivo `MainActivity.kt` y `MainScreen.kt` a través de `NavHost`. Las rutas principales incluyen:

- `welcome`: Pantalla de inicio
- `login` / `register`: Autenticación
- `dashboard`: Panel principal del usuario (Tickets, Mapa, Live, Perfil)
- `admin_dashboard`: Panel principal del administrador (Gestión de eventos, Escáner, Live, Usuarios)
- `checkout/{eventoId}`: Flujo de compra
- `ticket_success`: Confirmación de pago

---

## 5. Paso 4: Implementación de Autenticación JWT

La autenticación utiliza el `AuthViewModel` para realizar llamadas a la API y guardar el token usando `TokenManager`.

```kotlin
/**
 * ViewModel de autenticación para las pantallas LoginScreen y RegisterScreen.
 *
 * Responsabilidades:
 * - Verificar si hay una sesión activa al iniciar la app.
 * - Realizar el login llamando a `POST /auth/login` y guardar el token.
 * - Proporcionar el estado actual del flujo auth a través de un [StateFlow].
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    /**
     * Inicia sesión con correo y contraseña.
     * @param correo Correo electrónico del usuario.
     * @param contrasena Contraseña del usuario.
     */
    fun login(correo: String, contrasena: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = api.login(LoginDto(correo, contrasena))
                tokenManager.saveToken(response.accessToken)
                tokenManager.saveUserRole(response.usuario.rol)
                _authState.value = AuthState.Success(response.usuario.rol)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Credenciales inválidas")
            }
        }
    }
}
```

---

## 6. Paso 5: CRUD de Eventos Admin

El `AdminManageViewModel` permite sincronizar y modificar eventos.

```kotlin
/**
 * ViewModel encargado de la gestión de eventos por parte del administrador.
 * 
 * Interactúa con el [FestivalRepository] para mantener sincronizada la fuente 
 * de datos local (Room) con la remota (API).
 */
class AdminManageViewModel(private val repository: FestivalRepository) : ViewModel() {
    /** Flujo de estado que expone la lista de eventos locales desde Room. */
    val eventos: StateFlow<List<EventoEntity>> = repository.getEventosLocales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Guarda o actualiza un evento.
     * @param token Token JWT del administrador.
     * @param id ID del evento (null si es creación).
     * @param title Título del evento.
     */
    fun saveEvent(token: String?, id: String?, title: String, date: String, location: String, price: String) {
        // Genera el DTO y llama a repository.addEvento o repository.updateEvento
    }
}
```

---

## 7. Paso 6: Gestión de Usuarios y Roles

A través de `AdminUsersViewModel`, los administradores pueden registrar otros administradores o cambiar los roles.

```kotlin
/**
 * ViewModel para gestionar usuarios desde el panel de administrador.
 * 
 * Responsabilidades:
 * - Cargar la lista de todos los usuarios registrados.
 * - Cambiar el rol (USUARIO <-> ADMINISTRADOR).
 */
class AdminUsersViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * Intercambia el rol del usuario especificado.
     * @param usuarioId ID del usuario a modificar.
     * @param currentRole El rol actual ("USUARIO" o "ADMINISTRADOR").
     */
    fun toggleRole(usuarioId: String, currentRole: String) {
        val newRole = if (currentRole == "ADMINISTRADOR") "USUARIO" else "ADMINISTRADOR"
        // Llama a API y actualiza el StateFlow
    }
}
```

---

## 8. Paso 7: Compra de Boletos y Código QR

La compra de boletos está gestionada por el `CheckoutViewModel`.

```kotlin
/**
 * ViewModel que gestiona la lógica de compra de boletos.
 *
 * Responsabilidades:
 * - Validar los datos de la tarjeta localmente.
 * - Enviar la petición de compra al backend llamando a `POST /boletos/comprar`.
 */
class CheckoutViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * Procesa el pago de boletos validando la tarjeta y llamando al API.
     */
    fun procesarPago(eventoId: String, categoria: String, cantidad: Int, precioTotal: Int, tarjetaNumero: String, tarjetaVencimiento: String, tarjetaCVV: String) {
        // Validaciones...
        // Llamada a api.comprarBoleto()
    }
}
```

Una vez completado el pago, el usuario puede ver su boleto generado con un **código QR** desde `QrCodeGenerator.kt`.

---

## 9. Paso 8: Live Stream y Chat con Polling

Los usuarios pueden ver transmisiones en vivo gracias a `ExoPlayer`, mientras que el administrador transmite desde la cámara de su dispositivo con la librería **RootEncoder**.

En `LiveViewModel.kt`, se maneja la obtención y envío de mensajes en el chat de la transmisión:

```kotlin
/**
 * ViewModel que controla la lógica de la transmisión en vivo y el chat (Polling).
 */
class LiveViewModel(application: Application) : AndroidViewModel(application) {
    // Implementa la lógica para refrescar el chat cada N segundos
    // y para mandar mensajes a la sala del stream activo.
}
```

---

## 10. Paso 9: Flujo completo Usuario vs Administrador

Dependiendo del rol en el JWT, el usuario es redirigido:

- **USUARIO**: 
  - `DashboardScreen`: Muestra opciones para comprar boletos (`TicketsScreen`).
  - Ver el mapa de los escenarios (`MapScreen`).
  - Entrar al Live Stream del concierto en curso (`UserLiveStreamScreen`).
  - Ver sus boletos y perfil (`ProfileScreen`).
  
- **ADMINISTRADOR**:
  - `AdminDashboardScreen`: Tiene accesos directos de administración.
  - CRUD de Eventos (`AdminManageScreen`).
  - Cambiar privilegios de usuarios (`AdminUsersScreen`).
  - Transmitir la cámara del dispositivo al servidor RTSP (`AdminLiveStreamScreen`).
  - Escanear Códigos QR en la entrada del evento (`AdminScannerScreen`).

---

## 11. Paso 10: Compilación y Comandos Gradle

Para limpiar, compilar e instalar la aplicación en un dispositivo conectado:

```bash
# Limpiar el proyecto
./gradlew :app:clean

# Compilar un APK de depuración
./gradlew :app:assembleDebug

# Instalar el APK directamente en el emulador o dispositivo
./gradlew :app:installDebug
```

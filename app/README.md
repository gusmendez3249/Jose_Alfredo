# 📱 Módulo `app` — Guía Paso a Paso y Código Documentado

> Aplicación Android nativa desarrollada con **Kotlin** y **Jetpack Compose** para usuarios finales y administradores del **Festival José Alfredo Jiménez**.

---

## 📋 Índice
1. [Requisitos Previos](#1-requisitos-previos)
2. [Estructura Completa del Módulo](#2-estructura-completa-del-módulo)
3. [Paso 1: Configuración en Android Studio](#paso-1-configuración-en-android-studio)
4. [Paso 2: Compilación y Verificación de Dependencias](#paso-2-compilación-y-verificación-de-dependencias)
5. [Paso 3: Configuración de Red para Backend Local](#paso-3-configuración-de-red-para-backend-local)
6. [Paso 4: Ejecución en Emulador o Dispositivo Físico](#paso-4-ejecución-en-emulador-o-dispositivo-físico)
7. [Paso 5: Flujo de Uso Paso a Paso (Usuario Espectador)](#paso-5-flujo-de-uso-paso-a-paso-usuario-espectador)
8. [Paso 6: Flujo de Uso Paso a Paso (Administrador)](#paso-6-flujo-de-uso-paso-a-paso-administrador)
9. [Paso 7: Código Fuente Explicado y Documentado](#paso-7-código-fuente-explicado-y-documentado)
10. [Paso 8: Solución de Problemas Frecuentes](#paso-8-solución-de-problemas-frecuentes)

---

## 1. Requisitos Previos

- **Android Studio**: Jellyfish / Koala o superior.
- **JDK**: OpenJDK 17 configurado en Gradle.
- **Android SDK**: API 34 (Android 14) o superior (mínimo soportado: API 24).
- **Backend NestJS**: Debe estar en ejecución en `http://localhost:3001` (ver guía de `festivaltrack-backend`).

---

## 2. Estructura Completa del Módulo

```
app/src/main/java/mx/utng/festivaltrack/app/
│
├── MainActivity.kt                  # Punto de entrada. NavHost y rutas globales.
├── FestivalTrackApplication.kt      # Contenedor global de dependencias (AppContainer).
│
├── data/
│   └── TokenManager.kt              # Persistencia segura del token JWT en SharedPreferences.
│
├── di/
│   └── AppModule.kt                 # Inyección de dependencias manual.
│
└── ui/
    ├── screens/
    │   ├── WelcomeScreen.kt         # Bienvenida y splash.
    │   ├── LoginScreen.kt           # Autenticación JWT.
    │   ├── RegisterScreen.kt        # Registro de usuarios.
    │   ├── MainScreen.kt            # Contenedor con BottomNav (usuario).
    │   ├── DashboardScreen.kt       # Portada principal y eventos.
    │   ├── AudioScreen.kt           # Reproductor musical.
    │   ├── BiographyScreen.kt       # Biografía interactiva.
    │   ├── GalleryScreen.kt         # Galería de fotos oficial.
    │   ├── MapScreen.kt             # Mapa de escenarios recinto.
    │   ├── TicketsScreen.kt         # Selección de boletos.
    │   ├── CheckoutScreen.kt        # Formulario de pago seguro.
    │   ├── TicketSuccessScreen.kt   # Confirmación con código QR.
    │   ├── ProfileScreen.kt         # Perfil del usuario y logout.
    │   ├── UserLiveStreamScreen.kt  # Reproductor live stream + chat.
    │   ├── AdminMainScreen.kt       # Contenedor BottomNav (admin).
    │   ├── AdminDashboardScreen.kt  # Panel principal del administrador.
    │   ├── AdminLiveStreamScreen.kt # ⭐ Emisión de video cámara RTSP (puerto 1935).
    │   ├── AdminUploadScreen.kt     # Subida de contenido.
    │   ├── AdminManageScreen.kt     # CRUD de eventos.
    │   ├── AdminCreateEventScreen.kt# Creación de eventos.
    │   └── AdminScannerScreen.kt   # Escáner QR de entradas.
    │
    └── viewmodels/
        ├── AuthViewModel.kt         # Gestión de login/registro.
        ├── EventosViewModel.kt      # Carga de eventos.
        ├── CheckoutViewModel.kt     # Proceso de pago.
        └── AdminManageViewModel.kt  # Operaciones CRUD admin.
```

---

## Paso 1: Configuración en Android Studio

1. Abre Android Studio.
2. Selecciona **Open** y navega a la carpeta raíz del proyecto (`Jose_Alfredo`).
3. Espera a que Gradle sincronice todas las dependencias del proyecto (*Gradle Sync*).

---

## Paso 2: Compilación y Verificación de Dependencias

Puedes compilar la app desde consola usando Gradle Wrapper:

```powershell
.\gradlew.bat :app:assembleDebug
```

**Ubicación del APK resultante:**
`app/build/outputs/apk/debug/app-debug.apk`

---

## Paso 3: Configuración de Red para Backend Local

La app conecta con el backend usando Retrofit apuntando por defecto a:
`http://10.0.2.2:3001/api/v1/`

- **En Emulador Android**: `10.0.2.2` apunta automáticamente al `localhost` de tu computadora.
- **En Celular Físico**: Cambia la dirección en `shared/src/.../remote/FestivalApiService.kt` a la IP local de tu máquina en la red Wi-Fi (ej. `http://192.168.1.50:3001/api/v1/`).

---

## Paso 4: Ejecución en Emulador o Dispositivo Físico

1. En la barra superior de Android Studio, selecciona el target **`app`**.
2. Elige tu emulador o dispositivo Android físico.
3. Haz clic en **Run (▶️)** o presiona `Shift + F10`.

---

## Paso 5: Flujo de Uso Paso a Paso (Usuario Espectador)

1. **Pantalla de Bienvenida**: Presiona *"Iniciar Sesión"*.
2. **Registro**: Si no tienes cuenta, selecciona *"Regístrate"*, ingresa tu nombre, correo y contraseña.
3. **Inicio de Sesión**: Inicia sesión. La app guardará tu token JWT y te llevará al **Dashboard**.
4. **Explorar Eventos**: Revisa la agenda de actuaciones y artistas.
5. **Comprar Boletos**:
   - Selecciona un evento.
   - Elige cantidad y categoría (*General / VIP*).
   - Presiona *"Proceder al Pago"*.
   - Ingresa los datos del formulario en **`CheckoutScreen`** y confirma.
   - Verás la pantalla **`TicketSuccessScreen`** con tu código QR generado.
6. **Ver Transmisión en Vivo**:
   - Toca *"Ver Transmisión en Vivo"* para abrir el reproductor y el chat.

---

## Paso 6: Flujo de Uso Paso a Paso (Administrador)

1. **Login como Admin**:
   - **Correo**: `admin@admin.com`
   - **Contraseña**: `admin123`
2. **Panel de Administración**:
   - Accede a las pestañas *Inicio, Subir, Gestionar, Perfil*.
3. **Transmisión de Cámara en Vivo (RTSP)**:
   - Ve a la sección de Transmisión.
   - Otorga permisos de **Cámara** y **Micrófono**.
   - Presiona *"INICIAR EN VIVO"*. Tu teléfono comenzará a emitir la señal de video RTSP en `rtsp://<IP>:1935`.
4. **Escáner QR**:
   - Abre la herramienta de escaneo de cámara para leer boletos de visitantes en el recinto.
5. **Cerrar Sesión Segura**:
   - Ve a la pestaña *"Perfil"*, presiona *"Cerrar Sesión"* y confirma. El token JWT y el backstack se limpiarán completamente.

---

## Paso 7: Código Fuente Explicado y Documentado

### 1. `MainActivity.kt` — Grafo de Navegación Completo
```kotlin
// Grafo de navegación principal de la aplicación
NavHost(navController = navController, startDestination = "login") {
    composable("login") {
        LoginScreen(
            onNavigateToDashboard = { 
                navController.navigate("main") { popUpTo("login") { inclusive = true } }
            },
            onNavigateToAdmin = {
                navController.navigate("admin_dashboard") { popUpTo("login") { inclusive = true } }
            }
        )
    }
    composable("admin_dashboard") {
        AdminMainScreen(
            onLogout = {
                // Borra token JWT y destruye toda la pila de navegación
                TokenManager(this@MainActivity).clear()
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
            }
        )
    }
}
```

### 2. `AdminLiveStreamScreen.kt` — Servidor de Cámara RTSP Nativo
```kotlin
// Inicialización del servidor RTSP en el puerto TCP 1935 mediante SurfaceView
SurfaceView(ctx).apply {
    holder.addCallback(object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            val server = RtspServerCamera1(this@apply, connectChecker, 1935)
            rtspServer = server
            server.startPreview() // Inicia vista previa de cámara física
        }
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            if (isStreaming) rtspServer?.stopStream()
            rtspServer?.stopPreview()
        }
    })
}
```

### 3. `AuthViewModel.kt` — Gestión de Estados de Autenticación
```kotlin
// Estado de autenticación mediante Sealed Class
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

fun login(correo: String, contrasena: String) {
    viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            val response = api.login(LoginDto(correo, contrasena))
            tokenManager.saveToken(response.accessToken)
            tokenManager.saveUserRole(response.usuario.rol)
            _authState.value = AuthState.Success(response.usuario.rol)
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Credenciales inválidas o error de red")
        }
    }
}
```

---

## Paso 8: Solución de Problemas Frecuentes

### Error de permisos al iniciar la cámara
- Ve a Ajustes de Android -> Aplicaciones -> FestivalTrack -> Permisos -> Activa Cámara y Micrófono.

### No carga información del backend
- Verifica que el backend NestJS esté corriendo en `http://localhost:3001`.

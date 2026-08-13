# 📺 Módulo `tv` — Guía Paso a Paso y Código Documentado

> Aplicación Android TV desarrollada con **Compose for TV** y **ExoPlayer (Media3)** para transmitir el **Festival José Alfredo Jiménez** en pantallas grandes.

---

## 📋 Índice
1. [Requisitos Previos](#1-requisitos-previos)
2. [Estructura Completa del Módulo](#2-estructura-completa-del-módulo)
3. [Paso 1: Configurar Emulador de Android TV](#paso-1-configurar-emulador-de-android-tv)
4. [Paso 2: Redirección ADB para el Puerto RTSP (1935)](#paso-2-redirección-adb-para-el-puerto-rtsp-1935)
5. [Paso 3: Compilación y Ejecución](#paso-3-compilación-y-ejecución)
6. [Paso 4: Guía de Navegación con D-Pad (Control Remoto)](#paso-4-guía-de-navegación-con-d-pad-control-remoto)
7. [Paso 5: Probar Transmisión en Vivo y Chat Comunitario](#paso-5-probar-transmisión-en-vivo-y-chat-comunitario)
8. [Paso 6: Código Fuente Explicado y Documentado](#paso-6-código-fuente-explicado-y-documentado)
9. [Paso 7: Solución de Problemas Frecuentes](#paso-7-solución-de-problemas-frecuentes)

---

## 1. Requisitos Previos

- **Android Studio** Jellyfish / Koala o superior.
- **Emulador de Android TV**: AVD con resolución 1080p (API 30 o API 34).
- **Backend NestJS**: Ejecutándose en puerto `3001`.
- **App Móvil (opcional)**: Transmitiendo video por la cámara en puerto `1935`.

---

## 2. Estructura Completa del Módulo

```
tv/src/main/java/mx/utng/festivaltrack/tv/
│
├── MainActivity.kt                           # Entry Point TV. Contenedor de navegación por sidebar.
│
├── presentation/
│   ├── components/
│   │   └── SidebarMenuItem.kt                # Ítem de menú lateral enfocable por D-Pad.
│   │
│   ├── screens/
│   │   ├── TvHomeScreen.kt                   # Pantalla principal con agenda destacada.
│   │   ├── TvHistoricalGalleryScreen.kt      # Galería fotográfica histórica.
│   │   ├── TvLiveStreamScreen.kt             # ⭐ Reproductor RTSP + Chat de la comunidad.
│   │   ├── TvProgramacionScreen.kt           # Cronograma completo de actividades.
│   │   └── TvSettingsScreen.kt               # Ajustes y datos técnicos de conexión.
│   │
│   └── viewmodel/
│       └── TvViewModel.kt                    # Gestión de estado UI para TV.
│
└── ui/
    ├── theme/                                # Paleta de colores y estilos oscuros para TV.
    └── utils/
        └── DynamicQrCode.kt                  # Generador de QR para la pantalla.
```

---

## Paso 1: Configurar Emulador de Android TV

1. Abre el **Device Manager** en Android Studio.
2. Selecciona **Create Device**.
3. Elige la categoría **TV** -> **Television (1080p)**.
4. Selecciona la imagen del sistema (API 30+) y presiona **Finish**.
5. Inicia la Smart TV virtual.

---

## Paso 2: Redirección ADB para el Puerto RTSP (1935)

Para conectar el flujo de video emitido por el teléfono hacia la Smart TV:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" forward tcp:1935 tcp:1935
```

---

## Paso 3: Compilación y Ejecución

Selecciona el módulo **`tv`** en Android Studio y presiona **Run (▶️)** o compila desde terminal:

```powershell
.\gradlew.bat :tv:assembleDebug
```

---

## Paso 4: Guía de Navegación con D-Pad (Control Remoto)

- **Flechas Arriba / Abajo**: Moverse por las secciones del sidebar (*Inicio, Galería, En Vivo, Programación, Ajustes*).
- **Flecha Derecha**: Pasar del sidebar al contenido principal de la pantalla.
- **Teclas Enter / Espacio (OK)**: Activar elementos, pausar/reanudar streaming o abrir el cuadro de diálogo para comentar en el chat.

---

## Paso 5: Probar Transmisión en Vivo y Chat Comunitario

1. Navega a **"Transmisión En Vivo"**.
2. **Distribución**:
   - **68% Izquierda**: Reproductor ExoPlayer con streaming en directo (`rtsp://10.0.2.2:1935`).
   - **32% Derecha**: Chat público con actualización automática mediante polling cada 3 segundos.
3. **Escribir en el Chat**:
   - Resalta el botón *"Pulsa OK para escribir un mensaje..."*.
   - Presiona **OK / Enter**.
   - Se abrirá un cuadro emergente modal con teclado.
   - Escribe tu comentario y presiona el botón dorado **"Enviar"**.

---

## Paso 6: Código Fuente Explicado y Documentado

### 1. `TvLiveStreamScreen.kt` — Configuración de ExoPlayer para RTSP
```kotlin
// Creación del reproductor de video para la URL del flujo RTSP
val streamUrl = "rtsp://10.0.2.2:1935"
val exoPlayer = remember {
    ExoPlayer.Builder(context).build().apply {
        val mediaItem = MediaItem.fromUri(streamUrl)
        setMediaItem(mediaItem)
        prepare()
        playWhenReady = true
    }
}
```

### 2. Polling de Chat cada 3 Segundos
```kotlin
// Actualización automática de mensajes desde el backend NestJS
LaunchedEffect(Unit) {
    while (true) {
        try {
            val msgs = api.getChatMessages("EVT-001")
            if (msgs.isNotEmpty()) {
                chatMessages.clear()
                msgs.forEach { m -> chatMessages.add(ChatMessage(m.usuarioNombre, m.mensaje, "En vivo", m.esAdmin)) }
            }
        } catch (e: Exception) {}
        kotlinx.coroutines.delay(3000)
    }
}
```

### 3. `TvChatInputDialog` — Diálogo Accesible para Control Remoto
```kotlin
// Cuadro modal de texto que solicita automáticamente el foco del teclado para D-Pad
@Composable
fun TvChatInputDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escribe tu mensaje", color = Color.White) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Button(onClick = { onSend(text); onDismiss() }) { Text("Enviar") }
        }
    )
}
```

---

## Paso 7: Solución de Problemas Frecuentes

### El video no carga
- Comprueba que la app móvil esté transmitiendo y ejecutaste `adb forward tcp:1935 tcp:1935`.

### No se puede escribir en el chat
- En Android TV no uses clics directos sobre el texto. Siempre selecciona el componente con el botón **OK / Enter** del control remoto para abrir el diálogo emergente con teclado.

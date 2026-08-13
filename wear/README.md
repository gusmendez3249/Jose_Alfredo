# ⌚ Módulo `wear` — Guía Paso a Paso y Código Documentado

> Aplicación para relojes inteligentes **WearOS**, desarrollada con **Compose for Wear OS** para consultar la agenda oficial y notificaciones del **Festival José Alfredo Jiménez** en pantallas circulares.

---

## 📋 Índice
1. [Requisitos Previos](#1-requisitos-previos)
2. [Estructura Completa del Módulo](#2-estructura-completa-del-módulo)
3. [Paso 1: Configurar Emulador WearOS en Android Studio](#paso-1-configurar-emulador-wearos-en-android-studio)
4. [Paso 2: Compilación con Gradle](#paso-2-compilación-con-gradle)
5. [Paso 3: Ejecución y Pruebas en el Reloj](#paso-3-ejecución-y-pruebas-en-el-reloj)
6. [Paso 4: Código Fuente Explicado y Documentado](#paso-4-código-fuente-explicado-y-documentado)
7. [Paso 5: Solución de Problemas Frecuentes](#paso-5-solución-de-problemas-frecuentes)

---

## 1. Requisitos Previos

- **Android Studio** Jellyfish / Koala o superior.
- **Emulador WearOS**: AVD con pantalla circular (*Wear OS Small Round*, API 30+).
- **Backend NestJS**: Corriendo en `http://localhost:3001`.

---

## 2. Estructura Completa del Módulo

```
wear/src/main/java/mx/utng/festivaltrack/wear/
│
├── data/                    # Acceso a datos e integración con el módulo shared.
├── domain/                  # Lógica de negocio y casos de uso para WearOS.
└── presentation/            # Componentes UI optimizados para pantallas circulares.
```

---

## Paso 1: Configurar Emulador WearOS en Android Studio

1. Abre **Device Manager** en Android Studio.
2. Haz clic en **Create Device**.
3. Selecciona la categoría **Wear OS** -> **Wear OS Small Round** (o *Large Round*).
4. Selecciona la imagen del sistema sugerida (API 30+) y presiona **Finish**.
5. Enciende el reloj virtual.

---

## Paso 2: Compilación con Gradle

Puedes verificar la compilación de la app WearOS desde consola:

```powershell
.\gradlew.bat :wear:assembleDebug
```

---

## Paso 3: Ejecución y Pruebas en el Reloj

1. En la barra superior de Android Studio, selecciona la configuración **`wear`**.
2. Elige el emulador de reloj WearOS activo.
3. Haz clic en **Run (▶️)** o presiona `Shift + F10`.
4. Desliza verticalmente en la pantalla táctil circular del reloj para explorar la lista de eventos.

---

## Paso 4: Código Fuente Explicado y Documentado

### Integración de la Librería Shared (`build.gradle.kts`)
```kotlin
dependencies {
    implementation(project(":shared"))
    implementation("androidx.wear.compose:compose-material:1.3.0")
    implementation("androidx.wear.compose:compose-foundation:1.3.0")
    implementation("androidx.wear:wear:1.3.0")
}
```

---

## Paso 5: Solución de Problemas Frecuentes

### Error al cargar información en el reloj
- Asegúrate de que el servidor backend NestJS esté corriendo en puerto `3001` y la computadora no esté bloqueando las peticiones locales del emulador.

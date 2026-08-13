# 🔗 Módulo `shared` — Guía Paso a Paso y Código Documentado

> Librería Kotlin que centraliza la capa de datos, cliente HTTP Retrofit, base de datos local Room, DTOs y el repositorio **Offline-First** para los módulos **Android Móvil**, **Smart TV** y **WearOS**.

---

## 📋 Índice
1. [Propósito y Arquitectura](#propósito-y-arquitectura)
2. [Estructura Completa del Módulo](#estructura-completa-del-módulo)
3. [Paso 1: Integración en un Módulo Android](#paso-1-integración-en-un-módulo-android)
4. [Paso 2: Guía de Uso del Repositorio (Offline-First)](#paso-2-guía-de-uso-del-repositorio-offline-first)
5. [Paso 3: Guía de Uso del Cliente HTTP (Retrofit API)](#paso-3-guía-de-uso-del-cliente-http-retrofit-api)
6. [Paso 4: Código Fuente Explicado y Documentado](#paso-4-código-fuente-explicado-y-documentado)
7. [Paso 5: Compilación y Verificación](#paso-5-compilación-y-verificación)

---

## Propósito y Arquitectura

```
┌───────────────────────────────────────────────────────────┐
│                    Móvil / TV / WearOS                    │
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                    MÓDULO SHARED                          │
│                                                           │
│   ┌───────────────────────────────────────────────────┐   │
│   │                 FestivalRepository                │   │
│   └─────────────┬───────────────────────┬─────────────┘   │
│                 │                       │                 │
│                 ▼                       ▼                 │
│   ┌──────────────────────────┐ ┌──────────────────────┐   │
│   │  Room DB (EventoEntity)  │ │ FestivalApiService   │   │
│   │  (Almacenamiento Local)  │ │ (Retrofit / HTTP)    │   │
│   └──────────────────────────┘ └──────────────────────┘   │
└───────────────────────────────────────────────────────────┘
```

---

## Estructura Completa del Módulo

```
shared/src/main/java/mx/utng/festivaltrack/shared/
│
├── data/
│   ├── remote/
│   │   └── FestivalApiService.kt   # ⭐ Cliente Retrofit, factory method y DTOs.
│   │
│   ├── local/
│   │   ├── entity/
│   │   │   └── EventoEntity.kt     # Entidad de tabla SQLite / Room.
│   │   └── dao/
│   │       └── EventoDao.kt        # Interface DAO de Room con consultas SQL.
│   │
│   └── repository/
│       └── FestivalRepository.kt   # ⭐ Orquestador de datos Offline-First.
```

---

## Paso 1: Integración en un Módulo Android

Agrega la referencia al proyecto `:shared` en el archivo `build.gradle.kts` de tu aplicación Android:

```kotlin
dependencies {
    implementation(project(":shared"))
}
```

---

## Paso 2: Guía de Uso del Repositorio (Offline-First)

El `FestivalRepository` lee siempre los datos primero desde la base local (Room SQLite) y se encarga de descargar las actualizaciones desde el backend NestJS cuando existe conexión de red.

```kotlin
val apiService = FestivalApiService.create()
val repository = FestivalRepository(eventoDao, apiService)

// Obtener flujo reactivo de eventos
val eventosFlow: Flow<List<EventoEntity>> = repository.getEventosLocales()

// Sincronizar eventos con el servidor en segundo plano
viewModelScope.launch {
    repository.syncEventos()
}
```

---

## Paso 3: Guía de Uso del Cliente HTTP (Retrofit API)

```kotlin
val api = FestivalApiService.create()

// Ejemplos de uso asíncrono
val authResponse = api.login(LoginDto("admin@admin.com", "admin123"))
val listaEventos = api.getEventos()
val listaChat = api.getChatMessages("EVT-001")
```

---

## Paso 4: Código Fuente Explicado y Documentado

### 1. `FestivalRepository.kt` — Implementación Offline-First
```kotlin
/**
 * Sincroniza la lista de eventos entre PostgreSQL (NestJS) y la base de datos local (Room).
 */
suspend fun syncEventos() {
    withContext(Dispatchers.IO) {
        try {
            // 1. Consulta al backend remoto
            val remoteEventos = apiService.getEventos()
            
            // 2. Transforma DTOs a entidades de Room
            val localEntities = remoteEventos.map { it.toEntity() }
            
            // 3. Limpia e inserta las nuevas entidades en SQLite local
            eventoDao.deleteAll()
            eventoDao.upsertAll(localEntities)
        } catch (e: Exception) {
            // Si no hay red, conserva la información en caché local sin interrumpir al usuario
            e.printStackTrace()
        }
    }
}
```

### 2. `FestivalApiService.kt` — Definición de Cliente Retrofit
```kotlin
companion object {
    private const val BASE_URL = "http://10.0.2.2:3001/api/v1/"

    fun create(): FestivalApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FestivalApiService::class.java)
    }
}
```

---

## Paso 5: Compilación y Verificación

Para verificar la compilación independiente de la librería:

```powershell
.\gradlew.bat :shared:assembleDebug
```

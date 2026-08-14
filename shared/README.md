# FestivalTrack - Módulo Shared (Librería Compartida)

Este documento describe la arquitectura y configuración del módulo `shared`, el cual centraliza toda la capa de datos de la plataforma FestivalTrack.

## 1. Propósito

El módulo `shared` es una **Android Library** que agrupa toda la lógica de acceso a datos (Local y Remoto), repositorios y modelos de dominio. Su propósito es evitar la duplicación de código en un ecosistema multi-dispositivo, proveyendo un único punto de verdad para los datos consumidos por las distintas interfaces.

## 2. Arquitectura

El ecosistema está diseñado con una arquitectura modular. Las diferentes aplicaciones (Móvil, TV y Wear) consumen el módulo `shared` para interactuar con la Base de Datos Local y la API Remota.

```text
+----------------+       +----------------+       +----------------+
|  App Móvil     |       |  App Android TV|       |  App Wear OS   |
| (Admin / User) |       | (Transmisión)  |       | (Notific. / QR)|
+-------+--------+       +-------+--------+       +-------+--------+
        |                        |                        |
        +------------------------+------------------------+
                                 |
                                 v
                       +-------------------+
                       |   Módulo Shared   |
                       | (Android Library) |
                       +---------+---------+
                                 |
           +---------------------+---------------------+
           |                                           |
           v                                           v
+---------------------+                      +---------------------+
|      Caché Local    | <--- Offline-First --- |      API Remota     |
|   (Room / SQLite)   |                      | (NestJS / Postgres) |
+---------------------+                      +---------------------+
```

## 3. Estructura de Directorios

La estructura interna del módulo `shared` sigue el patrón de separación por capas (remoto, local y repositorio):

```text
shared/
├── src/main/java/mx/utng/festivaltrack/shared/
│   ├── data/
│   │   ├── local/
│   │   │   ├── dao/
│   │   │   │   ├── ArtistaDao.kt
│   │   │   │   └── EventoDao.kt
│   │   │   ├── entity/
│   │   │   │   ├── ArtistaEntity.kt
│   │   │   │   └── EventoEntity.kt
│   │   │   └── FestivalDatabase.kt
│   │   ├── remote/
│   │   │   └── FestivalApiService.kt (Incluye todos los DTOs)
│   │   └── repository/
│   │       └── FestivalRepository.kt
│   └── model/
│       └── Models.kt
├── build.gradle.kts
└── proguard-rules.pro
```

## 4. Paso 1: Configuración en `settings.gradle.kts`

Para incluir la librería compartida en el proyecto principal, debes declararla en el archivo `settings.gradle.kts` raíz:

```kotlin
include(":shared")
```

## 5. Paso 2: `build.gradle.kts` del módulo shared

El archivo `build.gradle.kts` del módulo define que se trata de una librería (`com.android.library`) e incluye las dependencias necesarias de Retrofit, Gson, Room y Coroutines:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "mx.utng.festivaltrack.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    // Room (Base de datos local)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Retrofit (Cliente HTTP) y Gson
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
```

## 6. Paso 3: `FestivalApiService.kt` (Endpoints Remotos)

El cliente HTTP que conecta con la API Rest de NestJS. Aquí un extracto con KDoc:

```kotlin
/**
 * Interfaz Retrofit que define todos los endpoints del API REST de FestivalTrack.
 */
interface FestivalApiService {

    /**
     * Obtiene la lista completa de eventos del festival.
     * @return Lista de [EventoDto] con información de cada evento.
     */
    @GET("eventos")
    suspend fun getEventos(): List<EventoDto>

    /**
     * Inicia sesión con las credenciales del usuario.
     * @param request DTO con correo y contraseña.
     * @return [AuthResponseDto] con el token JWT y datos del usuario.
     */
    @retrofit2.http.POST("auth/login")
    suspend fun login(@retrofit2.http.Body request: LoginDto): AuthResponseDto

    // ... otros métodos documentados con KDoc
}
```

## 7. Paso 4: DTOs Principales

Los Data Transfer Objects permiten mapear la respuesta JSON al ecosistema Kotlin. 

```kotlin
/**
 * Representa un evento del festival con toda su información asociada.
 * @property id Identificador único del evento.
 * @property nombre Nombre del evento.
 * ...
 */
data class EventoDto(
    val id: String,
    val nombre: String,
    val fechaHora: String,
    val ubicacion: String,
    // ...
)

/**
 * Respuesta del servidor tras un login o registro exitoso.
 * @property accessToken Token JWT.
 * @property usuario Datos del usuario autenticado.
 */
data class AuthResponseDto(
    val accessToken: String,
    val usuario: UsuarioDto
)
// Igualmente se documentaron LoginDto, RegisterDto, CompraBoletoDto, ChatMessageDto, UsuarioDto, RoleUpdateDto
```

## 8. Paso 5: Room Database (Acceso Local)

La capa local está gobernada por Room, que almacena en SQLite los datos clave para usarse offline.

**Entidad (`EventoEntity.kt`):**
```kotlin
/**
 * Entidad de base de datos Room que representa un Evento almacenado localmente.
 * Se utiliza para mantener una caché local de los eventos y permitir
 * que la aplicación funcione en modo Offline-First.
 */
@Entity(tableName = "eventos")
data class EventoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val fechaHora: String,
    // ...
)
```

**DAO (`EventoDao.kt`):**
```kotlin
/**
 * Data Access Object (DAO) para la entidad [EventoEntity].
 */
@Dao
interface EventoDao {
    /**
     * Observa todos los eventos ordenados por fecha de forma ascendente.
     * @return [Flow] que emite la lista de eventos cada vez que hay un cambio.
     */
    @Query("SELECT * FROM eventos ORDER BY fechaHora ASC")
    fun observeTodos(): Flow<List<EventoEntity>>
    
    // ... @Upsert, @Delete, @Query ...
}
```

**Base de Datos (`FestivalDatabase.kt`):**
```kotlin
/**
 * Clase principal de la base de datos Room para FestivalTrack.
 */
@Database(
    entities = [EventoEntity::class, ArtistaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FestivalDatabase : RoomDatabase() { ... }
```

## 9. Paso 6: `FestivalRepository` (Offline-First)

El repositorio orquesta ambas capas (Remota y Local).

```kotlin
/**
 * Repositorio central de datos para la plataforma FestivalTrack.
 * Implementa el patrón **Offline-First**.
 */
class FestivalRepository(
    private val eventoDao: EventoDao,
    private val apiService: FestivalApiService = FestivalApiService.create()
) {
    /**
     * Sincroniza la base de datos local con el servidor Postgres remoto.
     * Estrategia Offline-First: ...
     */
    suspend fun syncEventos() {
        // 1. Obtiene remoteEventos
        // 2. Transforma a Entity
        // 3. Upsert en Room
        // 4. Borra locales obsoletos (soft-delete remote detection)
    }

    /**
     * Agrega un nuevo evento tanto en la base local como en el servidor remoto.
     */
    suspend fun addEvento(...) {
        // Offline-first: inserta local de inmediato, luego hace el POST en red
    }
}
```

## 10. Paso 7: Cómo integrar en otros módulos

Cualquier módulo (app, wear, tv) que requiera acceso a los datos, solo necesita incluir el `shared` en sus dependencias dentro de su respectivo `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":shared"))
}
```

## 11. Paso 8: Compilación Independiente

El módulo `shared` se puede construir o limpiar de manera aislada sin tener que compilar todos los front-ends:

```bash
# Compilar solo el módulo shared
./gradlew :shared:assemble

# Correr pruebas y análisis en el shared
./gradlew :shared:check
```

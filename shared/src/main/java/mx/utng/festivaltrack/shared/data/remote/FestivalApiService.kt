package mx.utng.festivaltrack.shared.data.remote

import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import retrofit2.http.GET
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Interfaz Retrofit que define todos los endpoints del API REST de FestivalTrack.
 *
 * Se instancia a través del companion object [create], que configura Retrofit
 * con la URL base del backend y el convertidor Gson para serialización JSON.
 *
 * URL Base: `http://10.0.2.2:3001/api/v1/`
 * Nota: `10.0.2.2` es el alias del host (PC) desde dentro del emulador Android.
 *
 * Uso:
 * ```kotlin
 * val api = FestivalApiService.create()
 * val eventos = api.getEventos()
 * ```
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
     * @throws retrofit2.HttpException si las credenciales son inválidas (401).
     */
    @retrofit2.http.POST("auth/login")
    suspend fun login(@retrofit2.http.Body request: LoginDto): AuthResponseDto

    /**
     * Registra un nuevo usuario en el sistema.
     * @param request DTO con nombre, correo y contraseña.
     * @return [AuthResponseDto] con el token JWT del usuario recién creado.
     * @throws retrofit2.HttpException si el correo ya está registrado (409).
     */
    @retrofit2.http.POST("auth/register")
    suspend fun register(@retrofit2.http.Body request: RegisterDto): AuthResponseDto

    /**
     * Crea un nuevo evento del festival (requiere rol ADMIN).
     * @param token Token JWT del admin en formato "Bearer <token>".
     * @param evento Datos del evento a crear.
     * @return [EventoDto] del evento creado con su ID asignado.
     */
    @retrofit2.http.POST("eventos")
    suspend fun createEvento(
        @retrofit2.http.Header("Authorization") token: String? = null,
        @retrofit2.http.Body evento: EventoCreateDto
    ): EventoDto

    /**
     * Actualiza un evento existente por su ID (requiere rol ADMIN).
     * @param token Token JWT del admin en formato "Bearer <token>".
     * @param id Identificador único del evento (ej. "EVT-001").
     * @param evento Nuevos datos del evento.
     * @return [EventoDto] actualizado.
     */
    @retrofit2.http.PUT("eventos/{id}")
    suspend fun updateEvento(
        @retrofit2.http.Header("Authorization") token: String? = null,
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body evento: EventoCreateDto
    ): EventoDto

    /**
     * Elimina un evento por su ID (requiere rol ADMIN).
     * @param token Token JWT del admin en formato "Bearer <token>".
     * @param id Identificador único del evento.
     */
    @retrofit2.http.DELETE("eventos/{id}")
    suspend fun deleteEvento(
        @retrofit2.http.Header("Authorization") token: String? = null,
        @retrofit2.http.Path("id") id: String
    )

    /**
     * Compra un boleto para un evento.
     * @param token Token JWT del usuario en formato "Bearer <token>".
     * @param request Datos de la compra: eventoId, categoría, cantidad, precio y método de pago.
     * @return [BoletoDto] con el boleto generado y su código QR único.
     */
    @retrofit2.http.POST("boletos/comprar")
    suspend fun comprarBoleto(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: CompraBoletoDto
    ): BoletoDto

    /**
     * Obtiene los boletos del usuario autenticado.
     * @param token Token JWT del usuario en formato "Bearer <token>".
     * @return Lista de [BoletoDto] del usuario.
     */
    @retrofit2.http.GET("boletos/mis-boletos")
    suspend fun getMisBoletos(
        @retrofit2.http.Header("Authorization") token: String
    ): List<BoletoDto>

    /**
     * Obtiene los mensajes del chat de un evento en transmisión.
     * Se usa con polling periódico para simular tiempo real.
     * @param eventoId ID del evento (ej. "EVT-001").
     * @return Lista de [ChatMessageDto] ordenados cronológicamente.
     */
    @retrofit2.http.GET("stream/chat/{eventoId}")
    suspend fun getChatMessages(
        @retrofit2.http.Path("eventoId") eventoId: String
    ): List<ChatMessageDto>

    /**
     * Envía un mensaje al chat de la transmisión en vivo.
     * @param request Mensaje con el ID del evento, nombre del usuario y texto.
     */
    @retrofit2.http.POST("stream/chat")
    suspend fun sendChatMessage(
        @retrofit2.http.Body request: ChatMessageDto
    )

    /**
     * Obtiene las biografías de los artistas del festival.
     * @return Lista de [BiografiaDto] con descripción, citas y discografía.
     */
    @retrofit2.http.GET("biografias")
    suspend fun getBiografias(): List<BiografiaDto>

    /**
     * Obtiene el catálogo de canciones disponibles del artista.
     * @return Lista de [CancionDto] con título, duración y URL del archivo de audio.
     */
    @retrofit2.http.GET("canciones")
    suspend fun getCanciones(): List<CancionDto>

    /**
     * Obtiene las galerías de fotos del festival.
     * @return Lista de [GaleriaDto] con sus colecciones de imágenes.
     */
    @retrofit2.http.GET("galeria")
    suspend fun getGalerias(): List<GaleriaDto>

    /**
     * Publica una nueva canción en el catálogo.
     */
    @retrofit2.http.POST("canciones")
    suspend fun createCancion(@retrofit2.http.Body request: CancionCreateDto): CancionDto

    /**
     * Publica una nueva imagen en la galería del festival.
     */
    @retrofit2.http.POST("galeria/imagen")
    suspend fun addImagenGaleria(@retrofit2.http.Body request: ImagenCreateDto): ImagenDto

    /**
     * Elimina una canción del catálogo por su ID.
     */
    @retrofit2.http.DELETE("canciones/{id}")
    suspend fun deleteCancion(@retrofit2.http.Path("id") id: String)

    /**
     * Elimina una imagen de la galería por su ID.
     */
    @retrofit2.http.DELETE("galeria/imagen/{id}")
    suspend fun deleteImagenGaleria(@retrofit2.http.Path("id") id: String)

    /**
     * Obtiene el estado actual de la transmisión en vivo (URL del stream RTSP y si está activo).
     * La Smart TV llama esto cada 5 segundos para conectarse automáticamente cuando el admin inicia el live.
     * @return [StreamStatusDto] con streamUrl e isLive.
     */
    @retrofit2.http.GET("stream/status")
    suspend fun getStreamStatus(): StreamStatusDto

    /**
     * El admin móvil publica la URL RTSP al iniciar o detener la transmisión en vivo.
     * @param body Objeto con streamUrl (ej. "rtsp://192.168.1.5:1935") e isLive (true/false).
     */
    @retrofit2.http.POST("stream/status")
    suspend fun setStreamStatus(@retrofit2.http.Body body: StreamStatusDto): StreamStatusDto

    /**
     * Obtiene la lista de todos los usuarios registrados (requiere rol ADMIN).
     * @param token Token JWT del admin en formato "Bearer <token>".
     * @return Lista de [UsuarioDto].
     */
    @retrofit2.http.GET("auth/usuarios")
    suspend fun getUsuarios(
        @retrofit2.http.Header("Authorization") token: String? = null
    ): List<UsuarioDto>

    /**
     * Registra un nuevo administrador en el sistema (requiere rol ADMIN).
     * @param token Token JWT del admin creador en formato "Bearer <token>".
     * @param request Datos del nuevo administrador.
     * @return [AuthResponseDto] con los datos generados.
     */
    @retrofit2.http.POST("auth/register-admin")
    suspend fun registerAdmin(
        @retrofit2.http.Header("Authorization") token: String? = null,
        @retrofit2.http.Body request: RegisterDto
    ): AuthResponseDto

    /**
     * Actualiza el rol de un usuario existente (requiere rol ADMIN).
     * @param token Token JWT del admin en formato "Bearer <token>".
     * @param id ID del usuario a modificar.
     * @param request DTO con el nuevo rol.
     * @return [UsuarioDto] actualizado.
     */
    @retrofit2.http.PUT("auth/usuarios/{id}/rol")
    suspend fun updateUserRole(
        @retrofit2.http.Header("Authorization") token: String? = null,
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body request: RoleUpdateDto
    ): UsuarioDto

    companion object {
        /**
         * URL base del API REST.
         * `10.0.2.2` es el alias especial del emulador Android para referirse
         * al localhost del PC host. En producción, cambiar por la URL del servidor real.
         */
        private const val BASE_URL = "http://10.0.2.2:3001/api/v1/"

        /**
         * Factory method que crea y retorna una instancia de [FestivalApiService].
         * Configura Retrofit con Gson para convertir automáticamente JSON ↔ data classes.
         *
         * @return Instancia lista para usar de [FestivalApiService].
         */
        fun create(): FestivalApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FestivalApiService::class.java)
        }
    }
}

// ─── DTOs (Data Transfer Objects) ────────────────────────────────────────────
// Los DTOs representan la estructura exacta de los datos JSON que viajan
// entre la app Android y el backend NestJS.

/**
 * Representa un evento del festival con toda su información asociada.
 *
 * @property id Identificador único del evento (ej. "EVT-001").
 * @property nombre Nombre del evento (ej. "Serenata de Gala: Mausoleo").
 * @property fechaHora Fecha y hora del evento en formato ISO 8601.
 * @property ubicacion Nombre del lugar del evento.
 * @property escenario Nombre del escenario específico (puede ser null).
 * @property estado Estado actual: "PROGRAMADO", "EN_VIVO", "FINALIZADO".
 * @property latitud Coordenada geográfica para mostrar en mapa (puede ser null).
 * @property longitud Coordenada geográfica para mostrar en mapa (puede ser null).
 * @property artistaId ID del artista principal (puede ser null).
 * @property artista Datos del artista principal (puede ser null).
 * @property transmision Datos de transmisión en vivo si está activa (puede ser null).
 */
data class EventoDto(
    val id: String,
    val nombre: String,
    val fechaHora: String,
    val ubicacion: String,
    val escenario: String?,
    val estado: String,
    val latitud: Double?,
    val longitud: Double?,
    val artistaId: String?,
    val artista: ArtistaDto?,
    val transmision: TransmisionDto? = null
) {
    /**
     * Convierte este DTO a una entidad Room para persistencia local.
     * Usado por [FestivalRepository] para guardar en caché la información de eventos.
     */
    fun toEntity(): EventoEntity {
        return EventoEntity(
            id = id,
            nombre = nombre,
            fechaHora = fechaHora,
            ubicacion = ubicacion,
            escenario = escenario,
            bannerUrl = null,
            estado = estado,
            artistaId = artistaId,
            artistaNombre = artista?.nombre,
            latitud = latitud,
            longitud = longitud,
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Artista del festival (datos mínimos embebidos en [EventoDto]).
 * @property id Identificador único del artista.
 * @property nombre Nombre completo del artista.
 */
data class ArtistaDto(
    val id: String,
    val nombre: String
)

/**
 * Payload para crear o actualizar un evento (usado solo por administradores).
 * @property nombre Nombre del nuevo evento.
 * @property fechaHora Fecha y hora en formato ISO 8601.
 * @property ubicacion Nombre del lugar.
 * @property escenario Escenario específico (opcional).
 * @property capacidad Aforo máximo del evento.
 * @property estado Estado inicial: generalmente "PROGRAMADO".
 */
data class EventoCreateDto(
    val nombre: String,
    val fechaHora: String,
    val ubicacion: String,
    val escenario: String?,
    val capacidad: Int,
    val estado: String
)

/**
 * Credenciales de inicio de sesión.
 * @property correo Correo electrónico del usuario.
 * @property contrasena Contraseña en texto plano (se envía por HTTPS).
 */
data class LoginDto(
    val correo: String,
    val contrasena: String
)

/**
 * Datos para registro de nuevo usuario.
 * @property nombre Nombre completo del usuario.
 * @property correo Correo electrónico (debe ser único).
 * @property contrasena Contraseña en texto plano (hasheada en el servidor con bcrypt).
 */
data class RegisterDto(
    val nombre: String,
    val correo: String,
    val contrasena: String
)

/**
 * Respuesta del servidor tras un login o registro exitoso.
 * @property accessToken Token JWT que debe adjuntarse en headers de peticiones protegidas.
 * @property usuario Datos del usuario autenticado.
 */
data class AuthResponseDto(
    val accessToken: String,
    val usuario: UsuarioDto
)

/**
 * Información del usuario autenticado.
 * @property id UUID del usuario en la base de datos.
 * @property nombre Nombre completo.
 * @property correo Correo electrónico.
 * @property rol Rol del usuario: "USER" o "ADMIN".
 */
data class UsuarioDto(
    val id: String,
    val nombre: String,
    val correo: String,
    val rol: String
)

/**
 * Solicitud de compra de boleto.
 * @property eventoId ID del evento para el que se compra el boleto.
 * @property categoria Tipo de boleto: "GENERAL", "VIP", "PALCO".
 * @property cantidad Número de boletos a comprar.
 * @property precioTotal Precio total en pesos mexicanos (MXN).
 * @property metodoPago Método elegido: "TARJETA", "EFECTIVO", etc.
 */
data class CompraBoletoDto(
    val eventoId: String,
    val categoria: String,
    val cantidad: Int,
    val precioTotal: Int,
    val metodoPago: String
)

/**
 * Boleto generado tras una compra exitosa.
 * @property id UUID único del boleto.
 * @property eventoId ID del evento asociado.
 * @property usuarioId ID del usuario propietario.
 * @property categoria Tipo de boleto.
 * @property precio Precio unitario.
 * @property codigoQR Código QR único para validación en entrada (UUID).
 * @property estado Estado del boleto: "ACTIVO", "USADO", "CANCELADO".
 * @property evento Datos del evento asociado (puede ser null).
 */
data class BoletoDto(
    val id: String,
    val eventoId: String,
    val usuarioId: String,
    val categoria: String,
    val precio: Double,
    val codigoQR: String,
    val estado: String,
    val evento: EventoDto?
)

/**
 * Mensaje del chat de transmisión en vivo.
 * @property id UUID del mensaje (null al enviarlo, asignado por el servidor).
 * @property eventoId ID del evento al que pertenece el mensaje (debe existir en BD).
 * @property usuarioNombre Nombre visible del remitente.
 * @property mensaje Contenido del mensaje.
 * @property esAdmin Si true, el mensaje es del administrador del festival (se resalta visualmente).
 * @property fechaEnvio Timestamp de envío (null en el request, asignado por el servidor).
 */
data class ChatMessageDto(
    val id: String? = null,
    val eventoId: String,
    val usuarioNombre: String,
    val mensaje: String,
    val esAdmin: Boolean = false,
    val fechaEnvio: String? = null
)

/**
 * Datos de una transmisión en vivo asociada a un evento.
 * @property id UUID de la transmisión.
 * @property titulo Título descriptivo de la transmisión.
 * @property streamUrl URL del stream RTSP (ej. "rtsp://10.0.2.2:1935"). Puede ser null.
 * @property estado Estado: "ACTIVA", "INACTIVA", "FINALIZADA".
 */
data class TransmisionDto(
    val id: String,
    val titulo: String,
    val streamUrl: String?,
    val estado: String
)

/**
 * Información biográfica de un artista del festival.
 * @property id UUID de la biografía.
 * @property descripcion Texto largo con la historia del artista.
 * @property citaCelebre Frase famosa del artista (puede ser null).
 * @property hitos JSON serializado con hitos históricos de la carrera.
 * @property discografia JSON serializado con lista de álbumes/canciones destacadas.
 * @property artista Datos del artista asociado (puede ser null).
 */
data class BiografiaDto(
    val id: String,
    val descripcion: String,
    val citaCelebre: String?,
    val hitos: String, // JSON String — parsear con Gson si se necesita lista
    val discografia: String, // JSON String — parsear con Gson si se necesita lista
    val artista: ArtistaDto?
)

/**
 * Canción del catálogo musical del artista.
 * @property id UUID de la canción.
 * @property titulo Título de la canción (ej. "Camino de Guanajuato").
 * @property artista Nombre del artista intérprete.
 * @property duracion Duración en segundos.
 * @property archivoUrl URL pública del archivo de audio (MP3).
 * @property genero Género musical (ej. "Ranchera"). Puede ser null.
 */
data class CancionDto(
    val id: String,
    val titulo: String,
    val artista: String,
    val duracion: Int,
    val archivoUrl: String,
    val genero: String?
)

/**
 * Imagen individual dentro de una galería.
 * @property id UUID de la imagen.
 * @property url URL pública de la imagen.
 * @property titulo Título descriptivo (puede ser null).
 * @property orden Posición en la galería para mostrar en orden.
 */
data class ImagenDto(
    val id: String,
    val url: String,
    val titulo: String?,
    val orden: Int
)

/**
 * Galería de fotos del festival con su colección de imágenes.
 * @property id UUID de la galería.
 * @property nombre Nombre de la galería (ej. "Festival 2023").
 * @property categoria Categoría temática (ej. "Oficial", "Artistas", "Público").
 * @property imagenes Lista de [ImagenDto] ordenada por [ImagenDto.orden].
 */
data class GaleriaDto(
    val id: String,
    val nombre: String,
    val categoria: String,
    val imagenes: List<ImagenDto>
)

/**
 * Estado de la transmisión en vivo compartido entre el móvil admin y la Smart TV.
 * @property streamUrl URL RTSP real del stream (ej. "rtsp://192.168.1.5:1935"). Vacía si no hay stream.
 * @property emulatorUrl URL alternativa con 10.0.2.2 para que el emulador Android TV acceda vía el host PC.
 * @property isLive true si el admin está transmitiendo en este momento.
 * @property port Puerto RTSP extraído (ej. 1935).
 */
data class StreamStatusDto(
    val streamUrl: String = "",
    val emulatorUrl: String = "",
    val isLive: Boolean = false,
    val port: Int = 1935
)

/**
 * Datos requeridos para crear o publicar una nueva canción en el catálogo.
 * @property titulo Título de la canción.
 * @property artista Nombre del artista (por defecto José Alfredo Jiménez).
 * @property duracion Duración en segundos.
 * @property archivoUrl URL pública de la canción (MP3).
 * @property genero Género musical.
 */
data class CancionCreateDto(
    val titulo: String,
    val artista: String = "José Alfredo Jiménez",
    val duracion: Int = 180,
    val archivoUrl: String = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    val genero: String = "Ranchera"
)

/**
 * Datos requeridos para agregar una nueva imagen a la galería.
 * @property url URL pública donde está alojada la imagen.
 * @property titulo Título o descripción breve de la foto.
 */
data class ImagenCreateDto(
    val url: String = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80",
    val titulo: String = "Nueva Foto"
)

/**
 * Payload para actualizar el rol de un usuario existente.
 * @property rol Nuevo rol a asignar, típicamente "USER" o "ADMIN".
 */
data class RoleUpdateDto(
    val rol: String
)

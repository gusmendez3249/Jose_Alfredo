package mx.utng.festivaltrack.shared.data.remote

import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import retrofit2.http.GET
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface FestivalApiService {
    @GET("eventos")
    suspend fun getEventos(): List<EventoDto>

    @retrofit2.http.POST("auth/login")
    suspend fun login(@retrofit2.http.Body request: LoginDto): AuthResponseDto

    @retrofit2.http.POST("auth/register")
    suspend fun register(@retrofit2.http.Body request: RegisterDto): AuthResponseDto

    @retrofit2.http.POST("eventos")
    suspend fun createEvento(@retrofit2.http.Body evento: EventoCreateDto): EventoDto

    @retrofit2.http.PUT("eventos/{id}")
    suspend fun updateEvento(@retrofit2.http.Path("id") id: String, @retrofit2.http.Body evento: EventoCreateDto): EventoDto

    @retrofit2.http.DELETE("eventos/{id}")
    suspend fun deleteEvento(@retrofit2.http.Path("id") id: String)

    @retrofit2.http.POST("boletos/comprar")
    suspend fun comprarBoleto(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: CompraBoletoDto
    ): BoletoDto

    @retrofit2.http.GET("boletos/mis-boletos")
    suspend fun getMisBoletos(
        @retrofit2.http.Header("Authorization") token: String
    ): List<BoletoDto>

    @retrofit2.http.GET("stream/chat/{eventoId}")
    suspend fun getChatMessages(
        @retrofit2.http.Path("eventoId") eventoId: String
    ): List<ChatMessageDto>

    @retrofit2.http.POST("stream/chat")
    suspend fun sendChatMessage(
        @retrofit2.http.Body request: ChatMessageDto
    )

    companion object {
        // 10.0.2.2 es el localhost de la máquina host desde el emulador de Android
        private const val BASE_URL = "http://10.0.2.2:3001/api/v1/"

        fun create(): FestivalApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FestivalApiService::class.java)
        }
    }
}

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

data class ArtistaDto(
    val id: String,
    val nombre: String
)

data class EventoCreateDto(
    val nombre: String,
    val fechaHora: String,
    val ubicacion: String,
    val escenario: String?,
    val capacidad: Int,
    val estado: String
)

data class LoginDto(
    val correo: String,
    val contrasena: String
)

data class RegisterDto(
    val nombre: String,
    val correo: String,
    val contrasena: String
)

data class AuthResponseDto(
    val accessToken: String,
    val usuario: UsuarioDto
)

data class UsuarioDto(
    val id: String,
    val nombre: String,
    val correo: String,
    val rol: String
)

data class CompraBoletoDto(
    val eventoId: String,
    val categoria: String,
    val cantidad: Int,
    val precioTotal: Int,
    val metodoPago: String
)

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

data class ChatMessageDto(
    val id: String? = null,
    val eventoId: String,
    val usuarioNombre: String,
    val mensaje: String,
    val esAdmin: Boolean = false,
    val fechaEnvio: String? = null
)

data class TransmisionDto(
    val id: String,
    val titulo: String,
    val streamUrl: String?,
    val estado: String
)

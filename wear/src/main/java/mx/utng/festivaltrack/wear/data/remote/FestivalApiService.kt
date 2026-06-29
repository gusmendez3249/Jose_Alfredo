package mx.utng.festivaltrack.wear.data.remote

import mx.utng.festivaltrack.wear.data.local.entity.EventoEntity
import retrofit2.http.GET
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface FestivalApiService {
    @GET("eventos")
    suspend fun getEventos(): List<EventoDto>

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
    val artista: ArtistaDto?
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

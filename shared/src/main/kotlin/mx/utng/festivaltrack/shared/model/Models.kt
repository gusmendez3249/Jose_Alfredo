package mx.utng.festivaltrack.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class EventoDto(
    val id: String,
    val nombre: String,
    val fechaHora: String,
    val ubicacion: String,
    val escenario: String? = null,
    val bannerUrl: String? = null,
    val estado: String,
    val artista: ArtistaDto? = null
)

@Serializable
data class ArtistaDto(
    val id: String,
    val nombre: String,
    val imagenUrl: String? = null
)

@Serializable
data class WearSyncPayload(
    val eventos: List<EventoDto>,
    val artistas: List<ArtistaDto>,
    val generadoEn: String,
    val total: SyncTotal
)

@Serializable
data class SyncTotal(
    val eventos: Int,
    val artistas: Int
)

package mx.utng.festivaltrack.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de base de datos Room que representa un Evento almacenado localmente.
 *
 * Se utiliza para mantener una caché local de los eventos y permitir
 * que la aplicación funcione en modo Offline-First.
 *
 * @property id Identificador único del evento.
 * @property nombre Nombre del evento.
 * @property fechaHora Fecha y hora del evento en formato ISO 8601.
 * @property ubicacion Nombre del lugar del evento.
 * @property escenario Nombre del escenario específico (puede ser null).
 * @property bannerUrl URL de la imagen promocional (puede ser null).
 * @property estado Estado actual: "PROGRAMADO", "EN_VIVO", "FINALIZADO".
 * @property artistaId ID del artista principal (puede ser null).
 * @property artistaNombre Nombre del artista principal (puede ser null).
 * @property latitud Coordenada geográfica para mostrar en mapa (puede ser null).
 * @property longitud Coordenada geográfica para mostrar en mapa (puede ser null).
 * @property updatedAt Timestamp de la última actualización local en milisegundos.
 */
@Entity(tableName = "eventos")
data class EventoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val fechaHora: String,
    val ubicacion: String,
    val escenario: String?,
    val bannerUrl: String?,
    val estado: String,
    val artistaId: String?,
    val artistaNombre: String?,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

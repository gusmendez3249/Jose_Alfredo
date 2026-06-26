package mx.utng.festivaltrack.wear.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val updatedAt: Long = System.currentTimeMillis()
)

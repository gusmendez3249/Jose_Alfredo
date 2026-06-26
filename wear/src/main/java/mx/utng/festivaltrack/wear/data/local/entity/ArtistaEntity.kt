package mx.utng.festivaltrack.wear.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artistas")
data class ArtistaEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val imagenUrl: String?
)

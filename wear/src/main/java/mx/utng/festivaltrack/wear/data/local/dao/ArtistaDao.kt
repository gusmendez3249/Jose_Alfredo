package mx.utng.festivaltrack.wear.data.local.dao

import androidx.room.*
import mx.utng.festivaltrack.wear.data.local.entity.ArtistaEntity

@Dao
interface ArtistaDao {
    @Upsert
    suspend fun upsertAll(artistas: List<ArtistaEntity>)

    @Query("SELECT * FROM artistas WHERE id = :id")
    suspend fun findById(id: String): ArtistaEntity?

    @Query("DELETE FROM artistas")
    suspend fun deleteAll()
}

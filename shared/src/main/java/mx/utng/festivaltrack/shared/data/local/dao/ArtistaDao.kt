package mx.utng.festivaltrack.shared.data.local.dao

import androidx.room.*
import mx.utng.festivaltrack.shared.data.local.entity.ArtistaEntity

@Dao
interface ArtistaDao {
    @Upsert
    suspend fun upsertAll(artistas: List<ArtistaEntity>)

    @Query("SELECT * FROM artistas WHERE id = :id")
    suspend fun findById(id: String): ArtistaEntity?

    @Query("DELETE FROM artistas")
    suspend fun deleteAll()
}

package mx.utng.festivaltrack.wear.data.local.dao

import androidx.room.*
import mx.utng.festivaltrack.wear.data.local.entity.EventoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {
    @Query("SELECT * FROM eventos ORDER BY fechaHora ASC")
    fun observeTodos(): Flow<List<EventoEntity>>

    @Query("SELECT * FROM eventos WHERE fechaHora >= :ahora ORDER BY fechaHora ASC LIMIT 10")
    fun observeProximos(ahora: String): Flow<List<EventoEntity>>

    @Upsert
    suspend fun upsertAll(eventos: List<EventoEntity>)

    @Query("DELETE FROM eventos")
    suspend fun deleteAll()
}

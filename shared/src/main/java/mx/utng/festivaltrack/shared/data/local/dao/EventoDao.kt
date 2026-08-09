package mx.utng.festivaltrack.shared.data.local.dao

import androidx.room.*
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {
    @Query("SELECT * FROM eventos ORDER BY fechaHora ASC")
    fun observeTodos(): Flow<List<EventoEntity>>

    @Query("SELECT * FROM eventos WHERE fechaHora >= :ahora ORDER BY fechaHora ASC LIMIT 10")
    fun observeProximos(ahora: String): Flow<List<EventoEntity>>

    @Query("SELECT * FROM eventos WHERE id = :id LIMIT 1")
    suspend fun getEventoById(id: String): EventoEntity?

    @Upsert
    suspend fun upsertAll(eventos: List<EventoEntity>)

    @Upsert
    suspend fun upsert(evento: EventoEntity)

    @Delete
    suspend fun delete(evento: EventoEntity)

    @Query("DELETE FROM eventos")
    suspend fun deleteAll()
}

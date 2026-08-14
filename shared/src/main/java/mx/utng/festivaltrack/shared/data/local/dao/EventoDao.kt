package mx.utng.festivaltrack.shared.data.local.dao

import androidx.room.*
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la entidad [EventoEntity].
 * Define las operaciones de base de datos para interactuar con la tabla de eventos.
 */
@Dao
interface EventoDao {
    /**
     * Observa todos los eventos ordenados por fecha de forma ascendente.
     * @return [Flow] que emite la lista de eventos cada vez que hay un cambio en la tabla.
     */
    @Query("SELECT * FROM eventos ORDER BY fechaHora ASC")
    fun observeTodos(): Flow<List<EventoEntity>>

    /**
     * Observa los próximos eventos a partir de la hora actual.
     * @param ahora Cadena de texto con la fecha y hora actual en formato ISO 8601.
     * @return [Flow] que emite la lista de los próximos 10 eventos.
     */
    @Query("SELECT * FROM eventos WHERE fechaHora >= :ahora ORDER BY fechaHora ASC LIMIT 10")
    fun observeProximos(ahora: String): Flow<List<EventoEntity>>

    /**
     * Obtiene un evento específico por su ID.
     * @param id El identificador único del evento.
     * @return El [EventoEntity] correspondiente, o null si no se encuentra.
     */
    @Query("SELECT * FROM eventos WHERE id = :id LIMIT 1")
    suspend fun getEventoById(id: String): EventoEntity?

    /**
     * Obtiene todos los eventos de la base de datos de una sola vez (sin observar cambios).
     * @return Lista de [EventoEntity].
     */
    @Query("SELECT * FROM eventos ORDER BY fechaHora ASC")
    suspend fun getAllOnce(): List<EventoEntity>

    /**
     * Inserta o actualiza una lista de eventos en la base de datos.
     * Si un evento con el mismo ID ya existe, lo reemplaza.
     * @param eventos La lista de [EventoEntity] a insertar o actualizar.
     */
    @Upsert
    suspend fun upsertAll(eventos: List<EventoEntity>)

    /**
     * Inserta o actualiza un único evento en la base de datos.
     * @param evento El [EventoEntity] a insertar o actualizar.
     */
    @Upsert
    suspend fun upsert(evento: EventoEntity)

    /**
     * Elimina un evento específico de la base de datos.
     * @param evento El [EventoEntity] a eliminar.
     */
    @Delete
    suspend fun delete(evento: EventoEntity)

    /**
     * Elimina todos los eventos de la tabla.
     */
    @Query("DELETE FROM eventos")
    suspend fun deleteAll()
}

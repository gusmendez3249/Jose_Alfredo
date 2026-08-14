package mx.utng.festivaltrack.shared.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.shared.data.local.dao.EventoDao
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

/**
 * Repositorio central de datos para la plataforma FestivalTrack.
 *
 * Implementa el patrón **Offline-First**: los datos se leen primero desde la
 * base de datos local (Room/SQLite), y se sincronizan con el servidor remoto
 * (NestJS + PostgreSQL) cuando hay conexión disponible.
 *
 * Este repositorio es el único punto de acceso a datos para los ViewModels.
 * La UI nunca accede directamente a [EventoDao] ni a [FestivalApiService].
 *
 * @property eventoDao DAO de Room para operaciones CRUD en SQLite local.
 * @property apiService Cliente HTTP Retrofit para comunicación con el backend.
 *
 * Diagrama de flujo de datos:
 * ```
 * ViewModel → Repository → [Room (local)] ← sync() → [NestJS API (remoto)]
 * ```
 */
class FestivalRepository(
    private val eventoDao: EventoDao,
    private val apiService: FestivalApiService = FestivalApiService.create()
) {

    
    // Obtenemos los eventos locales (Offline-First)
    fun getEventosLocales(): Flow<List<EventoEntity>> {
        return eventoDao.observeTodos()
    }

    // Obtenemos solo los próximos a partir de la hora actual
    fun getProximosEventosLocales(ahora: String): Flow<List<EventoEntity>> {
        return eventoDao.observeProximos(ahora)
    }

    // Obtenemos un evento en particular
    suspend fun getEventoById(id: String): EventoEntity? {
        return eventoDao.getEventoById(id)
    }

    // Sincronizamos con el servidor Postgres -> Room
    suspend fun syncEventos() {
        withContext(Dispatchers.IO) {
            try {
                // 1. Obtener desde el Backend NestJS
                val remoteEventos = apiService.getEventos()
                
                // 2. Convertir a entidades de Room
                val remoteEntities = remoteEventos.map { it.toEntity() }
                val remoteIds = remoteEntities.map { it.id }.toSet()
                
                // 3. Insertar / actualizar los eventos que vienen del servidor
                eventoDao.upsertAll(remoteEntities)

                // 4. Eliminar de Room SOLO los eventos que el servidor ya no tiene
                //    (soft-delete remoto = CANCELADO fue filtrado por el backend, así que
                //     cualquier ID local que no esté en remoteIds fue eliminado en servidor).
                //    Conservamos eventos locales cuyo ID empiece con letras UUID (pendientes
                //    de sincronizar) para no perder los que aún no se subieron.
                val allLocal = eventoDao.getAllOnce()
                val toDelete = allLocal.filter { local ->
                    val isStaleEvt = local.id.startsWith("EVT-") && local.id !in remoteIds
                    val isDuplicateUuid = !local.id.startsWith("EVT-") && remoteEntities.any { it.nombre == local.nombre && it.fechaHora == local.fechaHora }
                    isStaleEvt || isDuplicateUuid
                }
                toDelete.forEach { eventoDao.delete(it) }
            } catch (e: Exception) {
                e.printStackTrace()
                // Si falla (no hay internet), no hacemos nada, la app seguirá usando Room (Offline)
            }
        }
    }

    suspend fun addEvento(token: String? = null, id: String, eventoCreateDto: mx.utng.festivaltrack.shared.data.remote.EventoCreateDto) {
        withContext(Dispatchers.IO) {
            // Offline-first: Guardamos localmente inmediatamente
            val localEntity = EventoEntity(
                id = id,
                nombre = eventoCreateDto.nombre,
                fechaHora = eventoCreateDto.fechaHora,
                ubicacion = eventoCreateDto.ubicacion,
                escenario = eventoCreateDto.escenario,
                bannerUrl = null,
                estado = eventoCreateDto.estado,
                artistaId = null,
                artistaNombre = null,
                latitud = null,
                longitud = null,
                updatedAt = System.currentTimeMillis()
            )
            eventoDao.upsert(localEntity)

            try {
                // 1. Post to API con token si está presente
                val authToken = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
                val remoteEvent = apiService.createEvento(authToken, eventoCreateDto)
                // 2. Si el servidor asignó un ID distinto (ej. EVT-004 vs UUID), eliminar la entidad temporal local
                if (remoteEvent.id != id) {
                    eventoDao.delete(localEntity)
                }
                // 3. Guardar la versión oficial del servidor en Room
                eventoDao.upsert(remoteEvent.toEntity())
            } catch (e: Exception) {
                e.printStackTrace()
                // Si falla la red, ya quedó guardado localmente en Room
            }
        }
    }

    suspend fun updateEvento(token: String? = null, id: String, eventoCreateDto: mx.utng.festivaltrack.shared.data.remote.EventoCreateDto) {
        withContext(Dispatchers.IO) {
            // 1. Actualizar localmente inmediatamente
            val localEntity = EventoEntity(
                id = id,
                nombre = eventoCreateDto.nombre,
                fechaHora = eventoCreateDto.fechaHora,
                ubicacion = eventoCreateDto.ubicacion,
                escenario = eventoCreateDto.escenario,
                bannerUrl = null,
                estado = eventoCreateDto.estado,
                artistaId = null,
                artistaNombre = null,
                latitud = null,
                longitud = null,
                updatedAt = System.currentTimeMillis()
            )
            eventoDao.upsert(localEntity)

            try {
                // 2. Actualizar en API remota
                val authToken = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
                val remoteEvent = apiService.updateEvento(authToken, id, eventoCreateDto)
                eventoDao.upsert(remoteEvent.toEntity())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteEvento(token: String? = null, id: String) {
        withContext(Dispatchers.IO) {
            // 1. Eliminar localmente inmediatamente
            val localEvent = eventoDao.getEventoById(id)
            if (localEvent != null) {
                eventoDao.delete(localEvent)
            }

            try {
                // 2. Eliminar en API remota
                val authToken = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
                apiService.deleteEvento(authToken, id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

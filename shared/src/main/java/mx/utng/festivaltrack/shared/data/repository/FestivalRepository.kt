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

    
    /**
     * Obtiene todos los eventos locales como un flujo continuo.
     * Esta función permite la arquitectura Offline-First, los ViewModels
     * pueden observar los datos en tiempo real de la base local de SQLite.
     *
     * @return [Flow] que emite la lista de todos los [EventoEntity] cuando cambia.
     */
    fun getEventosLocales(): Flow<List<EventoEntity>> {
        return eventoDao.observeTodos()
    }

    /**
     * Obtiene solo los próximos eventos a partir de la hora indicada.
     *
     * @param ahora Cadena con la hora actual (ISO 8601) que sirve de límite inferior.
     * @return [Flow] que emite la lista de los próximos [EventoEntity].
     */
    fun getProximosEventosLocales(ahora: String): Flow<List<EventoEntity>> {
        return eventoDao.observeProximos(ahora)
    }

    /**
     * Obtiene un evento específico de la base de datos local por su ID.
     *
     * @param id Identificador único del evento a buscar.
     * @return [EventoEntity] encontrado o null si no existe.
     */
    suspend fun getEventoById(id: String): EventoEntity? {
        return eventoDao.getEventoById(id)
    }

    /**
     * Sincroniza la base de datos local con el servidor Postgres remoto.
     *
     * Estrategia Offline-First:
     * 1. Consulta el servidor remoto.
     * 2. Si es exitoso, actualiza las entradas locales en Room con `upsertAll`.
     * 3. Realiza la limpieza eliminando de la base de datos local aquellos registros
     *    que el servidor remoto ya no tiene (o han sido filtrados, ej. CANCELADO).
     * 4. En caso de error de red, no hace nada para que la app pueda seguir usando Room de forma normal (Offline).
     *
     * Llama al endpoint remoto `GET /api/v1/eventos`.
     */
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

    /**
     * Agrega un nuevo evento tanto en la base local como en el servidor remoto.
     *
     * Guarda la entidad primero localmente para proveer de respuesta rápida, y
     * luego intenta publicarlo en el servidor remoto a través de `POST /api/v1/eventos`.
     * Si la red falla, la entrada se mantendrá en local permitiendo reintentos.
     *
     * @param token Token de autenticación del usuario.
     * @param id ID provisional (típicamente UUID local) a sobreescribir.
     * @param eventoCreateDto Datos del evento a crear mediante DTO de creación.
     */
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

    /**
     * Actualiza un evento existente en local y remoto.
     *
     * Llama a `PUT /api/v1/eventos/{id}` si hay red, pero antes lo almacena
     * en Room para que los cambios se reflejen de inmediato en la UI.
     *
     * @param token Token de autenticación del usuario.
     * @param id ID del evento a actualizar.
     * @param eventoCreateDto Datos modificados del evento.
     */
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

    /**
     * Elimina un evento primero de la base local, e inmediatamente hace la
     * petición de borrado al servidor en `DELETE /api/v1/eventos/{id}`.
     *
     * @param token Token de autenticación del usuario.
     * @param id ID del evento que se va a eliminar.
     */
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

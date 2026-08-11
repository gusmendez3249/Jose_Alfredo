package mx.utng.festivaltrack.shared.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.shared.data.local.dao.EventoDao
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

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
                val localEntities = remoteEventos.map { it.toEntity() }
                
                // 3. Limpiar base de datos local y guardar los nuevos de la nube
                eventoDao.deleteAll()
                eventoDao.upsertAll(localEntities)
            } catch (e: Exception) {
                e.printStackTrace()
                // Si falla (no hay internet), no hacemos nada, la app seguirá usando Room (Offline)
            }
        }
    }

    suspend fun addEvento(id: String, eventoCreateDto: mx.utng.festivaltrack.shared.data.remote.EventoCreateDto) {
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
                // 1. Post to API
                val remoteEvent = apiService.createEvento(eventoCreateDto)
                // 2. Save remote version locally (with real ID if different)
                eventoDao.upsert(remoteEvent.toEntity())
            } catch (e: Exception) {
                e.printStackTrace()
                // It failed to sync, but we already saved it locally!
            }
        }
    }

    suspend fun updateEvento(id: String, eventoCreateDto: mx.utng.festivaltrack.shared.data.remote.EventoCreateDto) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Put to API
                val remoteEvent = apiService.updateEvento(id, eventoCreateDto)
                // 2. Update locally
                eventoDao.upsert(remoteEvent.toEntity())
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    suspend fun deleteEvento(id: String) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Delete from API
                apiService.deleteEvento(id)
                // 2. Delete locally
                val localEvent = eventoDao.getEventoById(id)
                if (localEvent != null) {
                    eventoDao.delete(localEvent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }
}

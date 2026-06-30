package mx.utng.festivaltrack.wear.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.wear.data.local.dao.EventoDao
import mx.utng.festivaltrack.wear.data.local.entity.EventoEntity
import mx.utng.festivaltrack.wear.data.remote.FestivalApiService

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
}

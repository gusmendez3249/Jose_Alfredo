package mx.utng.festivaltrack.wear.domain.usecase

import kotlinx.coroutines.flow.Flow
import mx.utng.festivaltrack.shared.data.local.FestivalDatabase
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import java.time.Instant

/**
 * Caso de uso para obtener la lista de los próximos eventos desde la base de datos local.
 *
 * @property db Instancia de la base de datos de Room.
 * @constructor Crea el caso de uso pasando la instancia de la base de datos.
 */
class GetProximosEventosUseCase(private val db: FestivalDatabase) {
    /**
     * Ejecuta la consulta para obtener los próximos eventos a partir de la hora actual.
     * @return [Flow] que emite la lista de eventos.
     */
    fun execute(): Flow<List<EventoEntity>> {
        val ahora = Instant.now().toString()
        return db.eventoDao().observeProximos(ahora)
    }
}

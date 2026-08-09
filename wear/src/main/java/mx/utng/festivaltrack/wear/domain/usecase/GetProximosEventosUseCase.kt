package mx.utng.festivaltrack.wear.domain.usecase

import kotlinx.coroutines.flow.Flow
import mx.utng.festivaltrack.shared.data.local.FestivalDatabase
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import java.time.Instant

class GetProximosEventosUseCase(private val db: FestivalDatabase) {
    fun execute(): Flow<List<EventoEntity>> {
        val ahora = Instant.now().toString()
        return db.eventoDao().observeProximos(ahora)
    }
}

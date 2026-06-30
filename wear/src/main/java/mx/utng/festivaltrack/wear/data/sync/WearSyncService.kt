package mx.utng.festivaltrack.wear.data.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import mx.utng.festivaltrack.shared.model.WearSyncPayload
import mx.utng.festivaltrack.wear.data.local.FestivalDatabase
import mx.utng.festivaltrack.wear.data.local.entity.ArtistaEntity
import mx.utng.festivaltrack.wear.data.local.entity.EventoEntity
import mx.utng.festivaltrack.wear.domain.usecase.ScheduleAlertasUseCase

class WearSyncService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == "/festival/sync") {

                val jsonPayload = DataMapItem
                    .fromDataItem(event.dataItem)
                    .dataMap
                    .getString("payload") ?: return@forEach

                scope.launch {
                    val payload = Json.decodeFromString<WearSyncPayload>(jsonPayload)
                    persistirPayload(payload)
                }
            }
        }
    }

    private suspend fun persistirPayload(payload: WearSyncPayload) {
        val db = FestivalDatabase.getInstance(applicationContext)

        // Limpiamos la base de datos local antes de insertar para evitar duplicados
        // o eventos fantasma que hayan cambiado de ID en el backend
        db.artistaDao().deleteAll()
        db.eventoDao().deleteAll()

        // Persistir artistas
        db.artistaDao().upsertAll(
            payload.artistas.map { dto ->
                ArtistaEntity(id = dto.id, nombre = dto.nombre, imagenUrl = dto.imagenUrl)
            }
        )

        // Persistir eventos
        db.eventoDao().upsertAll(
            payload.eventos.map { dto ->
                EventoEntity(
                    id = dto.id,
                    nombre = dto.nombre,
                    fechaHora = dto.fechaHora,
                    ubicacion = dto.ubicacion,
                    escenario = dto.escenario,
                    bannerUrl = dto.bannerUrl,
                    estado = dto.estado,
                    artistaId = dto.artista?.id,
                    artistaNombre = dto.artista?.nombre
                )
            }
        )

        // Programar alertas locales a partir de los datos descargados
        ScheduleAlertasUseCase(applicationContext, db).execute()
    }
}

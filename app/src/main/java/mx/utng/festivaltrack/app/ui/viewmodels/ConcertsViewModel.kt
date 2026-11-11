package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.ui.utils.mediaUrl
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

/** Resumen del repertorio de un concierto para su tarjeta en la pestaña Conciertos. */
sealed interface RepertoireSummary {
    object Loading : RepertoireSummary

    /** No se pudo consultar (sin conexión): la tarjeta no afirma nada sobre el repertorio. */
    object Unknown : RepertoireSummary

    data class Loaded(val songCount: Int, val covers: List<String>) : RepertoireSummary

    companion object {
        /** Cantidad de demos y hasta cuatro portadas distintas para el mosaico de la tarjeta. */
        fun from(songs: List<CancionDto>): Loaded =
            Loaded(songs.size, songs.mapNotNull { mediaUrl(it.portadaUrl) }.distinct().take(4))
    }
}

/**
 * Consulta el repertorio de cada concierto de la lista para mostrar en su tarjeta si ya tiene
 * demos (cuántas y sus portadas) o todavía no.
 */
class ConcertsViewModel : ViewModel() {
    private val api = FestivalApiService.create()

    private val _summaries = MutableStateFlow<Map<String, RepertoireSummary>>(emptyMap())
    val summaries: StateFlow<Map<String, RepertoireSummary>> = _summaries.asStateFlow()

    /** Consulta el repertorio de cada concierto; lo ya cargado se sigue viendo mientras se actualiza. */
    fun load(eventoIds: List<String>) {
        for (id in eventoIds) {
            if (_summaries.value[id] !is RepertoireSummary.Loaded) {
                _summaries.update { it + (id to RepertoireSummary.Loading) }
            }
            viewModelScope.launch {
                val summary = try {
                    RepertoireSummary.from(api.getCancionesEvento(id))
                } catch (e: Exception) {
                    RepertoireSummary.Unknown
                }
                _summaries.update { it + (id to summary) }
            }
        }
    }
}

package mx.utng.festivaltrack.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.playback.PlaybackManager
import mx.utng.festivaltrack.app.playback.QueueTrack
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

/**
 * Playlist de demos de un concierto: carga sus canciones y las manda a la cola global de reproducción
 * ([PlaybackManager]). El audio ya no depende de esta pantalla: sigue sonando al salir de ella.
 */
class EventDemosViewModel : ViewModel() {
    private val api = FestivalApiService.create()

    private val _songs = MutableStateFlow<List<CancionDto>>(emptyList())
    val songs: StateFlow<List<CancionDto>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private var eventoId: String? = null

    fun load(eventoId: String) {
        this.eventoId = eventoId
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            try {
                _songs.value = api.getCancionesEvento(eventoId)
            } catch (e: Exception) {
                _loadError.value = "No pudimos cargar las canciones. Revisa tu conexión e intenta de nuevo."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retry() {
        eventoId?.let(::load)
    }

    private fun tracks(evento: EventoEntity?): List<QueueTrack> {
        val id = eventoId ?: return emptyList()
        return _songs.value.map { QueueTrack.from(it, evento, id) }
    }

    /** Toca una canción de la lista: la inicia (con el resto del concierto en cola) o pausa/reanuda si ya es la activa. */
    fun toggle(song: CancionDto, evento: EventoEntity?, activeId: String?) {
        if (activeId == song.id) {
            PlaybackManager.playPause()
        } else {
            val index = _songs.value.indexOfFirst { it.id == song.id }
            if (index >= 0) PlaybackManager.play(tracks(evento), index)
        }
    }

    /** Botón grande de la cabecera: empieza la playlist del concierto o pausa/reanuda si ya suena. */
    fun playAll(evento: EventoEntity?, isThisConcertActive: Boolean) {
        if (_songs.value.isEmpty()) return
        if (isThisConcertActive) PlaybackManager.playPause()
        else PlaybackManager.play(tracks(evento), if (PlaybackManager.shuffle.value) _songs.value.indices.random() else 0)
    }
}

package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.ui.utils.mediaUrl
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

/**
 * Estado y reproducción de las demos de audio de un concierto.
 *
 * Usa un único [ExoPlayer], así que solo puede sonar una demo a la vez: al tocar otra
 * canción la anterior se detiene. Cada demo es un fragmento de [DEMO_MS] ms.
 */
class EventDemosViewModel(application: Application) : AndroidViewModel(application) {
    private val api = FestivalApiService.create()

    private val _songs = MutableStateFlow<List<CancionDto>>(emptyList())
    val songs: StateFlow<List<CancionDto>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    /** Canción seleccionada (sonando, en pausa o cargando). */
    private val _currentId = MutableStateFlow<String?>(null)
    val currentId: StateFlow<String?> = _currentId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private var eventoId: String? = null

    private val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(state: Int) {
                _isBuffering.value = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED) {
                    // La demo terminó: ninguna canción queda activa.
                    _currentId.value = null
                    _isPlaying.value = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackError.value = "No se pudo reproducir la demo. Revisa tu conexión e intenta de nuevo."
                _currentId.value = null
                _isPlaying.value = false
                _isBuffering.value = false
            }
        })
    }

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

    /** Toca una canción: la inicia, o pausa/reanuda si ya es la activa. */
    fun toggle(song: CancionDto) {
        _playbackError.value = null
        if (_currentId.value == song.id) {
            if (player.isPlaying) player.pause() else player.play()
            return
        }
        player.stop()
        val item = MediaItem.Builder()
            .setUri(mediaUrl(song.archivoUrl))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setEndPositionMs(DEMO_MS).build()
            )
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.playWhenReady = true
        _currentId.value = song.id
        _isBuffering.value = true
    }

    /** Detiene la reproducción (al salir de la pantalla). */
    fun stop() {
        player.stop()
        player.clearMediaItems()
        _currentId.value = null
        _isPlaying.value = false
        _isBuffering.value = false
    }

    fun dismissPlaybackError() {
        _playbackError.value = null
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    companion object {
        /** Duración de cada fragmento de demo. */
        const val DEMO_MS = 30_000L
    }
}

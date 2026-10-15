package mx.utng.festivaltrack.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.ui.utils.mediaUrl
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

/**
 * Estado y reproducción de la playlist de demos de un concierto.
 *
 * Funciona como una cola: al terminar una demo (fragmento de [DEMO_MS] ms) pasa sola a la
 * siguiente, con siguiente/anterior y modo aleatorio. Usa un único [ExoPlayer], así que solo
 * suena una demo a la vez.
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

    /** Avance de la demo actual, de 0 a 1. */
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private var eventoId: String? = null
    private var progressJob: Job? = null

    private val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgress() else progressJob?.cancel()
            }

            override fun onPlaybackStateChanged(state: Int) {
                _isBuffering.value = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED) advance()
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackError.value = "No se pudo reproducir la demo. Revisa tu conexión e intenta de nuevo."
                _currentId.value = null
                _isPlaying.value = false
                _isBuffering.value = false
                _progress.value = 0f
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

    // ─── Controles ───────────────────────────────────────────────────────────

    /** Toca una canción de la lista: la inicia, o pausa/reanuda si ya es la activa. */
    fun toggle(song: CancionDto) {
        if (_currentId.value == song.id) {
            playPause()
        } else {
            val index = _songs.value.indexOfFirst { it.id == song.id }
            if (index >= 0) playIndex(index)
        }
    }

    /** Botón grande de la cabecera: empieza la playlist o pausa/reanuda. */
    fun playAll() {
        if (_songs.value.isEmpty()) return
        if (_currentId.value != null) playPause()
        else playIndex(if (_shuffle.value) _songs.value.indices.random() else 0)
    }

    fun playPause() {
        if (_currentId.value == null) return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        advance()
    }

    /** Anterior: si ya avanzó más de 3 s reinicia la demo; si no, va a la canción previa. */
    fun previous() {
        val index = currentIndex()
        if (index < 0) return
        if (player.currentPosition > 3_000 || index == 0) {
            player.seekTo(0)
        } else {
            playIndex(index - 1)
        }
    }

    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
    }

    /** Detiene la reproducción (al salir de la pantalla). */
    fun stop() {
        progressJob?.cancel()
        player.stop()
        player.clearMediaItems()
        _currentId.value = null
        _isPlaying.value = false
        _isBuffering.value = false
        _progress.value = 0f
    }

    fun dismissPlaybackError() {
        _playbackError.value = null
    }

    // ─── Internos ────────────────────────────────────────────────────────────

    private fun currentIndex(): Int = _songs.value.indexOfFirst { it.id == _currentId.value }

    private fun playIndex(index: Int) {
        val song = _songs.value.getOrNull(index) ?: return
        _playbackError.value = null
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
        _progress.value = 0f
    }

    /** Pasa a la siguiente demo (o a una al azar en modo aleatorio); al final de la cola se detiene. */
    private fun advance() {
        val songs = _songs.value
        val index = currentIndex()
        val nextIndex = when {
            songs.isEmpty() -> -1
            _shuffle.value && songs.size > 1 -> songs.indices.filter { it != index }.random()
            index + 1 < songs.size -> index + 1
            else -> -1
        }
        if (nextIndex >= 0) playIndex(nextIndex) else stop()
    }

    private fun startProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val total = player.duration.takeIf { it > 0 } ?: DEMO_MS
                _progress.value = (player.currentPosition.toFloat() / total).coerceIn(0f, 1f)
                delay(200)
            }
        }
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

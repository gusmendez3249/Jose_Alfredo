package mx.utng.festivaltrack.app.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.ui.utils.mediaUrl
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.CancionDto

/** Canción de la cola con los datos del concierto al que pertenece (escenario y horario). */
data class QueueTrack(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val audioUrl: String,
    val durationSec: Int,
    val eventoId: String,
    val eventoNombre: String,
    val escenario: String?,
    val fechaHora: String?
) {
    companion object {
        fun from(song: CancionDto, evento: EventoEntity?, eventoId: String) = QueueTrack(
            id = song.id,
            title = song.titulo,
            artist = song.artista,
            coverUrl = mediaUrl(song.portadaUrl),
            audioUrl = mediaUrl(song.archivoUrl) ?: song.archivoUrl,
            durationSec = song.duracion,
            eventoId = eventoId,
            eventoNombre = evento?.nombre ?: "Concierto del festival",
            escenario = evento?.escenario,
            fechaHora = evento?.fechaHora
        )
    }
}

/**
 * Cola de reproducción de la app (#25), compartida por todas las pantallas.
 *
 * Habla con [PlaybackService] mediante un `MediaController`, de modo que la reproducción no depende de
 * ninguna pantalla: sigue sonando al navegar a biografía, mapa o AR, y con la app en segundo plano.
 * Cada demo es un fragmento de [DEMO_MS] ms; al terminar pasa sola a la siguiente de la cola.
 */
object PlaybackManager {
    /** Duración de cada fragmento de demo. */
    const val DEMO_MS = 30_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var appContext: Context? = null
    private var controller: MediaController? = null
    private var connecting: ListenableFuture<MediaController>? = null
    private val pendingActions = ArrayDeque<(MediaController) -> Unit>()

    private val _queue = MutableStateFlow<List<QueueTrack>>(emptyList())
    /** Canciones de la cola, en el orden de la playlist del concierto. */
    val queue: StateFlow<List<QueueTrack>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    /** Posición en [queue] de la canción activa; -1 si no hay nada en reproducción. */
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    /** Avance de la demo actual, de 0 a 1. */
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentIndex.value = controller?.currentMediaItemIndex ?: -1
            _progress.value = 0f
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlaybackStateChanged(state: Int) {
            _isBuffering.value = state == Player.STATE_BUFFERING
            if (state == Player.STATE_ENDED) clearQueue() // terminó la última demo
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffle.value = shuffleModeEnabled
        }

        override fun onPlayerError(error: PlaybackException) {
            _error.value = "No se pudo reproducir la demo. Revisa tu conexión e intenta de nuevo."
            clearQueue()
        }
    }

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        scope.launch {
            while (isActive) {
                controller?.takeIf { it.isPlaying }?.let { c ->
                    val total = c.duration.takeIf { it > 0 } ?: DEMO_MS
                    _progress.value = (c.currentPosition.toFloat() / total).coerceIn(0f, 1f)
                }
                delay(200)
            }
        }
    }

    // ─── Controles ───────────────────────────────────────────────────────────

    /** Reemplaza la cola por las canciones de un concierto y empieza a sonar desde [startIndex]. */
    fun play(tracks: List<QueueTrack>, startIndex: Int) {
        if (tracks.isEmpty()) return
        _error.value = null
        _queue.value = tracks
        _currentIndex.value = startIndex
        _progress.value = 0f
        _isBuffering.value = true
        withController { c ->
            c.setMediaItems(tracks.map(::toMediaItem), startIndex.coerceIn(0, tracks.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    fun playPause() = withController { c -> if (c.isPlaying) c.pause() else c.play() }

    fun next() = withController { c -> if (c.hasNextMediaItem()) c.seekToNextMediaItem() else clearQueue() }

    /** Anterior: si ya avanzó más de 3 s reinicia la demo; si no, va a la canción previa. */
    fun previous() = withController { c -> c.seekToPrevious() }

    /** Salta a una canción de la cola. */
    fun skipTo(index: Int) = withController { c ->
        c.seekToDefaultPosition(index)
        c.play()
    }

    fun toggleShuffle() = withController { c -> c.shuffleModeEnabled = !c.shuffleModeEnabled }

    /** Detiene la reproducción y vacía la cola. */
    fun stop() {
        clearQueue()
    }

    fun dismissError() {
        _error.value = null
    }

    // ─── Internos ────────────────────────────────────────────────────────────

    private fun clearQueue() {
        controller?.run {
            pause()
            clearMediaItems()
        }
        _queue.value = emptyList()
        _currentIndex.value = -1
        _isPlaying.value = false
        _isBuffering.value = false
        _progress.value = 0f
    }

    private fun toMediaItem(track: QueueTrack): MediaItem =
        MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.audioUrl)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setEndPositionMs(DEMO_MS).build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.eventoNombre)
                    .setArtworkUri(track.coverUrl?.let(Uri::parse))
                    .build()
            )
            .build()

    /** Ejecuta [action] cuando el controlador está conectado al servicio (lo conecta si hace falta). */
    private fun withController(action: (MediaController) -> Unit) {
        controller?.let { action(it); return }
        pendingActions.addLast(action)
        if (connecting != null) return
        val ctx = appContext ?: return
        val future = MediaController.Builder(ctx, SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java)))
            .buildAsync()
        connecting = future
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(listener)
                while (pendingActions.isNotEmpty()) pendingActions.removeFirst()(c)
            } catch (e: Exception) {
                pendingActions.clear()
                connecting = null
                _error.value = "No se pudo iniciar el reproductor."
                clearQueue()
            }
        }, ContextCompat.getMainExecutor(ctx))
    }
}

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
 * Cada demo es un fragmento de hasta [DEMO_MS] ms; al terminar pasa sola a la siguiente de la cola.
 *
 * El estado publicado se toma siempre del reproductor ([sync]), nunca se supone: así la pantalla, la
 * notificación y la pantalla de bloqueo muestran lo mismo aunque el cambio venga de fuera de la app.
 */
object PlaybackManager {
    /** Duración máxima de cada fragmento de demo. */
    const val DEMO_MS = 30_000L

    private const val TICK_MS = 100L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var appContext: Context? = null
    private var controller: MediaController? = null
    private var connecting: ListenableFuture<MediaController>? = null
    private val pendingActions = ArrayDeque<(MediaController) -> Unit>()

    /** Cola en el orden del repertorio, para restaurarla al quitar el modo aleatorio. */
    private var originalQueue: List<QueueTrack> = emptyList()
    /** Fallos seguidos; se reinicia cuando una canción logra sonar. */
    private var consecutiveErrors = 0

    private val _queue = MutableStateFlow<List<QueueTrack>>(emptyList())
    /** Canciones de la cola, en el orden exacto en que van a sonar. */
    val queue: StateFlow<List<QueueTrack>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    /** Posición en [queue] de la canción activa; -1 si no hay nada en reproducción. */
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    /** Posición dentro de la demo actual. */
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(DEMO_MS)
    /** Duración de la demo actual: [DEMO_MS] o menos si el audio es más corto. */
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    /** Avance de la demo actual, de 0 a 1. */
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) && player.playbackState == Player.STATE_ENDED) {
                // Terminó la última demo: la cola se conserva, lista para volver a escucharla.
                backToStart(player)
            }
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) && player.isPlaying) consecutiveErrors = 0
            sync(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            val c = controller ?: return
            val failed = _queue.value.getOrNull(c.currentMediaItemIndex)
            consecutiveErrors++
            val networkError = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            when (PlaybackLogic.afterError(_queue.value.size, c.currentMediaItemIndex, consecutiveErrors, networkError)) {
                PlaybackLogic.ErrorAction.WAIT_FOR_NETWORK -> {
                    consecutiveErrors = 0
                    _error.value = "Sin conexión. Cuando vuelva el internet toca reproducir para continuar."
                    c.pause() // queda en la misma canción; al reanudar se vuelve a preparar
                }
                PlaybackLogic.ErrorAction.SKIP_TO_NEXT -> {
                    _error.value = "No se pudo reproducir «${failed?.title ?: "la demo"}». Pasamos a la siguiente."
                    c.seekToNextMediaItem()
                    c.prepare()
                    c.play()
                }
                PlaybackLogic.ErrorAction.BACK_TO_START -> {
                    _error.value = "No se pudo reproducir «${failed?.title ?: "la demo"}»."
                    backToStart(c)
                    c.prepare()
                }
                PlaybackLogic.ErrorAction.STOP -> {
                    _error.value = "No se pudo reproducir la demo. Revisa tu conexión e intenta de nuevo."
                    clearQueue()
                }
            }
        }
    }

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        scope.launch {
            while (isActive) {
                controller?.takeIf { it.isPlaying }?.let(::syncPosition)
                delay(TICK_MS)
            }
        }
    }

    // ─── Controles ───────────────────────────────────────────────────────────

    /** Reemplaza la cola por las canciones de un concierto y empieza a sonar desde [startIndex]. */
    fun play(tracks: List<QueueTrack>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val start = startIndex.coerceIn(0, tracks.lastIndex)
        originalQueue = tracks
        val ordered = if (_shuffle.value) PlaybackLogic.shuffled(tracks, start) else tracks
        val index = if (_shuffle.value) 0 else start
        consecutiveErrors = 0
        _error.value = null
        _queue.value = ordered
        _currentIndex.value = index
        _positionMs.value = 0L
        _durationMs.value = PlaybackLogic.demoDurationMs(0, ordered[index].durationSec, DEMO_MS)
        _progress.value = 0f
        _isBuffering.value = true
        withController { c ->
            c.setMediaItems(ordered.map(::toMediaItem), index, 0L)
            c.prepare()
            c.play()
        }
    }

    fun playPause() = withController { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare() // tras un error
            c.play()
        }
    }

    /** Siguiente de la cola; en la última no hace nada (el botón se muestra deshabilitado). */
    fun next() = withController { c ->
        if (c.hasNextMediaItem()) {
            c.seekToNextMediaItem()
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
        }
    }

    /** Anterior: si ya avanzó más de 3 s reinicia la demo; si no, va a la canción previa. */
    fun previous() = withController { c ->
        c.seekToPrevious()
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
    }

    /** Salta a una canción de la cola y la reproduce. */
    fun skipTo(index: Int) = withController { c ->
        if (index !in 0 until c.mediaItemCount) return@withController
        c.seekToDefaultPosition(index)
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    /** Mueve la reproducción a [fraction] (0 a 1) de la demo actual. */
    fun seekTo(fraction: Float) = withController { c ->
        if (c.mediaItemCount == 0) return@withController
        c.seekTo(PlaybackLogic.seekPositionMs(fraction, _durationMs.value))
        syncPosition(c)
    }

    /**
     * Activa o quita el orden aleatorio. La canción que suena no se interrumpe: solo se reordena
     * el resto de la cola, de modo que la lista en pantalla es siempre el orden real.
     */
    fun toggleShuffle() {
        val enabled = !_shuffle.value
        _shuffle.value = enabled
        val c = controller ?: return
        val queue = _queue.value
        val index = c.currentMediaItemIndex
        val current = queue.getOrNull(index) ?: return
        if (queue.size < 2) return

        val reordered = if (enabled) PlaybackLogic.shuffled(queue, index) else originalQueue.ifEmpty { queue }
        val newIndex = reordered.indexOfFirst { it.id == current.id }
        if (newIndex < 0) return
        // Se quitan las demás canciones y se vuelven a insertar alrededor de la actual.
        if (index < c.mediaItemCount - 1) c.removeMediaItems(index + 1, c.mediaItemCount)
        if (index > 0) c.removeMediaItems(0, index)
        if (newIndex > 0) c.addMediaItems(0, reordered.subList(0, newIndex).map(::toMediaItem))
        if (newIndex < reordered.lastIndex) {
            c.addMediaItems(newIndex + 1, reordered.subList(newIndex + 1, reordered.size).map(::toMediaItem))
        }
        _queue.value = reordered
        _currentIndex.value = newIndex
    }

    /** Detiene la reproducción, vacía la cola y libera el servicio. */
    fun stop() {
        pendingActions.clear()
        clearQueue()
        controller?.release()
        controller = null
        connecting = null
    }

    fun dismissError() {
        _error.value = null
    }

    // ─── Internos ────────────────────────────────────────────────────────────

    /** Publica el estado real del reproductor. */
    private fun sync(player: Player) {
        val queue = _queue.value
        if (player.mediaItemCount == 0 || queue.isEmpty()) return
        val index = player.currentMediaItemIndex.coerceIn(0, queue.lastIndex)
        if (index != _currentIndex.value) {
            _currentIndex.value = index
            _positionMs.value = 0L
            _progress.value = 0f
        }
        _isPlaying.value = player.isPlaying
        _isBuffering.value = player.playbackState == Player.STATE_BUFFERING && player.playWhenReady
        syncPosition(player)
    }

    private fun syncPosition(player: Player) {
        val track = _queue.value.getOrNull(_currentIndex.value) ?: return
        val duration = PlaybackLogic.demoDurationMs(player.duration, track.durationSec, DEMO_MS)
        val position = player.currentPosition.coerceIn(0, duration)
        _durationMs.value = duration
        _positionMs.value = position
        _progress.value = PlaybackLogic.progress(position, duration)
    }

    /** Deja la cola en pausa al inicio (fin de la última demo). */
    private fun backToStart(player: Player) {
        player.pause()
        if (player.mediaItemCount > 0) player.seekTo(0, 0L)
    }

    private fun clearQueue() {
        controller?.run {
            stop()
            clearMediaItems()
        }
        resetState()
    }

    private fun resetState() {
        originalQueue = emptyList()
        consecutiveErrors = 0
        _queue.value = emptyList()
        _currentIndex.value = -1
        _isPlaying.value = false
        _isBuffering.value = false
        _positionMs.value = 0L
        _durationMs.value = DEMO_MS
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
                    .setSubtitle(track.artist)
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
            .setListener(object : MediaController.Listener {
                /** El servicio terminó (lo cerró el sistema o el usuario): no queda nada sonando. */
                override fun onDisconnected(controller: MediaController) {
                    if (this@PlaybackManager.controller !== controller) return
                    this@PlaybackManager.controller = null
                    connecting = null
                    pendingActions.clear()
                    resetState()
                }
            })
            .buildAsync()
        connecting = future
        future.addListener({
            if (connecting !== future) { // se pidió detener mientras conectaba
                runCatching { future.get().release() }
                return@addListener
            }
            try {
                val c = future.get()
                controller = c
                c.addListener(listener)
                while (pendingActions.isNotEmpty()) pendingActions.removeFirst()(c)
            } catch (e: Exception) {
                pendingActions.clear()
                connecting = null
                _error.value = "No se pudo iniciar el reproductor."
                resetState()
            }
        }, ContextCompat.getMainExecutor(ctx))
    }
}

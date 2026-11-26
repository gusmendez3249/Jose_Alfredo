package mx.utng.festivaltrack.tv.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

/** Lo que está sonando en la TV. */
data class EstadoMusica(
    val cola: List<CancionDto> = emptyList(),
    val indice: Int = 0,
    val sonando: Boolean = false,
    /** De dónde viene la cola: "Discografía de José Alfredo" o el nombre del concierto. */
    val origen: String = "",
    val posicionMs: Long = 0,
    val duracionMs: Long = ReproductorTv.DEMO_MS,
    val error: String? = null
) {
    val actual: CancionDto? get() = cola.getOrNull(indice)
}

/**
 * Reproductor de música de la TV, igual que el del móvil: cada canción suena como demo de hasta
 * [DEMO_MS] y al terminar pasa sola a la siguiente. Lo usan la biografía (catálogo y cita
 * célebre) y el detalle de cada concierto (su repertorio).
 */
class ReproductorTv(context: Context, private val scope: CoroutineScope) {

    companion object {
        /** Igual que en el móvil: fragmentos de 30 s. */
        const val DEMO_MS = 30_000L

        /** Ruta del servidor (`/uploads/x.mp3`) → URL completa; deja intactas las URL completas. */
        fun url(ruta: String): String = if (ruta.startsWith("/")) FestivalApiService.SERVER_URL + ruta else ruta
    }

    private val _estado = MutableStateFlow(EstadoMusica())
    val estado: StateFlow<EstadoMusica> = _estado.asStateFlow()

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _estado.value = _estado.value.copy(sonando = isPlaying)
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) siguiente(automatico = true)
                if (state == Player.STATE_READY) {
                    val total = duration.takeIf { it > 0 }?.coerceAtMost(DEMO_MS) ?: DEMO_MS
                    _estado.value = _estado.value.copy(duracionMs = total)
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                val titulo = _estado.value.actual?.titulo ?: "la canción"
                _estado.value = _estado.value.copy(error = "No se pudo reproducir «$titulo».", sonando = false)
                // Si hay más canciones, pasa a la siguiente como en el móvil.
                if (_estado.value.indice < _estado.value.cola.lastIndex) siguiente(automatico = true)
            }
        })
    }
    private var avance: Job? = null

    /** Pone [canciones] en la cola y empieza por [inicio]. Si esa misma ya suena, la pausa o reanuda. */
    fun reproducir(canciones: List<CancionDto>, inicio: Int = 0, origen: String) {
        if (canciones.isEmpty()) return
        val e = _estado.value
        val misma = e.origen == origen && e.actual?.id == canciones.getOrNull(inicio)?.id
        if (misma) { alternar(); return }
        _estado.value = EstadoMusica(cola = canciones, indice = inicio.coerceIn(0, canciones.lastIndex), origen = origen)
        cargar()
    }

    fun alternar() {
        if (_estado.value.cola.isEmpty()) return
        if (player.isPlaying) player.pause() else {
            if (player.playbackState == Player.STATE_ENDED || player.playbackState == Player.STATE_IDLE) cargar() else player.play()
        }
    }

    fun siguiente(automatico: Boolean = false) {
        val e = _estado.value
        if (e.indice < e.cola.lastIndex) {
            _estado.value = e.copy(indice = e.indice + 1, error = if (automatico) e.error else null)
            cargar()
        } else if (automatico) {
            // Terminó la última: la cola se queda lista para volver a escucharla.
            player.pause()
            player.seekTo(0)
        }
    }

    fun anterior() {
        val e = _estado.value
        if (player.currentPosition > 3_000 || e.indice == 0) player.seekTo(0)
        else { _estado.value = e.copy(indice = e.indice - 1, error = null); cargar() }
    }

    fun detener() {
        player.stop()
        avance?.cancel()
        _estado.value = EstadoMusica()
    }

    /** Pausa sin cerrar el reproductor (por ejemplo al abrir la transmisión en vivo). */
    fun pausar() { if (player.isPlaying) player.pause() }

    fun liberar() {
        avance?.cancel()
        player.release()
    }

    private fun cargar() {
        val cancion = _estado.value.actual ?: return
        val item = MediaItem.Builder()
            .setUri(Uri.parse(url(cancion.archivoUrl)))
            .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().setEndPositionMs(DEMO_MS).build())
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.play()
        _estado.value = _estado.value.copy(posicionMs = 0, duracionMs = DEMO_MS)
        avance?.cancel()
        avance = scope.launch {
            while (isActive) {
                _estado.value = _estado.value.copy(posicionMs = player.currentPosition.coerceAtLeast(0))
                delay(500)
            }
        }
    }
}

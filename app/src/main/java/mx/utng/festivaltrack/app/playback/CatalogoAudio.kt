package mx.utng.festivaltrack.app.playback

import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.app.ui.utils.mediaUrl

/**
 * Reproduce el catálogo de canciones del artista (`GET /canciones`) con el reproductor global.
 * Lo usan la tarjeta "Audio" del inicio y la discografía de la biografía.
 */
object CatalogoAudio {

    /** Identifica en la cola las canciones que vienen del catálogo (no de un concierto). */
    const val ID_CATALOGO = "catalogo"

    private val api by lazy { FestivalApiService.create() }

    /**
     * Pone el catálogo en la cola y empieza por la canción cuyo título contiene [preferida]
     * (o por la primera). Si esa canción ya está sonando, la pausa o la reanuda.
     * @return false si no hay canciones o no hay conexión.
     */
    suspend fun reproducir(preferida: String? = null): Boolean {
        val actual = PlaybackManager.queue.value.getOrNull(PlaybackManager.currentIndex.value)
        if (actual != null && actual.eventoId == ID_CATALOGO &&
            (preferida == null || actual.title.contains(preferida, ignoreCase = true))
        ) {
            PlaybackManager.playPause()
            return true
        }
        val canciones = runCatching { api.getCanciones() }.getOrNull().orEmpty()
        if (canciones.isEmpty()) return false
        val inicio = indicePreferido(canciones, preferida)
        PlaybackManager.play(canciones.map(::pista), inicio)
        return true
    }

    /** true mientras suena una canción del catálogo cuyo título contiene [titulo] (o cualquiera). */
    fun sonando(titulo: String?, queue: List<QueueTrack>, index: Int, isPlaying: Boolean): Boolean {
        val actual = queue.getOrNull(index) ?: return false
        return isPlaying && actual.eventoId == ID_CATALOGO &&
            (titulo == null || actual.title.contains(titulo, ignoreCase = true))
    }

    fun indicePreferido(canciones: List<CancionDto>, preferida: String?): Int =
        preferida?.let { p -> canciones.indexOfFirst { it.titulo.contains(p, ignoreCase = true) } }
            ?.takeIf { it >= 0 } ?: 0

    private fun pista(c: CancionDto) = QueueTrack(
        id = c.id,
        title = c.titulo,
        artist = c.artista,
        coverUrl = mediaUrl(c.portadaUrl),
        audioUrl = mediaUrl(c.archivoUrl) ?: c.archivoUrl,
        durationSec = c.duracion,
        eventoId = ID_CATALOGO,
        eventoNombre = "Discografía de José Alfredo",
        escenario = null,
        fechaHora = null
    )
}

package mx.utng.festivaltrack.app.playback

import mx.utng.festivaltrack.app.auto.SalidaCalculator
import mx.utng.festivaltrack.app.auto.TextosAuto
import mx.utng.festivaltrack.app.ui.utils.mediaUrl
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.EventoDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import java.time.Instant

/**
 * Carpeta de música que se puede explorar desde el auto: la discografía o el repertorio de un concierto.
 *
 * @property id "discografia" o "evento:<id del evento>".
 * @property subtitulo "Catálogo del festival" o "Mañana 21:30 · Mausoleo".
 */
data class CarpetaMusical(
    val id: String,
    val titulo: String,
    val subtitulo: String,
    val pistas: List<QueueTrack>
)

/**
 * Música del festival organizada para el auto y para el reproductor del sistema.
 *
 * La usan la pantalla "Música" del modo automóvil y [PlaybackService] (que la publica para el
 * reproductor del auto y los controles del volante). Las canciones son las mismas de la app:
 * el catálogo (`GET /canciones`) y el repertorio de cada próximo concierto
 * (`GET /canciones/evento/{id}`).
 */
object BibliotecaMusical {

    const val ID_DISCOGRAFIA = "discografia"
    private const val PREFIJO_EVENTO = "evento:"

    /** Conciertos con repertorio que se ofrecen en el auto, del más próximo al más lejano. */
    private const val MAX_CONCIERTOS = 4

    private val api by lazy { FestivalApiService.create() }

    /** Última biblioteca cargada, para resolver al instante una canción elegida desde el volante. */
    @Volatile
    private var cache: List<CarpetaMusical> = emptyList()

    /** Discografía primero y luego los próximos conciertos que tienen canciones. */
    suspend fun carpetas(ahora: Instant = Instant.now()): List<CarpetaMusical> {
        val discografia = runCatching { api.getCanciones() }.getOrNull().orEmpty()
            .filter(::esValida)
            .map { pistaDeCatalogo(it) }
        val eventos = runCatching { api.getEventos() }.getOrNull().orEmpty()
        val conciertos = proximos(eventos, ahora).mapNotNull { evento ->
            val canciones = runCatching { api.getCancionesEvento(evento.id) }.getOrNull().orEmpty().filter(::esValida)
            if (canciones.isEmpty()) return@mapNotNull null
            val inicio = SalidaCalculator.parsearFecha(evento.fechaHora) ?: return@mapNotNull null
            CarpetaMusical(
                id = PREFIJO_EVENTO + evento.id,
                titulo = evento.nombre,
                subtitulo = "${TextosAuto.cuandoCorto(inicio, ahora)} · ${TextosAuto.nombreCorto(evento.escenario ?: evento.ubicacion)}",
                pistas = canciones.map { pistaDeEvento(it, evento) }
            )
        }.take(MAX_CONCIERTOS)

        val resultado = buildList {
            if (discografia.isNotEmpty()) {
                add(CarpetaMusical(ID_DISCOGRAFIA, "Discografía de José Alfredo", "Catálogo del festival", discografia))
            }
            addAll(conciertos)
        }
        if (resultado.isNotEmpty()) cache = resultado
        return resultado
    }

    /** Carpeta ya cargada (o se carga) por su id. */
    suspend fun carpeta(id: String): CarpetaMusical? =
        cache.firstOrNull { it.id == id } ?: carpetas().firstOrNull { it.id == id }

    /** Id de canción con su carpeta, para que el reproductor del auto sepa qué lista continuar. */
    fun idPista(carpetaId: String, cancionId: String) = "$carpetaId/$cancionId"

    /** Carpeta a la que pertenece un id de [idPista]; null si no es de la biblioteca. */
    fun carpetaDe(idPista: String): String? = idPista.substringBeforeLast('/', "").ifEmpty { null }

    fun textoCanciones(n: Int) = if (n == 1) "1 canción" else "$n canciones"

    /** Descarta canciones sin archivo de audio: no hay nada que reproducir. */
    fun esValida(cancion: CancionDto): Boolean = cancion.archivoUrl.isNotBlank()

    /** Eventos publicados que aún no empiezan, del más próximo al más lejano. */
    fun proximos(eventos: List<EventoDto>, ahora: Instant): List<EventoDto> = eventos
        .filter { it.estado != "FINALIZADO" && it.estado != "CANCELADO" }
        .mapNotNull { e -> SalidaCalculator.parsearFecha(e.fechaHora)?.takeIf { it.isAfter(ahora) }?.let { e to it } }
        .sortedBy { it.second }
        .map { it.first }

    private fun pistaDeCatalogo(c: CancionDto) = QueueTrack(
        id = idPista(ID_DISCOGRAFIA, c.id),
        title = c.titulo,
        artist = c.artista,
        coverUrl = mediaUrl(c.portadaUrl),
        audioUrl = mediaUrl(c.archivoUrl) ?: c.archivoUrl,
        durationSec = c.duracion,
        eventoId = CatalogoAudio.ID_CATALOGO,
        eventoNombre = "Discografía de José Alfredo",
        escenario = null,
        fechaHora = null
    )

    private fun pistaDeEvento(c: CancionDto, evento: EventoDto) = QueueTrack(
        id = idPista(PREFIJO_EVENTO + evento.id, c.id),
        title = c.titulo,
        artist = c.artista,
        coverUrl = mediaUrl(c.portadaUrl),
        audioUrl = mediaUrl(c.archivoUrl) ?: c.archivoUrl,
        durationSec = c.duracion,
        eventoId = evento.id,
        eventoNombre = evento.nombre,
        escenario = evento.escenario,
        fechaHora = evento.fechaHora
    )
}

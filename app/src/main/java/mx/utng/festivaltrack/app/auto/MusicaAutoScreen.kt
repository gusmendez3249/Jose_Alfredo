package mx.utng.festivaltrack.app.auto

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.R
import mx.utng.festivaltrack.app.playback.BibliotecaMusical
import mx.utng.festivaltrack.app.playback.CarpetaMusical
import mx.utng.festivaltrack.app.playback.PlaybackManager
import mx.utng.festivaltrack.app.playback.QueueTrack

/**
 * Lo que suena ahora, para las pantallas de música del auto.
 * @property pista Canción activa; null si no hay nada en la cola.
 */
data class EstadoAudioAuto(
    val pista: QueueTrack?,
    val sonando: Boolean,
    val haySiguiente: Boolean,
    val cargando: Boolean = false
)

/**
 * Base de las pantallas de música: sigue al reproductor global ([PlaybackManager]) y ofrece
 * los controles ⏯ y ⏭ arriba a la derecha, al alcance del conductor.
 */
abstract class PantallaMusicaAuto(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    protected var audio = EstadoAudioAuto(null, false, false)
        private set

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        PlaybackManager.conectar()
        lifecycleScope.launch {
            combine(
                PlaybackManager.queue, PlaybackManager.currentIndex, PlaybackManager.isPlaying, PlaybackManager.isBuffering
            ) { cola, i, sonando, cargando ->
                EstadoAudioAuto(cola.getOrNull(i), sonando, i in 0 until cola.lastIndex, cargando)
            }.collect { nuevo ->
                if (nuevo != audio) {
                    audio = nuevo
                    invalidate()
                }
            }
        }
        lifecycleScope.launch {
            PlaybackManager.error.filterNotNull().collect { mensaje ->
                CarToast.makeText(carContext, mensaje, CarToast.LENGTH_LONG).show()
                PlaybackManager.dismissError()
            }
        }
    }

    /** Botones ⏯ y ⏭ (con ícono: el auto admite un solo botón con texto); null si no hay cola. */
    protected fun controles(): ActionStrip? {
        if (audio.pista == null) return null
        val acciones = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(AccionesAuto.icono(carContext, if (audio.sonando || audio.cargando) R.drawable.ic_auto_pausar else R.drawable.ic_auto_reproducir))
                    .setOnClickListener { PlaybackManager.playPause() }
                    .build()
            )
        if (audio.haySiguiente) {
            acciones.addAction(
                Action.Builder()
                    .setIcon(AccionesAuto.icono(carContext, R.drawable.ic_auto_siguiente))
                    .setOnClickListener { PlaybackManager.next() }
                    .build()
            )
        }
        return acciones.build()
    }

    /** Portada de la canción (si tiene) o la nota dorada de FestivalTrack. */
    protected fun portadaDe(pista: QueueTrack?): CarIcon =
        PortadasAuto.obtener(pista?.coverUrl) { invalidate() }?.let(PortadasAuto::icono)
            ?: AccionesAuto.iconoDorado(carContext, R.drawable.ic_auto_musica)

    /** Abre el reproductor del auto (en Automotive); en Android Auto se queda en la lista. */
    protected fun abrirReproductor() {
        ReproductorAuto.abrir(carContext)
    }

    /** "▶ Sonando: El Rey", "Cargando: El Rey…" o "⏸ En pausa: El Rey". */
    protected fun textoSonando(pista: QueueTrack): String = when {
        audio.sonando -> "▶ Sonando: ${pista.title}"
        audio.cargando -> "Cargando: ${pista.title}…"
        else -> "⏸ En pausa: ${pista.title}"
    }
}

/**
 * Música del festival en el auto: lo que suena ahora y las carpetas (discografía y repertorio
 * de los próximos conciertos). Son las mismas canciones de la app y del reproductor del auto.
 */
class MusicaAutoScreen(carContext: CarContext) : PantallaMusicaAuto(carContext) {

    private var carpetas: List<CarpetaMusical> = emptyList()
    private var cargando = true
    private var error: String? = null

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        cargar()
    }

    private fun cargar() {
        cargando = true
        error = null
        invalidate()
        lifecycleScope.launch {
            carpetas = BibliotecaMusical.carpetas()
            if (carpetas.isEmpty()) error = "No se pudo cargar la música. Revisa la conexión."
            cargando = false
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val plantilla = ListTemplate.Builder()
            .setTitle("Música del festival")
            .setHeaderAction(Action.BACK)
        if (cargando) return plantilla.setLoading(true).build()
        controles()?.let(plantilla::setActionStrip)

        val lista = ItemList.Builder()
        val mensajeError = error
        val limite = LimitesAuto.filas(carContext)
        audio.pista?.let { pista ->
            lista.addItem(
                Row.Builder()
                    .setTitle(textoSonando(pista))
                    .addText("${pista.eventoNombre} · abrir reproductor")
                    .setImage(portadaDe(pista), Row.IMAGE_TYPE_LARGE)
                    .setOnClickListener { abrirReproductor() }
                    .build()
            )
        }
        if (mensajeError != null) {
            if (audio.pista == null) lista.setNoItemsMessage(mensajeError)
        } else {
            val disponibles = limite - (if (audio.pista != null) 1 else 0)
            carpetas.take(disponibles).forEach { carpeta ->
                lista.addItem(
                    Row.Builder()
                        .setTitle(carpeta.titulo)
                        .addText("${carpeta.subtitulo} · ${BibliotecaMusical.textoCanciones(carpeta.pistas.size)}")
                        .setImage(
                            AccionesAuto.iconoDorado(
                                carContext,
                                if (carpeta.id == BibliotecaMusical.ID_DISCOGRAFIA) R.drawable.ic_auto_musica else R.drawable.ic_auto_concierto
                            )
                        )
                        .setBrowsable(true)
                        .setOnClickListener { screenManager.push(CarpetaAutoScreen(carContext, carpeta)) }
                        .build()
                )
            }
        }
        return plantilla.setSingleList(lista.build()).build()
    }
}

/** Canciones de una carpeta; al tocar una suena desde ahí y sigue con las demás de la lista. */
class CarpetaAutoScreen(
    carContext: CarContext,
    private val carpeta: CarpetaMusical
) : PantallaMusicaAuto(carContext) {

    /** Pone la canción (y el resto de la lista después) y lleva al reproductor del auto. */
    private fun tocar(indice: Int) {
        val pista = carpeta.pistas[indice]
        if (audio.pista?.id != pista.id) PlaybackManager.play(carpeta.pistas, indice)
        abrirReproductor()
    }

    override fun onGetTemplate(): Template {
        val plantilla = ListTemplate.Builder()
            .setTitle(carpeta.titulo)
            .setHeaderAction(Action.BACK)
        controles()?.let(plantilla::setActionStrip)

        val lista = ItemList.Builder()
        if (carpeta.pistas.isEmpty()) lista.setNoItemsMessage("Este concierto aún no tiene canciones")
        carpeta.pistas.take(LimitesAuto.filas(carContext)).forEachIndexed { i, pista ->
            val esActual = audio.pista?.id == pista.id
            lista.addItem(
                Row.Builder()
                    .setTitle(if (esActual) textoSonando(pista) else pista.title)
                    .addText("${pista.artist} · ${textoDuracion(pista)}")
                    .setImage(portadaDe(pista))
                    .setOnClickListener { tocar(i) }
                    .build()
            )
        }
        return plantilla.setSingleList(lista.build()).build()
    }

    /** Las canciones se escuchan como demo de hasta 30 s (o menos si el audio es más corto). */
    private fun textoDuracion(pista: QueueTrack): String {
        val maximo = (PlaybackManager.DEMO_MS / 1000).toInt()
        val segundos = if (pista.durationSec in 1 until maximo) pista.durationSec else maximo
        return "demo de $segundos s"
    }
}

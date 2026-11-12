package mx.utng.festivaltrack.app.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.app.playback.PlaybackManager
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import retrofit2.HttpException

/**
 * Pantalla 10 del modo automóvil: Home.
 *
 * Android Auto y Android Automotive OS solo permiten plantillas, por lo que las tres tarjetas del
 * diseño (audio en curso, ruta activa y resumen AR) se arman como filas de una [ListTemplate],
 * junto con los accesos a "Elige tu destino" y a la alerta de salida. Se recarga cada vez que el
 * Home vuelve a verse, para reflejar la ruta o los sitios que el usuario haya cambiado.
 */
class InicioAutoScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val api = FestivalApiService.create()
    private val tokenManager = TokenManager(carContext)

    private var ruta: RutaActiva? = null
    private var textoAr: String = "Cargando…"

    private var audio: String = "Sin audio en curso"
    private var hayAudio = false

    init {
        lifecycle.addObserver(this)
    }

    /** La tarjeta de audio sigue al reproductor en segundo plano (#31) mientras el Home existe. */
    override fun onCreate(owner: LifecycleOwner) {
        lifecycleScope.launch {
            combine(PlaybackManager.queue, PlaybackManager.currentIndex, PlaybackManager.isPlaying) { cola, i, sonando ->
                val pista = cola.getOrNull(i)
                hayAudio = pista != null
                audio = when {
                    pista == null -> "Sin audio en curso"
                    sonando -> "▶ ${pista.title} · ${pista.eventoNombre}"
                    else -> "⏸ ${pista.title} · ${pista.eventoNombre}"
                }
            }.collect { invalidate() }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        ruta = RutaActivaStore(carContext).obtener()
        cargarResumenAr()
        invalidate()
    }

    /** `GET /ar/historial` requiere la sesión del usuario iniciada en el teléfono. */
    private fun cargarResumenAr() {
        val token = tokenManager.getToken()
        if (token == null) {
            textoAr = "Inicia sesión en FestivalTrack para ver tus sitios"
            return
        }
        lifecycleScope.launch {
            textoAr = try {
                val visitas = withContext(Dispatchers.IO) { api.getHistorialSitios("Bearer $token") }
                ResumenArCalculator.texto(ResumenArCalculator.deHoy(visitas))
            } catch (e: HttpException) {
                if (e.code() == 401) "Inicia sesión de nuevo en FestivalTrack" else "No se pudo cargar el historial"
            } catch (e: Exception) {
                "No se pudo cargar el historial"
            }
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val tarjetas = ItemList.Builder()
            .addItem(filaAudio())
            .addItem(filaRuta())
            .addItem(
                Row.Builder()
                    .setTitle("Elige tu destino")
                    .addText("Escenarios y sitios del festival con distancia y tiempo")
                    .setBrowsable(true)
                    .setOnClickListener { screenManager.push(DestinosAutoScreen(carContext)) }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Resumen AR")
                    .addText(textoAr)
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Alerta de salida y próximos eventos")
                    .addText("Cuándo salir para llegar a tiempo al siguiente concierto")
                    .setBrowsable(true)
                    .setOnClickListener { screenManager.push(AlertaSalidaScreen(carContext)) }
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("FestivalTrack")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(tarjetas)
            .build()
    }

    /** Demo que suena en el reproductor en segundo plano; al tocarla se pausa o se reanuda. */
    private fun filaAudio(): Row {
        val fila = Row.Builder()
            .setTitle("Audio en curso")
            .addText(audio)
        if (hayAudio) fila.setOnClickListener { PlaybackManager.playPause() }
        return fila.build()
    }

    /** Ruta elegida en el teléfono o en el auto (pantalla 7); al tocarla abre la pantalla 8. */
    private fun filaRuta(): Row {
        val activa = ruta
            ?: return Row.Builder()
                .setTitle("Ruta activa")
                .addText("Elige tu destino aquí abajo o en el teléfono: Mapa → ícono del auto")
                .build()
        return Row.Builder()
            .setTitle("Ruta activa: ${activa.nombre}")
            .addText("Toca para ver la ruta e iniciar la navegación")
            .setBrowsable(true)
            .setOnClickListener { screenManager.push(NavegacionAutoScreen(carContext, activa)) }
            .build()
    }
}

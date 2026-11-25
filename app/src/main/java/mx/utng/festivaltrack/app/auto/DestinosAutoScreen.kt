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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService

/**
 * Pantalla 7 desde el auto: elige tu destino sin usar el teléfono.
 *
 * En Android Automotive OS la app corre en la pantalla del auto y no hay teléfono donde elegir la
 * ruta, así que esta lista reutiliza [DestinosCalculator] (la misma lógica de la pantalla 7 del
 * teléfono). Al tocar un destino se guarda como ruta activa y se abre la pantalla 8 de navegación.
 */
class DestinosAutoScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val api = FestivalApiService.create()

    private var destinos: List<Destino> = emptyList()
    private var cargando = true
    private var error: String? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        lifecycleScope.launch {
            try {
                val (eventos, sitios) = withContext(Dispatchers.IO) {
                    val e = async { api.getEventos() }
                    val s = async { runCatching { api.getSitiosAr() }.getOrDefault(emptyList()) }
                    e.await() to s.await()
                }
                val (lat, lon) = UbicacionAuto.obtener(carContext) ?: UbicacionAuto.CENTRO_DOLORES
                destinos = DestinosCalculator.construir(eventos, sitios, lat, lon)
            } catch (e: Exception) {
                error = "No se pudieron cargar los destinos"
            } finally {
                cargando = false
                invalidate()
            }
        }
    }

    private fun elegir(destino: Destino) {
        val ruta = RutaActiva(destino.nombre, destino.latitud, destino.longitud)
        RutaActivaStore(carContext).guardar(ruta)
        screenManager.push(NavegacionAutoScreen(carContext, ruta))
    }

    override fun onGetTemplate(): Template {
        val plantilla = ListTemplate.Builder()
            .setTitle("Elige tu destino")
            .setHeaderAction(Action.BACK)
        if (cargando) return plantilla.setLoading(true).build()

        val lista = ItemList.Builder()
        val mensajeError = error
        when {
            mensajeError != null -> lista.setNoItemsMessage(mensajeError)
            destinos.isEmpty() -> lista.setNoItemsMessage("No hay destinos disponibles")
            else -> destinos.take(MAX_FILAS).forEach { destino ->
                val distancia = DestinosCalculator.textoDistancia(destino.distanciaKm)
                lista.addItem(
                    Row.Builder()
                        .setTitle(if (destino.esDestacado) "★ ${destino.nombre}" else destino.nombre)
                        .addText(destino.subtitulo)
                        .addText("$distancia · ${NavegacionCalculator.textoTiempo(destino.trasladoMin)}")
                        .setBrowsable(true)
                        .setOnClickListener { elegir(destino) }
                        .build()
                )
            }
        }
        return plantilla.setSingleList(lista.build()).build()
    }

    private companion object {
        /** Las plantillas de lista del auto admiten un número limitado de filas. */
        const val MAX_FILAS = 6
    }
}

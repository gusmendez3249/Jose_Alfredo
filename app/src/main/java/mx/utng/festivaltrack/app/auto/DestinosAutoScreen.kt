package mx.utng.festivaltrack.app.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.app.R
import mx.utng.festivaltrack.app.auto.mapa.IconosMapa
import mx.utng.festivaltrack.app.auto.mapa.MapaAutoRenderer
import mx.utng.festivaltrack.app.auto.mapa.MarcadorMapa
import mx.utng.festivaltrack.app.auto.mapa.SuperficieAuto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import java.time.Instant

/**
 * Pantalla 7 desde el auto: elige tu destino sin usar el teléfono.
 *
 * En Android Automotive OS la app corre en la pantalla del auto y no hay teléfono donde elegir la
 * ruta, así que esta lista reutiliza [DestinosCalculator] (la misma lógica de la pantalla 7 del
 * teléfono). Los lugares se ven numerados en el mapa de FestivalTrack y en la lista con el mismo
 * número (el próximo concierto en dorado). Al tocar un destino se guarda como ruta activa y se
 * abre la pantalla 8 con la ruta en el mapa.
 */
class DestinosAutoScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val api = FestivalApiService.create()
    private val mapa = MapaAutoRenderer()
    private val conMapa = carContext.carAppApiLevel >= 7

    private var destinos: List<Destino> = emptyList()
    private var cargando = true
    private var error: String? = null
    private var ubicacionAproximada = true

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) = cargar()

    override fun onStart(owner: LifecycleOwner) {
        if (conMapa) SuperficieAuto.activar(carContext, mapa)
    }

    override fun onStop(owner: LifecycleOwner) = SuperficieAuto.desactivar(mapa)

    override fun onDestroy(owner: LifecycleOwner) = mapa.liberar()

    private fun cargar() {
        cargando = true
        error = null
        invalidate()
        lifecycleScope.launch {
            try {
                val (eventos, sitios) = withContext(Dispatchers.IO) {
                    val e = async { api.getEventos() }
                    val s = async { runCatching { api.getSitiosAr() }.getOrDefault(emptyList()) }
                    e.await() to s.await()
                }
                UbicacionAuto.enDosPasos(carContext) { ubicacion ->
                    ubicacionAproximada = ubicacion == null
                    val (lat, lon) = ubicacion ?: UbicacionAuto.CENTRO_DOLORES
                    destinos = DestinosCalculator.construir(eventos, sitios, lat, lon)
                    ubicacion?.let { mapa.ponerUbicacion(it.first, it.second) }
                    mostrarEnMapa()
                    cargando = false
                    invalidate()
                }
            } catch (e: Exception) {
                error = "No se pudieron cargar los destinos. Revisa la conexión y toca Actualizar."
                cargando = false
                invalidate()
            }
        }
    }

    /** Destinos que caben en la lista, con su número (1, 2, 3…). */
    private fun visibles(): List<Destino> = destinos.take(LimitesAuto.filas(carContext))

    private fun mostrarEnMapa() {
        mapa.mostrar(
            marcadores = visibles().mapIndexed { i, d -> MarcadorMapa(d.latitud, d.longitud, "${i + 1}", d.esDestacado) }
        )
        mapa.encuadrarTodo()
    }

    private fun elegir(destino: Destino) {
        val ruta = destino.comoRuta()
        RutaActivaStore(carContext).guardar(ruta)
        screenManager.push(NavegacionAutoScreen(carContext, ruta))
    }

    override fun onGetTemplate(): Template {
        val plantilla = ListTemplate.Builder()
            .setTitle("Elige tu destino")
            .setHeaderAction(Action.BACK)
        if (cargando) {
            plantilla.setLoading(true)
        } else {
            val lista = ItemList.Builder()
            val mensajeError = error
            when {
                mensajeError != null -> lista.setNoItemsMessage(mensajeError)
                destinos.isEmpty() -> lista.setNoItemsMessage("No hay destinos disponibles")
                else -> visibles().forEachIndexed { i, destino -> lista.addItem(fila(i, destino)) }
            }
            plantilla.setSingleList(lista.build())
        }
        if (!conMapa) {
            if (!cargando) plantilla.setActionStrip(acciones())
            return plantilla.build()
        }
        val mapaTemplate = MapWithContentTemplate.Builder()
            .setContentTemplate(plantilla.build())
            .setMapController(
                MapController.Builder()
                    .setMapActionStrip(
                        ActionStrip.Builder()
                            .addAction(AccionesAuto.boton(carContext, R.drawable.ic_auto_encuadrar) { mapa.encuadrarTodo() })
                            .addAction(AccionesAuto.boton(carContext, R.drawable.ic_auto_acercar) { mapa.acercar() })
                            .addAction(AccionesAuto.boton(carContext, R.drawable.ic_auto_alejar) { mapa.alejar() })
                            .build()
                    )
                    .build()
            )
        if (!cargando) mapaTemplate.setActionStrip(acciones())
        return mapaTemplate.build()
    }

    /** Fila corta: nombre, distancia y tiempo, y el concierto solo si hay uno ahí. */
    private fun fila(indice: Int, destino: Destino): Row {
        val fila = Row.Builder()
            .setTitle(if (conMapa) TextosAuto.nombreCorto(destino.nombre) else destino.nombre)
            .setImage(IconosMapa.numero("${indice + 1}", destino.esDestacado))
            .addText(TextosAuto.trayecto(destino.distanciaKm, destino.trasladoMin, ubicacionAproximada, corto = conMapa))
            .setBrowsable(true)
            .setOnClickListener { elegir(destino) }
        val inicio = destino.eventoFechaHora?.let(SalidaCalculator::parsearFecha)
        if (inicio != null) {
            // Junto al mapa solo cabe cuándo es; sin mapa, el concierto completo.
            fila.addText(if (conMapa) TextosAuto.cuandoCorto(inicio, Instant.now()) else destino.subtitulo)
        }
        return fila.build()
    }

    /** "Actualizar" siempre; "Usar mi ubicación" si las distancias salen del centro por falta de permiso. */
    private fun acciones(): ActionStrip {
        val acciones = ActionStrip.Builder()
        if (ubicacionAproximada && !UbicacionAuto.tienePermiso(carContext)) {
            acciones.addAction(UbicacionAuto.accionPermiso(carContext) { cargar() })
        }
        acciones.addAction(AccionesAuto.actualizar(carContext) { cargar() })
        return acciones.build()
    }
}

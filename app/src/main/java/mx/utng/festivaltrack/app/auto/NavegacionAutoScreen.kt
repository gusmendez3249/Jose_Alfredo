package mx.utng.festivaltrack.app.auto

import android.speech.tts.TextToSpeech
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.R
import mx.utng.festivaltrack.app.auto.mapa.MapaAutoRenderer
import mx.utng.festivaltrack.app.auto.mapa.RutaEnAuto
import mx.utng.festivaltrack.app.auto.mapa.SuperficieAuto
import mx.utng.festivaltrack.app.ui.utils.Ruta
import java.time.Duration
import java.time.Instant
import java.util.Locale

/**
 * Pantalla 8 del modo automóvil: la ruta hacia el destino en el mapa de FestivalTrack.
 *
 * El mapa (dibujado por la app con [MapaAutoRenderer]) muestra la ruta por calles en dorado, el
 * destino y la posición del auto en vivo; a la izquierda, un panel corto con distancia, tiempo y,
 * si la ruta es para un concierto, a qué hora salir. "Seguirme" acerca el mapa y lo mueve con el
 * auto; al llegar avisa "¡Ya llegaste!". Si el conductor quiere indicaciones por voz vuelta por
 * vuelta, el botón secundario abre la ruta en la app de navegación del auto.
 *
 * En autos con una versión vieja de Android Auto (sin plantillas de mapa) se muestra solo el panel.
 */
class NavegacionAutoScreen(
    carContext: CarContext,
    private val ruta: RutaActiva
) : Screen(carContext), DefaultLifecycleObserver {

    private val mapa = MapaAutoRenderer()
    private val conMapa = carContext.carAppApiLevel >= 7

    private var resumen: ResumenNavegacion? = null
    private var trazo: Ruta? = null
    private var posicion: Pair<Double, Double>? = null
    private var ubicacionAproximada = true
    private var llegado = false
    private var calculoRuta: Job? = null
    private var ultimoRecalculo = 0L
    private var detenerSeguimiento: () -> Unit = {}

    private var tts: TextToSpeech? = null

    init {
        lifecycle.addObserver(this)
        mapa.alCambiarSeguimiento = { invalidate() }
    }

    override fun onCreate(owner: LifecycleOwner) {
        tts = TextToSpeech(carContext) { estado ->
            if (estado == TextToSpeech.SUCCESS) tts?.language = Locale("es", "MX")
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (conMapa) SuperficieAuto.activar(carContext, mapa)
        mapa.mostrar(destino = ruta.latitud to ruta.longitud)
        lifecycleScope.launch {
            UbicacionAuto.enDosPasos(carContext) { ubicacion -> actualizarPosicion(ubicacion, recalcularRuta = true) }
        }
        detenerSeguimiento = UbicacionAuto.seguir(carContext) {
            actualizarPosicion(it.latitude to it.longitude, recalcularRuta = false)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        SuperficieAuto.desactivar(mapa)
        detenerSeguimiento()
        detenerSeguimiento = {}
    }

    override fun onDestroy(owner: LifecycleOwner) {
        tts?.shutdown()
        tts = null
        mapa.liberar()
    }

    private fun actualizarPosicion(ubicacion: Pair<Double, Double>?, recalcularRuta: Boolean) {
        val antes = textos()
        ubicacionAproximada = ubicacion == null
        val origen = ubicacion ?: UbicacionAuto.CENTRO_DOLORES
        posicion = ubicacion
        ubicacion?.let { mapa.ponerUbicacion(it.first, it.second) }

        resumen = NavegacionCalculator.resumen(ruta, origen.first, origen.second)
        revisarLlegada()

        val actual = trazo
        val fueraDeRuta = actual != null && ubicacion != null &&
            RutaEnAuto.distanciaARutaM(origen.first, origen.second, actual.puntos) > FUERA_DE_RUTA_M
        if (!llegado && (recalcularRuta || actual == null || fueraDeRuta)) pedirRuta(origen, encuadrar = actual == null)
        if (textos() != antes) invalidate()
    }

    /** Pide la ruta por calles; si el auto se sale de ella se vuelve a pedir (como máximo cada 15 s). */
    private fun pedirRuta(origen: Pair<Double, Double>, encuadrar: Boolean) {
        val ahora = System.currentTimeMillis()
        if (!encuadrar && ahora - ultimoRecalculo < RECALCULO_MIN_MS) return
        ultimoRecalculo = ahora
        calculoRuta?.cancel()
        calculoRuta = lifecycleScope.launch {
            val nueva = RutaEnAuto.calcular(origen.first, origen.second, ruta.latitud, ruta.longitud)
            trazo = nueva
            mapa.mostrar(ruta = nueva.puntos)
            if (encuadrar) mapa.encuadrarTodo()
            invalidate()
        }
    }

    private fun revisarLlegada() {
        val datos = resumen ?: return
        if (llegado || ubicacionAproximada || datos.distanciaKm * 1000 > LLEGADA_M) return
        llegado = true
        calculoRuta?.cancel()
        mapa.mostrar(ruta = emptyList()) // ya no hay camino que recorrer: solo el destino y el auto
        tts?.speak("Llegaste a ${ruta.nombre}.", TextToSpeech.QUEUE_FLUSH, null, "llegada")
    }

    /** Distancia y tiempo por calles; mientras llega la ruta, el cálculo en línea recta. */
    private fun distanciaYTiempo(datos: ResumenNavegacion): String {
        val porCalles = trazo?.takeIf { it.porCalles }
        val pos = posicion
        if (porCalles == null || pos == null) {
            return TextosAuto.trayecto(datos.distanciaKm, datos.trasladoMin, ubicacionAproximada, corto = conMapa)
        }
        val restanteM = RutaEnAuto.restanteM(pos.first, pos.second, porCalles.puntos)
        return TextosAuto.trayecto(restanteM / 1000.0, RutaEnAuto.minutosEnAuto(restanteM.toInt()), corto = conMapa)
    }

    /** Lo que muestra el panel; sirve para redibujar solo cuando algo cambió. */
    private fun textos(): List<String> {
        val datos = resumen ?: return emptyList()
        return listOf(distanciaYTiempo(datos), llegado.toString(), ubicacionAproximada.toString())
    }

    override fun onGetTemplate(): Template {
        val datos = resumen
        val pane = Pane.Builder()
        if (datos == null) pane.setLoading(true) else llenarPanel(pane, datos)
        val panel = PaneTemplate.Builder(pane.build())
            .setTitle(if (conMapa) TextosAuto.nombreCorto(ruta.nombre) else ruta.nombre)
            .setHeaderAction(Action.BACK)
            .build()
        if (!conMapa) return panel

        val centrar = if (mapa.siguiendo) {
            AccionesAuto.boton(carContext, R.drawable.ic_auto_encuadrar) { mapa.encuadrarTodo() }
        } else {
            AccionesAuto.boton(carContext, R.drawable.ic_auto_centrar) { mapa.seguirAuto() }
        }
        return MapWithContentTemplate.Builder()
            .setContentTemplate(panel)
            .setMapController(
                MapController.Builder()
                    .setMapActionStrip(
                        ActionStrip.Builder()
                            .addAction(centrar)
                            .addAction(AccionesAuto.boton(carContext, R.drawable.ic_auto_acercar) { mapa.acercar() })
                            .addAction(AccionesAuto.boton(carContext, R.drawable.ic_auto_alejar) { mapa.alejar() })
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun llenarPanel(pane: Pane.Builder, datos: ResumenNavegacion) {
        val ahora = Instant.now()
        val concierto = NavegacionCalculator.concierto(ruta, datos, ahora)

        if (llegado) {
            pane.addRow(
                Row.Builder()
                    .setTitle("¡Ya llegaste!")
                    .addText(ruta.nombre)
                    .setImage(AccionesAuto.iconoDorado(carContext, R.drawable.ic_auto_destino))
                    .build()
            )
        } else {
            val detalle = when {
                ubicacionAproximada -> "Desde el centro"
                concierto == null || TextosAuto.dia(concierto.inicio, ahora) == "hoy" ->
                    "Llegas a las ${TextosAuto.hora(llegada(datos, ahora))} hrs"
                else -> "Por calles"
            }
            pane.addRow(
                Row.Builder()
                    .setTitle(distanciaYTiempo(datos))
                    .addText(detalle)
                    .setImage(AccionesAuto.iconoDorado(carContext, R.drawable.ic_auto_ruta))
                    .build()
            )
        }
        concierto?.let {
            pane.addRow(
                Row.Builder()
                    .setTitle(it.nombre)
                    .addText(TextosAuto.cuandoCorto(it.inicio, ahora))
                    .apply { if (!llegado) addText(it.textoSalida) } // ya en el lugar no hay que salir
                    .setImage(AccionesAuto.iconoDorado(carContext, R.drawable.ic_auto_concierto))
                    .build()
            )
        }

        if (conMapa && !llegado) {
            val seguir = Action.Builder()
                .setTitle(if (mapa.siguiendo) "Ver ruta completa" else "Seguirme")
                .setBackgroundColor(AccionesAuto.DORADO_BOTON)
                .setOnClickListener {
                    if (mapa.siguiendo) {
                        mapa.encuadrarTodo()
                    } else {
                        mapa.seguirAuto()
                        tts?.speak(NavegacionCalculator.textoVoz(datos), TextToSpeech.QUEUE_FLUSH, null, "navegacion")
                    }
                    invalidate()
                }
            if (carContext.carAppApiLevel >= 4) seguir.setFlags(Action.FLAG_PRIMARY)
            pane.addAction(seguir.build())
        }
        // Opcional: indicaciones por voz vuelta por vuelta en la app de navegación del auto.
        pane.addAction(
            Action.Builder()
                .setTitle(if (conMapa) "Voz en Maps" else "Iniciar navegación")
                .setOnClickListener { UbicacionAuto.navegar(carContext, datos.uriNavegacion) }
                .build()
        )
    }

    /** Hora de llegada saliendo ahora, con el tiempo por calles si ya se conoce. */
    private fun llegada(datos: ResumenNavegacion, ahora: Instant): Instant {
        val minutos = trazo?.takeIf { it.porCalles }?.minutos ?: datos.trasladoMin
        return ahora.plus(Duration.ofMinutes(minutos.toLong()))
    }

    private companion object {
        /** A menos de esto del destino se considera que el auto llegó. */
        const val LLEGADA_M = 40.0

        /** Si el auto se aleja más de esto de la ruta, se recalcula. */
        const val FUERA_DE_RUTA_M = 60.0
        const val RECALCULO_MIN_MS = 15_000L
    }
}

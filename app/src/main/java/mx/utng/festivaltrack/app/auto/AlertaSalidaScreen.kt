package mx.utng.festivaltrack.app.auto

import android.Manifest
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pantalla 9 del modo automóvil: alerta de salida y próximos eventos.
 *
 * Calcula con [SalidaCalculator] cuándo conviene salir hacia cada evento desde la ubicación
 * actual, lo muestra en una lista y lee en voz alta el aviso cuando ya es hora de salir.
 * Al tocar un evento se abre la navegación hacia él en la app de mapas del auto.
 */
class AlertaSalidaScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val api = FestivalApiService.create()
    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    private var salidas: List<SalidaEvento> = emptyList()
    private var cargando = true
    private var error: String? = null
    private var ubicacionAproximada = true
    private var avisoLeido = false

    private var tts: TextToSpeech? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        tts = TextToSpeech(carContext) { estado ->
            if (estado == TextToSpeech.SUCCESS) tts?.language = Locale("es", "MX")
        }
        cargar()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        tts?.shutdown()
        tts = null
    }

    private fun cargar() {
        cargando = true
        error = null
        invalidate()
        lifecycleScope.launch {
            try {
                val eventos = withContext(Dispatchers.IO) { api.getEventos() }
                val (lat, lon) = obtenerUbicacion()
                salidas = SalidaCalculator.calcular(eventos, lat, lon)
                avisarSiEsHora()
            } catch (e: Exception) {
                error = "No se pudieron cargar los eventos"
            } finally {
                cargando = false
                invalidate()
            }
        }
    }

    /** Ubicación del teléfono; si no hay permiso o señal, se usa el centro de Dolores Hidalgo. */
    private suspend fun obtenerUbicacion(): Pair<Double, Double> {
        val ubicacion = UbicacionAuto.obtener(carContext)
        ubicacionAproximada = ubicacion == null
        return ubicacion ?: UbicacionAuto.CENTRO_DOLORES
    }

    private fun tienePermisoUbicacion(): Boolean = UbicacionAuto.tienePermiso(carContext)

    /** Lee el aviso una sola vez por pantalla, para no distraer al conductor. */
    private fun avisarSiEsHora() {
        val siguiente = salidas.firstOrNull() ?: return
        if (siguiente.minutosParaSalir > 0 || avisoLeido) return
        avisoLeido = true
        val mensaje = "Sal ahora para llegar a ${siguiente.evento.nombre}. " +
            "El traslado es de ${siguiente.trasladoMin} minutos."
        tts?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "alerta-salida")
        CarToast.makeText(carContext, "Sal ahora: ${siguiente.evento.nombre}", CarToast.LENGTH_LONG).show()
    }

    /** Abre la ruta hacia el evento en la app de navegación del auto (Google Maps, Waze…). */
    private fun navegarA(salida: SalidaEvento) {
        val lat = salida.evento.latitud
        val lon = salida.evento.longitud
        val destino = if (lat != null && lon != null) {
            NavegacionCalculator.uriNavegacion(lat, lon)
        } else {
            "geo:0,0?q=${Uri.encode(salida.evento.ubicacion)}"
        }
        UbicacionAuto.navegar(carContext, destino)
    }

    override fun onGetTemplate(): Template {
        val plantilla = ListTemplate.Builder()
            .setTitle("Alerta de salida")
            .setHeaderAction(Action.BACK)
            .setActionStrip(accionesSuperiores())

        if (cargando) return plantilla.setLoading(true).build()

        val lista = ItemList.Builder()
        val mensajeError = error
        when {
            mensajeError != null -> lista.setNoItemsMessage(mensajeError)
            salidas.isEmpty() -> lista.setNoItemsMessage("No hay eventos próximos")
            else -> salidas.take(MAX_FILAS).forEach { lista.addItem(filaDe(it)) }
        }
        return plantilla.setSingleList(lista.build()).build()
    }

    private fun filaDe(salida: SalidaEvento): Row {
        val evento = salida.evento
        val lugar = evento.escenario ?: evento.ubicacion
        val traslado = if (salida.distanciaKm != null) {
            "%.1f km · %d min".format(Locale("es", "MX"), salida.distanciaKm, salida.trasladoMin)
        } else {
            "Sin ubicación registrada"
        }
        return Row.Builder()
            .setTitle("${SalidaCalculator.textoSalida(salida.minutosParaSalir)} · ${evento.nombre}")
            .addText("$lugar · ${formatoHora.format(salida.inicio)} hrs")
            .addText(traslado)
            .setOnClickListener { navegarA(salida) }
            .build()
    }

    private fun accionesSuperiores(): ActionStrip {
        val acciones = ActionStrip.Builder()
            .addAction(Action.Builder().setTitle("Actualizar").setOnClickListener { cargar() }.build())
        if (ubicacionAproximada && !tienePermisoUbicacion()) {
            acciones.addAction(
                Action.Builder()
                    .setTitle("Usar mi ubicación")
                    .setOnClickListener {
                        carContext.requestPermissions(
                            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        ) { concedidos, _ -> if (concedidos.isNotEmpty()) cargar() }
                    }
                    .build()
            )
        }
        return acciones.build()
    }

    private companion object {
        /** Límite de filas que Android Auto muestra sin distraer al conductor. */
        const val MAX_FILAS = 6
    }
}

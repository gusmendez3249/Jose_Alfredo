package mx.utng.festivaltrack.app.auto

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.app.R
import mx.utng.festivaltrack.shared.data.remote.EventoDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import java.time.Instant
import java.util.Locale

/**
 * Pantalla 9 del modo automóvil: alerta de salida y próximos eventos.
 *
 * Calcula con [SalidaCalculator] cuándo conviene salir hacia cada evento desde la ubicación
 * actual, lo muestra en una lista y lee en voz alta el aviso cuando ya es hora de salir.
 * La cuenta se actualiza sola cada minuto mientras la pantalla está a la vista. Al tocar un
 * evento se guarda como ruta activa y se abre la pantalla de navegación hacia él.
 */
class AlertaSalidaScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val api = FestivalApiService.create()

    private var eventos: List<EventoDto> = emptyList()
    private var origen: Pair<Double, Double> = UbicacionAuto.CENTRO_DOLORES
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
        // Cuenta regresiva: se recalcula cada minuto sin volver a pedir los eventos al servidor.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(INTERVALO_MS)
                    if (!cargando && error == null) recalcular()
                }
            }
        }
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
                eventos = withContext(Dispatchers.IO) { api.getEventos() }
                UbicacionAuto.enDosPasos(carContext) { ubicacion ->
                    ubicacionAproximada = ubicacion == null
                    origen = ubicacion ?: UbicacionAuto.CENTRO_DOLORES
                    salidas = SalidaCalculator.calcular(eventos, origen.first, origen.second)
                    cargando = false
                    invalidate()
                }
                avisarSiEsHora()
            } catch (e: Exception) {
                error = "No se pudieron cargar los eventos. Revisa la conexión y toca Actualizar."
                cargando = false
                invalidate()
            }
        }
    }

    /** Recalcula con los eventos ya cargados; solo redibuja si cambió algún texto. */
    private fun recalcular() {
        val ahora = Instant.now()
        val antes = salidas.take(LimitesAuto.filas(carContext)).map { SalidaCalculator.textoSalida(it, ahora) }
        salidas = SalidaCalculator.calcular(eventos, origen.first, origen.second, ahora)
        val despues = salidas.take(LimitesAuto.filas(carContext)).map { SalidaCalculator.textoSalida(it, ahora) }
        avisarSiEsHora()
        if (antes != despues) invalidate()
    }

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

    /** Guarda el evento como ruta activa y abre la pantalla de navegación hacia él. */
    private fun abrir(salida: SalidaEvento) {
        val evento = salida.evento
        val lat = evento.latitud
        val lon = evento.longitud
        if (lat == null || lon == null) {
            // Sin coordenadas no hay distancia que mostrar: se busca la dirección en la app de mapas.
            UbicacionAuto.navegar(carContext, "geo:0,0?q=${Uri.encode(evento.ubicacion)}")
            return
        }
        val ruta = RutaActiva(evento.escenario ?: evento.ubicacion, lat, lon, evento.nombre, evento.fechaHora)
        RutaActivaStore(carContext).guardar(ruta)
        screenManager.push(NavegacionAutoScreen(carContext, ruta))
    }

    override fun onGetTemplate(): Template {
        val plantilla = ListTemplate.Builder()
            .setTitle("Alerta de salida")
            .setHeaderAction(Action.BACK)

        if (cargando) return plantilla.setLoading(true).build()
        plantilla.setActionStrip(accionesSuperiores())

        val ahora = Instant.now()
        val lista = ItemList.Builder()
        val mensajeError = error
        when {
            mensajeError != null -> lista.setNoItemsMessage(mensajeError)
            salidas.isEmpty() -> lista.setNoItemsMessage("No hay eventos próximos")
            else -> salidas.take(LimitesAuto.filas(carContext)).forEach { lista.addItem(filaDe(it, ahora)) }
        }
        return plantilla.setSingleList(lista.build()).build()
    }

    private fun filaDe(salida: SalidaEvento, ahora: Instant): Row {
        val evento = salida.evento
        val lugar = evento.escenario ?: evento.ubicacion
        val traslado = salida.distanciaKm?.let { TextosAuto.trayecto(it, salida.trasladoMin, ubicacionAproximada) }
            ?: "Sin ubicación registrada"
        return Row.Builder()
            .setTitle(evento.nombre)
            .setImage(AccionesAuto.iconoDorado(carContext, R.drawable.ic_auto_concierto))
            .addText("${SalidaCalculator.textoSalida(salida, ahora)} · ${TextosAuto.inicia(salida.inicio, salida.horaSalida, ahora)}")
            .addText("${TextosAuto.nombreCorto(lugar)} · $traslado")
            .setBrowsable(true)
            .setOnClickListener { abrir(salida) }
            .build()
    }

    private fun accionesSuperiores(): ActionStrip {
        val acciones = ActionStrip.Builder()
        if (ubicacionAproximada && !UbicacionAuto.tienePermiso(carContext)) {
            acciones.addAction(UbicacionAuto.accionPermiso(carContext) { cargar() })
        }
        acciones.addAction(AccionesAuto.actualizar(carContext) { cargar() })
        return acciones.build()
    }

    private companion object {
        const val INTERVALO_MS = 60_000L
    }
}

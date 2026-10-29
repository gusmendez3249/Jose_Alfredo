package mx.utng.festivaltrack.app.auto

import android.speech.tts.TextToSpeech
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pantalla 8 del modo automóvil: navegación hacia la ruta activa.
 *
 * Muestra el destino elegido en el teléfono (pantalla 7) con su distancia, tiempo estimado
 * y hora de llegada desde la ubicación actual. "Iniciar navegación" envía la ruta a la app
 * de navegación del auto (Google Maps, Waze…), que da las indicaciones giro a giro por voz.
 */
class NavegacionAutoScreen(
    carContext: CarContext,
    private val ruta: RutaActiva
) : Screen(carContext), DefaultLifecycleObserver {

    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    private var resumen: ResumenNavegacion? = null
    private var ubicacionAproximada = true

    private var tts: TextToSpeech? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        tts = TextToSpeech(carContext) { estado ->
            if (estado == TextToSpeech.SUCCESS) tts?.language = Locale("es", "MX")
        }
        lifecycleScope.launch {
            val ubicacion = UbicacionAuto.obtener(carContext)
            ubicacionAproximada = ubicacion == null
            val (lat, lon) = ubicacion ?: UbicacionAuto.CENTRO_DOLORES
            resumen = NavegacionCalculator.resumen(ruta, lat, lon)
            invalidate()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        tts?.shutdown()
        tts = null
    }

    /** Avisa por voz el destino y la hora de llegada, y abre la ruta en la app de mapas. */
    private fun iniciarNavegacion(resumen: ResumenNavegacion) {
        tts?.speak(NavegacionCalculator.textoVoz(resumen), TextToSpeech.QUEUE_FLUSH, null, "navegacion")
        UbicacionAuto.navegar(carContext, resumen.uriNavegacion)
    }

    override fun onGetTemplate(): Template {
        val datos = resumen
        val pane = Pane.Builder()

        if (datos == null) {
            pane.setLoading(true)
        } else {
            val desde = if (ubicacionAproximada) "Desde el centro de Dolores Hidalgo" else "Desde tu ubicación"
            pane.addRow(
                Row.Builder()
                    .setTitle("${DestinosCalculator.textoDistancia(datos.distanciaKm)} · ${NavegacionCalculator.textoTiempo(datos.trasladoMin)}")
                    .addText(desde)
                    .build()
            )
            pane.addRow(
                Row.Builder()
                    .setTitle("Llegada estimada: ${formatoHora.format(datos.llegada)} hrs")
                    .addText("Si sales ahora")
                    .build()
            )
            pane.addRow(
                Row.Builder()
                    .setTitle("Indicaciones giro a giro")
                    .addText("Las dicta tu app de navegación al iniciar")
                    .build()
            )

            val iniciar = Action.Builder()
                .setTitle("Iniciar navegación")
                .setOnClickListener { iniciarNavegacion(datos) }
            if (carContext.carAppApiLevel >= 4) iniciar.setFlags(Action.FLAG_PRIMARY)
            pane.addAction(iniciar.build())
        }

        return PaneTemplate.Builder(pane.build())
            .setTitle(ruta.nombre)
            .setHeaderAction(Action.BACK)
            .build()
    }
}

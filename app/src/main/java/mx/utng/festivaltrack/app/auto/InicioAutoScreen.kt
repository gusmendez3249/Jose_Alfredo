package mx.utng.festivaltrack.app.auto

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.app.R
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import retrofit2.HttpException
import java.time.Instant

/**
 * Pantalla 10 del modo automóvil: Home.
 *
 * Android Auto y Android Automotive OS solo permiten plantillas, por lo que las tarjetas del diseño
 * se arman como filas de una [ListTemplate], en el orden en que las usa un conductor:
 * 1. Próximo concierto con la hora de salida (abre la alerta de salida).
 * 2. Ruta activa, si ya se eligió un destino.
 * 3. Elige tu destino.
 * 4. Música del festival, con lo que suena ahora (y ⏯/⏭ arriba a la derecha).
 * 5. Resumen AR, solo si hay sesión iniciada (en un auto con Android Automotive no la hay).
 *
 * Se recarga cada vez que el Home vuelve a verse, para reflejar la ruta o los sitios que cambiaron.
 */
class InicioAutoScreen(carContext: CarContext) : PantallaMusicaAuto(carContext) {

    private val api = FestivalApiService.create()
    private val tokenManager = TokenManager(carContext)

    private var ruta: RutaActiva? = null
    private var proxima: SalidaEvento? = null
    private var textoProxima: String = "Cargando…"
    private var textoAr: String? = null

    override fun onStart(owner: LifecycleOwner) {
        ruta = RutaActivaStore(carContext).obtener()
        cargarProximo()
        cargarResumenAr()
        invalidate()
    }

    /** Próximo concierto y cuándo salir hacia él desde la ubicación actual. */
    private fun cargarProximo() {
        lifecycleScope.launch {
            try {
                val eventos = withContext(Dispatchers.IO) { api.getEventos() }
                UbicacionAuto.enDosPasos(carContext) { ubicacion ->
                    val (lat, lon) = ubicacion ?: UbicacionAuto.CENTRO_DOLORES
                    proxima = SalidaCalculator.calcular(eventos, lat, lon).firstOrNull()
                    textoProxima = proxima?.let { "${it.evento.nombre} · ${TextosAuto.cuando(it.inicio, Instant.now())}" }
                        ?: "No hay conciertos próximos"
                    invalidate()
                }
            } catch (e: Exception) {
                proxima = null
                textoProxima = "Sin conexión con FestivalTrack"
                invalidate()
            }
        }
    }

    /** `GET /ar/historial` requiere la sesión del usuario; sin sesión la fila no se muestra. */
    private fun cargarResumenAr() {
        val token = tokenManager.getToken()
        if (token == null) {
            textoAr = null
            return
        }
        lifecycleScope.launch {
            textoAr = try {
                val visitas = withContext(Dispatchers.IO) { api.getHistorialSitios("Bearer $token") }
                ResumenArCalculator.texto(ResumenArCalculator.deHoy(visitas, zona = TextosAuto.ZONA))
            } catch (e: HttpException) {
                if (e.code() == 401) "Inicia sesión de nuevo en FestivalTrack" else "No se pudo cargar el historial"
            } catch (e: Exception) {
                "No se pudo cargar el historial"
            }
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val filas = ItemList.Builder()
            .addItem(filaProximo())
        ruta?.let { filas.addItem(filaRuta(it)) }
        filas.addItem(
            Row.Builder()
                .setTitle("Elige tu destino")
                .addText("Lugares del festival en el mapa")
                .setImage(AccionesAuto.iconoDorado(carContext, R.drawable.ic_auto_destino))
                .setBrowsable(true)
                .setOnClickListener { screenManager.push(DestinosAutoScreen(carContext)) }
                .build()
        )
        filas.addItem(filaMusica())
        textoAr?.let {
            filas.addItem(
                Row.Builder()
                    .setTitle("Resumen AR")
                    .addText(it)
                    .setImage(AccionesAuto.iconoDorado(carContext, R.drawable.ic_auto_ar))
                    .build()
            )
        }

        val plantilla = ListTemplate.Builder()
            .setTitle("FestivalTrack")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(filas.build())
        controles()?.let(plantilla::setActionStrip)
        return plantilla.build()
    }

    /** Próximo concierto: nombre y, debajo, a qué hora salir (abre la alerta de salida). */
    private fun filaProximo(): Row {
        val siguiente = proxima
        val fila = Row.Builder()
            .setImage(AccionesAuto.iconoDorado(carContext, R.drawable.ic_auto_concierto))
            .setBrowsable(true)
            .setOnClickListener { screenManager.push(AlertaSalidaScreen(carContext)) }
        if (siguiente == null) {
            fila.setTitle("Próximo concierto").addText(textoProxima)
        } else {
            val ahora = Instant.now()
            fila.setTitle(siguiente.evento.nombre)
                .addText("${SalidaCalculator.textoSalida(siguiente, ahora)} · ${TextosAuto.inicia(siguiente.inicio, siguiente.horaSalida, ahora)}")
        }
        return fila.build()
    }

    /** Ruta elegida en el teléfono o en el auto; al tocarla abre el mapa con la ruta. */
    private fun filaRuta(activa: RutaActiva): Row =
        Row.Builder()
            .setTitle("Ruta a ${activa.nombre}")
            .addText(activa.eventoNombre?.let { "Para $it · ver en el mapa" } ?: "Ver en el mapa")
            .setImage(AccionesAuto.iconoDorado(carContext, R.drawable.ic_auto_ruta))
            .setBrowsable(true)
            .setOnClickListener { screenManager.push(NavegacionAutoScreen(carContext, activa)) }
            .build()

    private fun filaMusica(): Row {
        val pista = audio.pista
        return Row.Builder()
            .setTitle("Música del festival")
            .addText(pista?.let { textoSonando(it) } ?: "Discografía y repertorio de los conciertos")
            .setImage(portadaDe(pista))
            .setBrowsable(true)
            .setOnClickListener { screenManager.push(MusicaAutoScreen(carContext)) }
            .build()
    }
}

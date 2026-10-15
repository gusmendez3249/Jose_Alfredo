package mx.utng.festivaltrack.app.auto

import mx.utng.festivaltrack.shared.data.remote.EventoDto
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Evento próximo con el cálculo de cuándo conviene salir hacia él.
 *
 * @property distanciaKm Distancia en línea recta al evento; null si el evento no tiene coordenadas.
 * @property trasladoMin Tiempo estimado de traslado en automóvil.
 * @property minutosParaSalir Minutos que faltan para salir; 0 o negativo significa "sal ya".
 */
data class SalidaEvento(
    val evento: EventoDto,
    val inicio: Instant,
    val distanciaKm: Double?,
    val trasladoMin: Int,
    val minutosParaSalir: Long
)

/**
 * Calcula la hora de salida hacia cada evento: inicio del evento − traslado − margen.
 *
 * El traslado se estima con la distancia en línea recta, un factor de ruta y una velocidad
 * promedio de ciudad, sin depender de un servicio de rutas externo.
 */
object SalidaCalculator {

    /** Velocidad promedio en las calles de Dolores Hidalgo durante el festival. */
    const val VELOCIDAD_PROMEDIO_KMH = 30.0

    /** Las calles no van en línea recta: la ruta real es ~30 % más larga. */
    const val FACTOR_RUTA = 1.3

    /** Tiempo extra para estacionarse y llegar al escenario. */
    const val MARGEN_MIN = 10L

    private const val RADIO_TIERRA_KM = 6371.0

    /** Distancia entre dos coordenadas con la fórmula de Haversine. */
    fun distanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * RADIO_TIERRA_KM * asin(sqrt(a))
    }

    /** Minutos de traslado en automóvil para una distancia en línea recta (mínimo 1). */
    fun trasladoMin(distanciaKm: Double): Int =
        ceil(distanciaKm * FACTOR_RUTA / VELOCIDAD_PROMEDIO_KMH * 60).toInt().coerceAtLeast(1)

    /**
     * Devuelve los eventos que aún no empiezan, ordenados por hora de inicio,
     * con su traslado y los minutos que faltan para salir desde [origenLat], [origenLon].
     */
    fun calcular(
        eventos: List<EventoDto>,
        origenLat: Double,
        origenLon: Double,
        ahora: Instant = Instant.now()
    ): List<SalidaEvento> = eventos
        .filter { it.estado != "FINALIZADO" && it.estado != "CANCELADO" }
        .mapNotNull { evento ->
            val inicio = parsearFecha(evento.fechaHora) ?: return@mapNotNull null
            if (inicio.isBefore(ahora)) return@mapNotNull null

            val lat = evento.latitud
            val lon = evento.longitud
            val distancia = if (lat != null && lon != null) distanciaKm(origenLat, origenLon, lat, lon) else null
            val traslado = distancia?.let { trasladoMin(it) } ?: 0
            val faltan = Duration.between(ahora, inicio).toMinutes() - traslado - MARGEN_MIN

            SalidaEvento(evento, inicio, distancia, traslado, faltan)
        }
        .sortedBy { it.inicio }

    /** Texto corto para la pantalla del auto: "Sal ya", "Sal en 25 min", "Sal en 1 h 20 min". */
    fun textoSalida(minutosParaSalir: Long): String = when {
        minutosParaSalir <= 0 -> "Sal ya"
        minutosParaSalir < 60 -> "Sal en $minutosParaSalir min"
        else -> {
            val horas = minutosParaSalir / 60
            val minutos = minutosParaSalir % 60
            if (minutos == 0L) "Sal en $horas h" else "Sal en $horas h $minutos min"
        }
    }

    /** El backend envía ISO 8601 (`2026-11-20T21:00:00.000Z`); se acepta también con desfase horario. */
    private fun parsearFecha(fecha: String): Instant? =
        runCatching { Instant.parse(fecha) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(fecha).toInstant() }.getOrNull()
}

package mx.utng.festivaltrack.wear.presentation.util

import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Reglas de la agenda del reloj, iguales a las de la app del teléfono:
 * horas en Ciudad de México, se ocultan los conciertos que ya terminaron
 * (4 h después de empezar), los cancelados y los borradores.
 */
object AgendaWear {

    val ZONA: ZoneId = ZoneId.of("America/Mexico_City")
    private val ES = Locale("es", "MX")
    private const val DURACION_HORAS = 4L
    private val NO_VISIBLES = setOf("FINALIZADO", "CANCELADO", "BORRADOR")

    fun inicio(evento: EventoEntity): Instant? = runCatching { Instant.parse(evento.fechaHora) }.getOrNull()

    /** Conciertos que todavía no terminan, del más próximo al más lejano. */
    fun vigentes(eventos: List<EventoEntity>, ahora: Instant = Instant.now()): List<EventoEntity> {
        val limite = ahora.minusSeconds(DURACION_HORAS * 3600)
        return eventos
            .filter { it.estado.uppercase() !in NO_VISIBLES }
            .mapNotNull { e -> inicio(e)?.let { e to it } }
            .filter { (_, i) -> i.isAfter(limite) }
            .sortedBy { (_, i) -> i }
            .map { (e, _) -> e }
    }

    /** "20:00" en hora de México. */
    fun hora(evento: EventoEntity): String =
        inicio(evento)?.let { DateTimeFormatter.ofPattern("HH:mm").withZone(ZONA).format(it) } ?: ""

    /** Encabezado del día en la agenda: "Hoy", "Mañana" o "vie 27 nov" (hora de México). */
    fun etiquetaDia(evento: EventoEntity, ahora: Instant = Instant.now()): String {
        val i = inicio(evento) ?: return ""
        val dia = i.atZone(ZONA).toLocalDate()
        val hoy = ahora.atZone(ZONA).toLocalDate()
        return when (dia) {
            hoy -> "Hoy"
            hoy.plusDays(1) -> "Mañana"
            else -> DateTimeFormatter.ofPattern("EEE d MMM", ES).format(dia).replace(".", "")
                .replaceFirstChar { it.uppercase() }
        }
    }

    /** Conciertos vigentes agrupados por día, en orden. */
    fun porDia(eventos: List<EventoEntity>, ahora: Instant = Instant.now()): List<Pair<String, List<EventoEntity>>> =
        vigentes(eventos, ahora).groupBy { etiquetaDia(it, ahora) }.toList()

    /** "mié 26 nov · 20:00" en hora de México. */
    fun fechaCorta(evento: EventoEntity): String =
        inicio(evento)?.let {
            DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", ES).withZone(ZONA).format(it).replace(".", "")
        } ?: ""

    /** "Faltan 2 días y 3 h", "Faltan 1 h 5 min", "Faltan 12 min" o "En curso". */
    fun faltan(evento: EventoEntity, ahora: Instant = Instant.now()): String {
        val i = inicio(evento) ?: return ""
        val d = Duration.between(ahora, i)
        if (d.isNegative || d.isZero) return "En curso"
        val dias = d.toDays()
        val horas = d.toHours() % 24
        val minutos = d.toMinutes() % 60
        return when {
            dias > 0 -> "Faltan $dias ${if (dias == 1L) "día" else "días"} y $horas h"
            horas > 0 -> "Faltan $horas h $minutos min"
            else -> "Faltan ${minutos.coerceAtLeast(1)} min"
        }
    }

    /** true si empieza en menos de 30 minutos o ya empezó. */
    fun estaPorComenzar(evento: EventoEntity, ahora: Instant = Instant.now()): Boolean =
        inicio(evento)?.let { Duration.between(ahora, it).toMinutes() < 30 } ?: false

    /** Lugar para mostrar: escenario o, si no hay, la dirección. */
    fun lugar(evento: EventoEntity): String = evento.escenario?.takeIf { it.isNotBlank() } ?: evento.ubicacion

    // ---------- Distancias ----------

    fun distanciaM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Rumbo en grados (0 = norte, 90 = este) para girar la flecha hacia el destino. */
    fun rumbo(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val y = sin(Math.toRadians(lon2 - lon1)) * cos(Math.toRadians(lat2))
        val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
            sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(Math.toRadians(lon2 - lon1))
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /** Dirección hacia el destino: "al norte", "al suroeste"… */
    fun direccion(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val grados = rumbo(lat1, lon1, lat2, lon2)
        val nombres = listOf("al norte", "al noreste", "al este", "al sureste", "al sur", "al suroeste", "al oeste", "al noroeste")
        return nombres[((grados + 22.5) / 45).toInt() % 8]
    }

    /** "850 m" o "1.4 km". */
    fun textoDistancia(metros: Double): String =
        if (metros < 1000) "${metros.toInt()} m" else String.format(Locale.US, "%.1f km", metros / 1000)

    /** Minutos caminando a 4.8 km/h (80 m/min). */
    fun minutosCaminando(metros: Double): Int = ceil(metros / 80.0).toInt().coerceAtLeast(1)

    /** Centro del Jardín Principal (OpenStreetMap), punto de partida del mapa. */
    val JARDIN = 21.157511 to -100.934529

    /** A menos de esta distancia se considera que ya llegaste. */
    const val LLEGADA_M = 30.0
}

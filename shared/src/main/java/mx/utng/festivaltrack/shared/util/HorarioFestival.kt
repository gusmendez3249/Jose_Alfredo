package mx.utng.festivaltrack.shared.util

import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Cartelera del festival en **hora de México**, sin importar la zona del dispositivo.
 *
 * El backend guarda las fechas en UTC (`2026-11-26T02:00:00.000Z`). Mostrar la parte después
 * de la "T" daba "02:00" para un concierto de las 20:00; aquí siempre se convierte a
 * America/Mexico_City. Mismas reglas que la cartelera del móvil: un concierto se muestra hasta
 * [DURACION_HORAS] horas después de empezar, y nunca los finalizados, cancelados o borradores.
 */
object HorarioFestival {

    /** Tiempo que un concierto sigue vigente (y "en curso") después de empezar. */
    const val DURACION_HORAS = 4L

    val ZONA: ZoneId = ZoneId.of("America/Mexico_City")
    private val ES = Locale("es", "MX")
    private val NO_VISIBLES = setOf("FINALIZADO", "CANCELADO", "BORRADOR")

    private val HORA = DateTimeFormatter.ofPattern("HH:mm", ES).withZone(ZONA)
    private val FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES).withZone(ZONA)
    private val FECHA_CORTA = DateTimeFormatter.ofPattern("EEE d MMM", ES).withZone(ZONA)

    /** Instante de inicio; null si la fecha no se puede leer. */
    fun inicio(evento: EventoEntity): Instant? = runCatching { Instant.parse(evento.fechaHora) }.getOrNull()

    /** Conciertos que todavía no terminan, del más próximo al más lejano. */
    fun proximos(eventos: List<EventoEntity>, ahora: Instant = Instant.now()): List<EventoEntity> {
        val limite = ahora.minusSeconds(DURACION_HORAS * 3600)
        return eventos
            .filter { it.estado.uppercase() !in NO_VISIBLES }
            .mapNotNull { evento -> inicio(evento)?.let { evento to it } }
            .filter { (_, inicio) -> inicio.isAfter(limite) }
            .sortedBy { (_, inicio) -> inicio }
            .map { (evento, _) -> evento }
    }

    /** true si ya empezó y no han pasado [DURACION_HORAS] horas. */
    fun enCurso(evento: EventoEntity, ahora: Instant = Instant.now()): Boolean {
        val inicio = inicio(evento) ?: return false
        return !inicio.isAfter(ahora) && inicio.plusSeconds(DURACION_HORAS * 3600).isAfter(ahora)
    }

    /** "20:00" */
    fun hora(evento: EventoEntity): String = inicio(evento)?.let { HORA.format(it) } ?: ""

    /** "jueves 26 de noviembre" */
    fun fechaLarga(evento: EventoEntity): String = inicio(evento)?.let { FECHA_LARGA.format(it) } ?: ""

    /** "jue 26 nov" */
    fun fechaCorta(evento: EventoEntity): String =
        inicio(evento)?.let { FECHA_CORTA.format(it).replace(".", "") } ?: ""

    /** "Hoy", "Mañana" o "jueves 26 de noviembre", según el día en México. */
    fun dia(evento: EventoEntity, ahora: Instant = Instant.now()): String {
        val inicio = inicio(evento) ?: return ""
        val dia = inicio.atZone(ZONA).toLocalDate()
        val hoy = LocalDate.ofInstant(ahora, ZONA)
        return when (dia) {
            hoy -> "Hoy"
            hoy.plusDays(1) -> "Mañana"
            else -> FECHA_LARGA.format(inicio)
        }
    }

    /** "Hoy a las 20:00 hrs" / "Mañana a las 21:30 hrs" / "jueves 26 de noviembre a las 11:00 hrs". */
    fun cuando(evento: EventoEntity, ahora: Instant = Instant.now()): String {
        val dia = dia(evento, ahora)
        return if (dia.isEmpty()) "" else "$dia a las ${hora(evento)} hrs"
    }

    /** Hora actual en Dolores Hidalgo: "13:42". */
    fun horaActual(ahora: Instant = Instant.now()): String = HORA.format(ahora)

    /** Fecha actual en Dolores Hidalgo: "miércoles 25 de noviembre". */
    fun fechaActual(ahora: Instant = Instant.now()): String = FECHA_LARGA.format(ahora)
}

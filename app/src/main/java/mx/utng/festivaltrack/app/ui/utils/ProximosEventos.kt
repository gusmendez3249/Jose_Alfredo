package mx.utng.festivaltrack.app.ui.utils

import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Reglas de la cartelera pública: qué eventos se muestran al usuario.
 *
 * Un evento se muestra mientras no haya terminado: se considera vigente hasta
 * [DURACION_HORAS] horas después de su inicio (un concierto que ya empezó sigue apareciendo).
 * Los finalizados, cancelados y borradores nunca se muestran. El panel de administración
 * no usa este filtro: necesita ver todos los eventos.
 */
object ProximosEventos {

    /** Tiempo que un evento sigue vigente después de empezar. */
    const val DURACION_HORAS = 4L

    private val NO_VISIBLES = setOf("FINALIZADO", "CANCELADO", "BORRADOR")
    private val ZONA: ZoneId = ZoneId.of("America/Mexico_City")
    private val MES_CORTO = DateTimeFormatter.ofPattern("MMM", Locale("es", "MX")).withZone(ZONA)
    private val DIA = DateTimeFormatter.ofPattern("d").withZone(ZONA)
    private val HORA = DateTimeFormatter.ofPattern("HH:mm").withZone(ZONA)

    /** Instante de inicio del evento; null si la fecha no se puede leer. */
    fun inicio(evento: EventoEntity): Instant? = runCatching { Instant.parse(evento.fechaHora) }.getOrNull()

    /** Eventos que todavía no terminan, del más próximo al más lejano. */
    fun filtrar(eventos: List<EventoEntity>, ahora: Instant = Instant.now()): List<EventoEntity> {
        val limite = ahora.minusSeconds(DURACION_HORAS * 3600)
        return eventos
            .filter { it.estado.uppercase() !in NO_VISIBLES }
            .mapNotNull { evento -> inicio(evento)?.let { evento to it } }
            .filter { (_, inicio) -> inicio.isAfter(limite) }
            .sortedBy { (_, inicio) -> inicio }
            .map { (evento, _) -> evento }
    }

    /** "NOV" para el selector de fechas. */
    fun mesCorto(evento: EventoEntity): String =
        inicio(evento)?.let { MES_CORTO.format(it).replace(".", "").uppercase(Locale("es", "MX")) } ?: ""

    /** "27" para el selector de fechas. */
    fun dia(evento: EventoEntity): String = inicio(evento)?.let { DIA.format(it) } ?: ""

    /** "21:00" para distinguir dos conciertos del mismo día. */
    fun hora(evento: EventoEntity): String = inicio(evento)?.let { HORA.format(it) } ?: ""
}

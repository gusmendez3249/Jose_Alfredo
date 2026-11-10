package mx.utng.festivaltrack.app.auto

import mx.utng.festivaltrack.shared.data.remote.VisitaSitioDto
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/** Sitios del festival descubiertos con la cámara AR durante el día de hoy. */
data class ResumenAr(val nombres: List<String>) {
    val total: Int get() = nombres.size
}

/**
 * Arma la tarjeta "Resumen AR" del Home del modo automóvil a partir de `GET /ar/historial`.
 */
object ResumenArCalculator {

    /** Nombres que caben en una fila del auto sin distraer al conductor. */
    private const val MAX_NOMBRES = 2

    /**
     * Sitios distintos visitados hoy (según [zona]), del más reciente al más antiguo.
     * Un sitio reconocido varias veces cuenta una sola vez.
     */
    fun deHoy(
        visitas: List<VisitaSitioDto>,
        ahora: Instant = Instant.now(),
        zona: ZoneId = ZoneId.systemDefault()
    ): ResumenAr {
        val hoy = ahora.atZone(zona).toLocalDate()
        val nombres = visitas
            .mapNotNull { visita -> parsearFecha(visita.fechaVisita)?.let { it to visita } }
            .filter { (fecha, _) -> fecha.atZone(zona).toLocalDate() == hoy }
            .sortedByDescending { (fecha, _) -> fecha }
            .distinctBy { (_, visita) -> visita.sitio.id }
            .map { (_, visita) -> visita.sitio.nombre }
        return ResumenAr(nombres)
    }

    /** "Aún no descubres sitios hoy", "1 sitio hoy: Parroquia" o "3 sitios hoy: A, B y 1 más". */
    fun texto(resumen: ResumenAr): String {
        if (resumen.total == 0) return "Aún no descubres sitios hoy"
        val etiqueta = if (resumen.total == 1) "1 sitio hoy" else "${resumen.total} sitios hoy"
        val visibles = resumen.nombres.take(MAX_NOMBRES).joinToString(", ")
        val restantes = resumen.total - MAX_NOMBRES
        return if (restantes > 0) "$etiqueta: $visibles y $restantes más" else "$etiqueta: $visibles"
    }

    private fun parsearFecha(fecha: String): Instant? =
        runCatching { Instant.parse(fecha) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(fecha).toInstant() }.getOrNull()
}

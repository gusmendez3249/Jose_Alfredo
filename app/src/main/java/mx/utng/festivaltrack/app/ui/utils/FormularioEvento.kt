package mx.utng.festivaltrack.app.ui.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Lugar del festival donde puede haber un concierto, con sus coordenadas reales. */
data class LugarFestival(val escenario: String, val ubicacion: String, val latitud: Double, val longitud: Double)

/** Reglas del formulario de alta/edición de conciertos del administrador. */
object FormularioEvento {

    val ZONA: ZoneId = ZoneId.of("America/Mexico_City")

    /** Mismos lugares y coordenadas que el backend (`prisma/ubicaciones.ts`, tomadas de OpenStreetMap). */
    val LUGARES = listOf(
        LugarFestival("Escenario Principal", "Jardín Principal, frente a la Parroquia, Dolores Hidalgo", 21.1583, -100.9341),
        LugarFestival("Plaza Principal", "Jardín Principal, Dolores Hidalgo", 21.157511, -100.934529),
        LugarFestival("Mausoleo de José Alfredo Jiménez", "Panteón Municipal, Dolores Hidalgo", 21.156687, -100.948773),
        LugarFestival("Casa Museo", "Calle Guanajuato 13, Centro, Dolores Hidalgo", 21.157341, -100.93277)
    )

    /** Fecha y hora de Ciudad de México → instante ISO en UTC para el servidor. */
    fun fechaIso(fecha: LocalDate, hora: LocalTime): String =
        ZonedDateTime.of(fecha, hora, ZONA).toInstant().toString()

    /** Instante ISO del servidor → fecha y hora de Ciudad de México (null si no se puede leer). */
    fun desdeIso(iso: String?): Pair<LocalDate, LocalTime>? = runCatching {
        val local = Instant.parse(iso).atZone(ZONA)
        local.toLocalDate() to local.toLocalTime().withSecond(0).withNano(0)
    }.getOrNull()

    /** Lugar conocido más cercano a las coordenadas (para preseleccionarlo al editar). */
    fun lugarPara(escenario: String?, latitud: Double?, longitud: Double?): LugarFestival? =
        LUGARES.firstOrNull { it.escenario.equals(escenario, ignoreCase = true) }
            ?: if (latitud != null && longitud != null) LUGARES.firstOrNull {
                kotlin.math.abs(it.latitud - latitud) < 0.0005 && kotlin.math.abs(it.longitud - longitud) < 0.0005
            } else null

    /**
     * Revisa el formulario; regresa el mensaje del primer problema o null si está bien.
     * [capacidadTexto] puede ir vacío al editar (se conserva la actual).
     */
    fun validar(
        nombre: String,
        fecha: LocalDate?,
        hora: LocalTime?,
        lugar: LugarFestival?,
        capacidadTexto: String,
        editando: Boolean,
        ahora: Instant = Instant.now()
    ): String? {
        if (nombre.isBlank()) return "Escribe el nombre del concierto"
        if (fecha == null || hora == null) return "Elige la fecha y la hora"
        if (lugar == null) return "Elige el lugar del concierto"
        val inicio = ZonedDateTime.of(fecha, hora, ZONA).toInstant()
        if (!editando && inicio.isBefore(ahora)) return "La fecha y hora ya pasaron"
        if (capacidadTexto.isBlank()) return if (editando) null else "Escribe la capacidad"
        val capacidad = capacidadTexto.toIntOrNull() ?: return "La capacidad debe ser un número"
        if (capacidad !in 1..100_000) return "La capacidad debe estar entre 1 y 100,000"
        return null
    }
}

package mx.utng.festivaltrack.app.auto

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Textos cortos que comparten las pantallas del modo automóvil: día, hora y distancia.
 *
 * Todo se muestra en la hora de Dolores Hidalgo, sin importar la zona configurada en el auto,
 * y siempre con el día ("mañana", "jue 27 nov") para que un horario nunca se confunda con hoy.
 */
object TextosAuto {

    val ZONA: ZoneId = ZoneId.of("America/Mexico_City")

    /** A menos de esta distancia el conductor ya está en el lugar. */
    const val ESTAS_AQUI_KM = 0.03

    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm").withZone(ZONA)
    private val DIAS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
    private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    /** "21:30". */
    fun hora(instante: Instant): String = formatoHora.format(instante)

    /** "hoy", "mañana" o "jue 27 nov", según el día de [instante] respecto a [ahora]. */
    fun dia(instante: Instant, ahora: Instant): String {
        val fecha = instante.atZone(ZONA).toLocalDate()
        val hoy = ahora.atZone(ZONA).toLocalDate()
        return when (ChronoUnit.DAYS.between(hoy, fecha)) {
            0L -> "hoy"
            1L -> "mañana"
            else -> "${DIAS[fecha.dayOfWeek.value - 1]} ${fecha.dayOfMonth} ${MESES[fecha.monthValue - 1]}"
        }
    }

    /** "Mañana 21:30 hrs" u "Hoy 20:00 hrs": cuándo empieza un evento. */
    fun cuando(inicio: Instant, ahora: Instant): String =
        "${dia(inicio, ahora).replaceFirstChar { it.titlecase(Locale("es", "MX")) }} ${hora(inicio)} hrs"

    /**
     * "inicia 21:30 hrs" si el evento empieza el mismo día en que hay que salir (el día ya se dijo
     * en la hora de salida), o "inicia mañana 00:15 hrs" si cambia de día.
     */
    fun inicia(inicio: Instant, horaSalida: Instant, ahora: Instant): String =
        if (dia(inicio, ahora) == dia(horaSalida, ahora)) "inicia ${hora(inicio)} hrs"
        else "inicia ${cuando(inicio, ahora).replaceFirstChar { it.lowercase() }}"

    /**
     * Nombre corto para el panel angosto junto al mapa: quita lo que el conductor ya sabe
     * ("de José Alfredo Jiménez") y abrevia los nombres largos de los sitios del festival.
     * "Mausoleo de José Alfredo Jiménez" → "Mausoleo"; "Parroquia de Nuestra Señora de los
     * Dolores" → "Parroquia de Dolores"; "Monumento a Miguel Hidalgo" → "Monumento Hidalgo" (con "a" el auto parte la palabra).
     */
    fun nombreCorto(nombre: String): String = nombre
        .replace(Regex("\\s+(de\\s+)?José Alfredo( Jiménez)?$"), "")
        .replace("Nuestra Señora de los Dolores", "Dolores")
        .replace("Monumento a Miguel Hidalgo", "Monumento Hidalgo")
        .trim()
        .ifEmpty { nombre }

    /** "Mañana 21:30" (sin "hrs"), para el panel angosto junto al mapa. */
    fun cuandoCorto(inicio: Instant, ahora: Instant): String = cuando(inicio, ahora).removeSuffix(" hrs")

    /** "Estás aquí", "850 m" o "3.4 km". */
    fun distancia(km: Double): String = when {
        km < ESTAS_AQUI_KM -> "Estás aquí"
        km < 1 -> "${(km * 1000).toInt()} m"
        else -> "%.1f km".format(Locale.US, km)
    }

    /**
     * "1.5 km · 4 min en auto", o solo "Estás aquí" si ya llegó.
     * Sin ubicación ([desdeCentro]) la distancia se mide desde el Jardín Principal y así se dice,
     * para no afirmar "Estás aquí" sin saber dónde está el auto.
     */
    fun trayecto(km: Double, minutos: Int, desdeCentro: Boolean = false, corto: Boolean = false): String {
        val tiempo = NavegacionCalculator.textoTiempo(minutos) + if (corto) "" else " en auto"
        return when {
            desdeCentro && km < ESTAS_AQUI_KM -> "En el centro de Dolores Hidalgo"
            desdeCentro -> "${distancia(km)} del centro · $tiempo"
            km < ESTAS_AQUI_KM -> distancia(km)
            else -> "${distancia(km)} · $tiempo"
        }
    }
}

package mx.utng.festivaltrack.app.auto

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Resumen de la ruta activa para la pantalla 8 "Navegación".
 *
 * @property distanciaKm Distancia en línea recta desde la ubicación actual.
 * @property trasladoMin Tiempo estimado en automóvil (mismo cálculo que la alerta de salida).
 * @property llegada Hora estimada de llegada si se sale ahora.
 * @property uriNavegacion URI `geo:` que recibe la app de navegación del auto.
 */
data class ResumenNavegacion(
    val destino: String,
    val distanciaKm: Double,
    val trasladoMin: Int,
    val llegada: Instant,
    val uriNavegacion: String
)

/**
 * Calcula lo que muestra la pantalla de navegación antes de abrir Google Maps o Waze:
 * distancia, tiempo y hora de llegada hacia la [RutaActiva] elegida en el teléfono.
 */
object NavegacionCalculator {

    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    fun resumen(
        ruta: RutaActiva,
        origenLat: Double,
        origenLon: Double,
        ahora: Instant = Instant.now()
    ): ResumenNavegacion {
        val km = SalidaCalculator.distanciaKm(origenLat, origenLon, ruta.latitud, ruta.longitud)
        val minutos = SalidaCalculator.trasladoMin(km)
        return ResumenNavegacion(
            destino = ruta.nombre,
            distanciaKm = km,
            trasladoMin = minutos,
            llegada = ahora.plus(Duration.ofMinutes(minutos.toLong())),
            uriNavegacion = uriNavegacion(ruta.latitud, ruta.longitud)
        )
    }

    /** URI que entiende `CarContext.ACTION_NAVIGATE`: `geo:21.1566,-100.9341`. */
    fun uriNavegacion(latitud: Double, longitud: Double): String = "geo:$latitud,$longitud"

    /** Texto corto de tiempo: "8 min" o "1 h 5 min". */
    fun textoTiempo(minutos: Int): String = when {
        minutos < 60 -> "$minutos min"
        minutos % 60 == 0 -> "${minutos / 60} h"
        else -> "${minutos / 60} h ${minutos % 60} min"
    }

    /** Frase que se lee en voz alta al iniciar la navegación. */
    fun textoVoz(resumen: ResumenNavegacion): String =
        "Iniciando navegación a ${resumen.destino}. " +
            "Llegarás en unos ${resumen.trasladoMin} minutos, a las ${formatoHora.format(resumen.llegada)}."
}

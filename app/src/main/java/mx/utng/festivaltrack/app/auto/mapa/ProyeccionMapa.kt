package mx.utng.festivaltrack.app.auto.mapa

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/**
 * Cuentas de la proyección Web Mercator que usan los mosaicos de OpenStreetMap.
 *
 * Un "píxel de mundo" es la posición dentro del mapa completo al nivel de [zoom]
 * (256 · 2^zoom píxeles de ancho). La vista del auto se describe con su centro y su zoom.
 */
object ProyeccionMapa {

    const val TAM_MOSAICO = 256.0
    const val ZOOM_MIN = 12.0
    const val ZOOM_MAX = 18.0

    private const val LAT_MAX = 85.05112878

    fun xMundo(lon: Double, zoom: Double): Double = (lon + 180.0) / 360.0 * tamMundo(zoom)

    fun yMundo(lat: Double, zoom: Double): Double {
        val l = Math.toRadians(lat.coerceIn(-LAT_MAX, LAT_MAX))
        return (1.0 - ln(tan(l) + 1.0 / kotlin.math.cos(l)) / PI) / 2.0 * tamMundo(zoom)
    }

    fun lonDe(x: Double, zoom: Double): Double = x / tamMundo(zoom) * 360.0 - 180.0

    fun latDe(y: Double, zoom: Double): Double {
        val n = PI - 2.0 * PI * y / tamMundo(zoom)
        return Math.toDegrees(atan(0.5 * (exp(n) - exp(-n))))
    }

    fun tamMundo(zoom: Double): Double = TAM_MOSAICO * Math.pow(2.0, zoom)

    /** Índice del mosaico que contiene un píxel de mundo, a nivel entero de zoom. */
    fun mosaico(pixelMundo: Double): Int = floor(pixelMundo / TAM_MOSAICO).toInt()

    /**
     * Centro y zoom para que todos los [puntos] quepan en un área de [ancho]×[alto] píxeles
     * dejando [margen] alrededor. Con un solo punto se usa [zoomUnPunto].
     */
    fun encuadrar(
        puntos: List<Pair<Double, Double>>,
        ancho: Double,
        alto: Double,
        margen: Double,
        zoomUnPunto: Double = 16.0
    ): Vista? {
        if (puntos.isEmpty()) return null
        val latMin = puntos.minOf { it.first }
        val latMax = puntos.maxOf { it.first }
        val lonMin = puntos.minOf { it.second }
        val lonMax = puntos.maxOf { it.second }
        val centroX0 = (xMundo(lonMin, 0.0) + xMundo(lonMax, 0.0)) / 2
        val centroY0 = (yMundo(latMax, 0.0) + yMundo(latMin, 0.0)) / 2
        val centro = latDe(centroY0, 0.0) to lonDe(centroX0, 0.0)
        val anchoMundo0 = xMundo(lonMax, 0.0) - xMundo(lonMin, 0.0)
        val altoMundo0 = yMundo(latMin, 0.0) - yMundo(latMax, 0.0)
        if (anchoMundo0 <= 0 && altoMundo0 <= 0) return Vista(centro.first, centro.second, zoomUnPunto)
        val utilAncho = max(ancho - 2 * margen, 1.0)
        val utilAlto = max(alto - 2 * margen, 1.0)
        val zx = if (anchoMundo0 > 0) log2(utilAncho / anchoMundo0) else ZOOM_MAX
        val zy = if (altoMundo0 > 0) log2(utilAlto / altoMundo0) else ZOOM_MAX
        val zoom = min(zx, zy).coerceIn(ZOOM_MIN, ZOOM_MAX - 0.5)
        return Vista(centro.first, centro.second, zoom)
    }
}

/** Lo que se ve del mapa: centro (latitud, longitud) y zoom (puede ser fraccionario). */
data class Vista(val lat: Double, val lon: Double, val zoom: Double)

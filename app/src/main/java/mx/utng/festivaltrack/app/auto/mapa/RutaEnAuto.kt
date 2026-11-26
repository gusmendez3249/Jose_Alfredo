package mx.utng.festivaltrack.app.auto.mapa

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.app.auto.SalidaCalculator
import mx.utng.festivaltrack.app.ui.utils.Ruta
import mx.utng.festivaltrack.app.ui.utils.RutaPeatonal
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.cos

/**
 * Ruta en automóvil por calles con el servicio OSRM de OpenStreetMap (perfil para autos).
 *
 * Reutiliza la lectura de respuestas de [RutaPeatonal]; si no hay conexión regresa la línea recta
 * para que el mapa siempre muestre hacia dónde ir. El tiempo se calcula con la misma velocidad de
 * ciudad que la alerta de salida, para que todas las pantallas den el mismo número.
 */
object RutaEnAuto {

    private const val SERVICIO = "https://routing.openstreetmap.de/routed-car/route/v1/driving"

    suspend fun calcular(origenLat: Double, origenLon: Double, destinoLat: Double, destinoLon: Double): Ruta =
        withContext(Dispatchers.IO) {
            val porCalles = runCatching { pedir(origenLat, origenLon, destinoLat, destinoLon) }.getOrNull()
            (porCalles ?: RutaPeatonal.lineaRecta(origenLat, origenLon, destinoLat, destinoLon))
                .let { it.copy(minutos = minutosEnAuto(it.distanciaM)) }
        }

    /** Minutos manejando a la velocidad promedio del festival (mínimo 1). */
    fun minutosEnAuto(metros: Int): Int =
        ceil(metros / 1000.0 / SalidaCalculator.VELOCIDAD_PROMEDIO_KMH * 60).toInt().coerceAtLeast(1)

    /**
     * Distancia en metros de un punto a la ruta (al tramo más cercano). Sirve para saber si el
     * conductor se salió de la ruta y hay que recalcularla.
     */
    fun distanciaARutaM(lat: Double, lon: Double, puntos: List<Pair<Double, Double>>): Double {
        if (puntos.isEmpty()) return Double.MAX_VALUE
        if (puntos.size == 1) return SalidaCalculator.distanciaKm(lat, lon, puntos[0].first, puntos[0].second) * 1000
        // Plano local en metros alrededor del punto: suficiente para distancias de una ciudad.
        val mLat = 111_320.0
        val mLon = 111_320.0 * cos(Math.toRadians(lat))
        var mejor = Double.MAX_VALUE
        for (i in 0 until puntos.lastIndex) {
            val ax = (puntos[i].second - lon) * mLon
            val ay = (puntos[i].first - lat) * mLat
            val bx = (puntos[i + 1].second - lon) * mLon
            val by = (puntos[i + 1].first - lat) * mLat
            val dx = bx - ax
            val dy = by - ay
            val largo2 = dx * dx + dy * dy
            val t = if (largo2 == 0.0) 0.0 else (-(ax * dx + ay * dy) / largo2).coerceIn(0.0, 1.0)
            val px = ax + t * dx
            val py = ay + t * dy
            mejor = minOf(mejor, kotlin.math.sqrt(px * px + py * py))
        }
        return mejor
    }

    /**
     * Metros que faltan por recorrer: desde el punto de la ruta más cercano al auto hasta el final.
     * Así la distancia baja conforme el auto avanza, sin volver a pedir la ruta.
     */
    fun restanteM(lat: Double, lon: Double, puntos: List<Pair<Double, Double>>): Double {
        if (puntos.size < 2) return puntos.firstOrNull()
            ?.let { SalidaCalculator.distanciaKm(lat, lon, it.first, it.second) * 1000 } ?: 0.0
        var tramoCercano = 0
        var mejor = Double.MAX_VALUE
        for (i in 0 until puntos.lastIndex) {
            val d = distanciaARutaM(lat, lon, listOf(puntos[i], puntos[i + 1]))
            if (d < mejor) {
                mejor = d
                tramoCercano = i
            }
        }
        val siguiente = puntos[tramoCercano + 1]
        var total = SalidaCalculator.distanciaKm(lat, lon, siguiente.first, siguiente.second) * 1000
        for (i in tramoCercano + 1 until puntos.lastIndex) {
            total += SalidaCalculator.distanciaKm(puntos[i].first, puntos[i].second, puntos[i + 1].first, puntos[i + 1].second) * 1000
        }
        return total
    }

    private fun pedir(origenLat: Double, origenLon: Double, destinoLat: Double, destinoLon: Double): Ruta? {
        val coords = String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f", origenLon, origenLat, destinoLon, destinoLat)
        val conexion = URL("$SERVICIO/$coords?overview=full&geometries=geojson").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 6000
            conexion.readTimeout = 8000
            conexion.setRequestProperty("User-Agent", "FestivalTrack-App")
            if (conexion.responseCode != 200) return null
            RutaPeatonal.parsear(conexion.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conexion.disconnect()
        }
    }
}

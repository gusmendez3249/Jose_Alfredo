package mx.utng.festivaltrack.app.ui.utils

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.app.auto.SalidaCalculator
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.ceil

/**
 * Ruta a pie entre dos puntos.
 *
 * @property puntos Trazo de la ruta como pares (latitud, longitud), del origen al destino.
 * @property distanciaM Distancia a recorrer en metros.
 * @property minutos Tiempo estimado caminando.
 * @property porCalles true si la ruta sigue las calles; false si es la línea recta de respaldo.
 */
data class Ruta(
    val puntos: List<Pair<Double, Double>>,
    val distanciaM: Int,
    val minutos: Int,
    val porCalles: Boolean
) {
    /** "650 m · 9 min a pie" o "1.4 km · 18 min a pie". */
    val resumen: String get() = "${RutaPeatonal.textoDistancia(distanciaM)} · $minutos min a pie"
}

/**
 * Calcula rutas a pie por calles con el servicio público OSRM de OpenStreetMap (perfil peatonal).
 * Si no hay conexión o el servicio no responde, regresa la línea recta con un tiempo estimado,
 * para que el mapa siempre muestre algo útil.
 */
object RutaPeatonal {

    /** Velocidad al caminar: 4.8 km/h. */
    private const val METROS_POR_MINUTO = 80.0

    private const val SERVICIO = "https://routing.openstreetmap.de/routed-foot/route/v1/foot"

    suspend fun calcular(origenLat: Double, origenLon: Double, destinoLat: Double, destinoLon: Double): Ruta =
        withContext(Dispatchers.IO) {
            runCatching { pedirRuta(origenLat, origenLon, destinoLat, destinoLon) }.getOrNull()
                ?: lineaRecta(origenLat, origenLon, destinoLat, destinoLon)
        }

    /** Ruta de respaldo: línea recta con el tiempo que tomaría caminarla. */
    fun lineaRecta(origenLat: Double, origenLon: Double, destinoLat: Double, destinoLon: Double): Ruta {
        val metros = (SalidaCalculator.distanciaKm(origenLat, origenLon, destinoLat, destinoLon) * 1000).toInt()
        return Ruta(
            puntos = listOf(origenLat to origenLon, destinoLat to destinoLon),
            distanciaM = metros,
            minutos = minutosCaminando(metros),
            porCalles = false
        )
    }

    fun minutosCaminando(metros: Int): Int = ceil(metros / METROS_POR_MINUTO).toInt().coerceAtLeast(1)

    /**
     * Lee la respuesta de OSRM (`geometries=geojson`). La geometría viene como [longitud, latitud].
     * Regresa null si no trae una ruta válida.
     */
    fun parsear(json: String): Ruta? {
        val raiz = @Suppress("DEPRECATION") JsonParser().parse(json).asJsonObject
        if (raiz.get("code")?.asString != "Ok") return null
        val ruta = raiz.getAsJsonArray("routes")?.firstOrNull()?.asJsonObject ?: return null
        val coordenadas = ruta.getAsJsonObject("geometry")?.getAsJsonArray("coordinates") ?: return null
        val puntos = coordenadas.map { c -> c.asJsonArray.let { it[1].asDouble to it[0].asDouble } }
        if (puntos.size < 2) return null
        val metros = ruta.get("distance").asDouble.toInt()
        // El tiempo del servicio puede variar según el servidor; usamos el ritmo de caminata propio.
        return Ruta(puntos, metros, minutosCaminando(metros), porCalles = true)
    }

    private fun pedirRuta(origenLat: Double, origenLon: Double, destinoLat: Double, destinoLon: Double): Ruta? {
        val coords = String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f", origenLon, origenLat, destinoLon, destinoLat)
        val conexion = URL("$SERVICIO/$coords?overview=full&geometries=geojson").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 6000
            conexion.readTimeout = 8000
            conexion.setRequestProperty("User-Agent", "FestivalTrack-App")
            if (conexion.responseCode != 200) return null
            parsear(conexion.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conexion.disconnect()
        }
    }

    fun textoDistancia(metros: Int): String =
        if (metros < 1000) "$metros m" else String.format(Locale.US, "%.1f km", metros / 1000.0)
}

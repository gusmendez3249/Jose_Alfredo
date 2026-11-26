package mx.utng.festivaltrack.app.auto

import android.content.Context

/**
 * Destino elegido (en el teléfono o en el auto) que el modo automóvil retoma al conectarse.
 *
 * @property eventoNombre Concierto al que se va, si el destino se eligió por un evento.
 * @property eventoFechaHora Inicio de ese concierto en ISO 8601.
 */
data class RutaActiva(
    val nombre: String,
    val latitud: Double,
    val longitud: Double,
    val eventoNombre: String? = null,
    val eventoFechaHora: String? = null
)

/**
 * Guarda la ruta activa en SharedPreferences: la pantalla 7 la escribe al tocar "Navegar"
 * y la pantalla de inicio de Android Auto la lee para ofrecer "Ruta al festival".
 */
class RutaActivaStore(context: Context) {
    private val prefs = context.getSharedPreferences("ruta_activa", Context.MODE_PRIVATE)

    fun guardar(ruta: RutaActiva) {
        prefs.edit()
            .putString(KEY_NOMBRE, ruta.nombre)
            .putString(KEY_LAT, ruta.latitud.toString())
            .putString(KEY_LON, ruta.longitud.toString())
            .putString(KEY_EVENTO, ruta.eventoNombre)
            .putString(KEY_EVENTO_FECHA, ruta.eventoFechaHora)
            .apply()
    }

    fun obtener(): RutaActiva? {
        val nombre = prefs.getString(KEY_NOMBRE, null) ?: return null
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString(KEY_LON, null)?.toDoubleOrNull() ?: return null
        return RutaActiva(nombre, lat, lon, prefs.getString(KEY_EVENTO, null), prefs.getString(KEY_EVENTO_FECHA, null))
    }

    private companion object {
        const val KEY_NOMBRE = "nombre"
        const val KEY_LAT = "latitud"
        const val KEY_LON = "longitud"
        const val KEY_EVENTO = "evento"
        const val KEY_EVENTO_FECHA = "evento_fecha"
    }
}

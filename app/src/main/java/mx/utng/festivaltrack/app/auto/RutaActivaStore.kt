package mx.utng.festivaltrack.app.auto

import android.content.Context

/** Destino elegido en el teléfono que el modo automóvil retoma al conectarse al auto. */
data class RutaActiva(val nombre: String, val latitud: Double, val longitud: Double)

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
            .apply()
    }

    fun obtener(): RutaActiva? {
        val nombre = prefs.getString(KEY_NOMBRE, null) ?: return null
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString(KEY_LON, null)?.toDoubleOrNull() ?: return null
        return RutaActiva(nombre, lat, lon)
    }

    private companion object {
        const val KEY_NOMBRE = "nombre"
        const val KEY_LAT = "latitud"
        const val KEY_LON = "longitud"
    }
}

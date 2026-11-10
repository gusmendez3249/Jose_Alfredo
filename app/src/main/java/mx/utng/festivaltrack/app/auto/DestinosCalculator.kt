package mx.utng.festivaltrack.app.auto

import mx.utng.festivaltrack.shared.data.remote.EventoDto
import mx.utng.festivaltrack.shared.data.remote.SitioArDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Lugar del festival al que se puede navegar en auto.
 *
 * @property subtitulo Próximo evento en ese lugar ("Homenaje: El Rey • 21:00 hrs") o su dirección.
 * @property esDestacado true para el lugar del próximo evento: se muestra primero y resaltado.
 */
data class Destino(
    val nombre: String,
    val subtitulo: String,
    val latitud: Double,
    val longitud: Double,
    val distanciaKm: Double,
    val trasladoMin: Int,
    val esDestacado: Boolean
)

/**
 * Arma la lista de la pantalla 7 "Elige tu destino" a partir de los escenarios de los
 * próximos eventos y del catálogo de sitios, con distancia y tiempo desde [origenLat], [origenLon].
 */
object DestinosCalculator {

    /** Dos lugares a menos de esta distancia se consideran el mismo destino. */
    private const val MISMO_LUGAR_KM = 0.05

    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    fun construir(
        eventos: List<EventoDto>,
        sitios: List<SitioArDto>,
        origenLat: Double,
        origenLon: Double,
        ahora: Instant = Instant.now()
    ): List<Destino> {
        val proximos = SalidaCalculator.calcular(eventos, origenLat, origenLon, ahora)
            .filter { it.evento.latitud != null && it.evento.longitud != null }

        val destinos = mutableListOf<Destino>()

        // Un destino por escenario, con su evento más cercano en el tiempo.
        proximos.groupBy { it.evento.escenario ?: it.evento.ubicacion }.forEach { (lugar, salidas) ->
            val siguiente = salidas.first()
            destinos += destino(
                nombre = lugar,
                subtitulo = "${siguiente.evento.nombre} • ${formatoHora.format(siguiente.inicio)} hrs",
                lat = siguiente.evento.latitud!!,
                lon = siguiente.evento.longitud!!,
                origenLat = origenLat,
                origenLon = origenLon,
                esDestacado = siguiente === proximos.first()
            )
        }

        // Sitios del catálogo que no coinciden con un escenario ya listado.
        sitios.forEach { sitio ->
            val lat = sitio.latitud ?: return@forEach
            val lon = sitio.longitud ?: return@forEach
            val repetido = destinos.any { SalidaCalculator.distanciaKm(it.latitud, it.longitud, lat, lon) < MISMO_LUGAR_KM }
            if (!repetido) {
                destinos += destino(sitio.nombre, sitio.ubicacion, lat, lon, origenLat, origenLon, esDestacado = false)
            }
        }

        return destinos.sortedWith(compareByDescending<Destino> { it.esDestacado }.thenBy { it.distanciaKm })
    }

    /** Texto corto de distancia: "850 m" o "3.4 km". */
    fun textoDistancia(km: Double): String =
        if (km < 1) "${(km * 1000).toInt()} m" else "%.1f km".format(java.util.Locale.US, km)

    private fun destino(
        nombre: String, subtitulo: String, lat: Double, lon: Double,
        origenLat: Double, origenLon: Double, esDestacado: Boolean
    ): Destino {
        val km = SalidaCalculator.distanciaKm(origenLat, origenLon, lat, lon)
        return Destino(nombre, subtitulo, lat, lon, km, SalidaCalculator.trasladoMin(km), esDestacado)
    }
}

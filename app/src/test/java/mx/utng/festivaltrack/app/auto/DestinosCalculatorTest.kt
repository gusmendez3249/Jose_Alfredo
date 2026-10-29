package mx.utng.festivaltrack.app.auto

import mx.utng.festivaltrack.shared.data.remote.EventoDto
import mx.utng.festivaltrack.shared.data.remote.SitioArDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DestinosCalculatorTest {

    private val ahora = Instant.parse("2026-11-20T18:00:00Z")
    private val origenLat = 21.1530
    private val origenLon = -100.9340

    private fun evento(id: String, fecha: String, escenario: String, lat: Double, lon: Double) =
        EventoDto(id, "Evento $id", fecha, "Dolores Hidalgo", escenario, "PUBLICADO", lat, lon, null, null)

    private fun sitio(nombre: String, lat: Double, lon: Double) =
        SitioArDto(id = nombre, nombre = nombre, ubicacion = "Centro", descripcion = "", latitud = lat, longitud = lon)

    @Test
    fun `el escenario del proximo evento va primero y destacado`() {
        val destinos = DestinosCalculator.construir(
            eventos = listOf(
                evento("tarde", "2026-11-21T03:00:00Z", "Escenario Principal", 21.1566, -100.9341),
                evento("pronto", "2026-11-20T20:00:00Z", "Mausoleo", 21.1571, -100.9406)
            ),
            sitios = emptyList(),
            origenLat = origenLat, origenLon = origenLon, ahora = ahora
        )
        assertEquals("Mausoleo", destinos.first().nombre)
        assertTrue(destinos.first().esDestacado)
        assertFalse(destinos[1].esDestacado)
    }

    @Test
    fun `un escenario con varios eventos aparece una sola vez con el mas proximo`() {
        val destinos = DestinosCalculator.construir(
            eventos = listOf(
                evento("gala", "2026-11-21T03:00:00Z", "Escenario Principal", 21.1566, -100.9341),
                evento("homenaje", "2026-11-22T03:00:00Z", "Escenario Principal", 21.1566, -100.9341)
            ),
            sitios = emptyList(),
            origenLat = origenLat, origenLon = origenLon, ahora = ahora
        )
        assertEquals(1, destinos.size)
        assertTrue(destinos.single().subtitulo.startsWith("Evento gala"))
    }

    @Test
    fun `los sitios del catalogo en el mismo lugar que un escenario no se repiten`() {
        val destinos = DestinosCalculator.construir(
            eventos = listOf(evento("e", "2026-11-20T20:00:00Z", "Escenario Principal", 21.1566, -100.9341)),
            sitios = listOf(sitio("Monumento a Hidalgo", 21.1566, -100.9341), sitio("Casa Museo", 21.1574, -100.9330)),
            origenLat = origenLat, origenLon = origenLon, ahora = ahora
        )
        assertEquals(listOf("Escenario Principal", "Casa Museo"), destinos.map { it.nombre })
    }

    @Test
    fun `sin eventos los sitios se ordenan por distancia`() {
        val destinos = DestinosCalculator.construir(
            eventos = emptyList(),
            sitios = listOf(sitio("Lejos", 21.2000, -100.9340), sitio("Cerca", 21.1540, -100.9340)),
            origenLat = origenLat, origenLon = origenLon, ahora = ahora
        )
        assertEquals(listOf("Cerca", "Lejos"), destinos.map { it.nombre })
        assertTrue(destinos.none { it.esDestacado })
    }

    @Test
    fun `texto de distancia en metros y kilometros`() {
        assertEquals("850 m", DestinosCalculator.textoDistancia(0.85))
        assertEquals("3.4 km", DestinosCalculator.textoDistancia(3.42))
    }
}

package mx.utng.festivaltrack.tv.presentation.screens

import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.SitioArDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvMapaTest {

    // Coordenadas oficiales de festivaltrack-backend/prisma/ubicaciones.ts
    private fun evento(id: String, escenario: String, lat: Double, lon: Double, sync: Long) = EventoEntity(
        id = id, nombre = "Concierto $id", fechaHora = "2026-11-28T03:00:00.000Z", ubicacion = "Dolores Hidalgo",
        escenario = escenario, bannerUrl = null, estado = "PUBLICADO", artistaId = null, artistaNombre = null,
        latitud = lat, longitud = lon, updatedAt = sync
    )

    private fun cartelera(sync: Long) = listOf(
        evento("1", "Mausoleo de José Alfredo Jiménez", 21.156687, -100.948773, sync),
        evento("2", "Casa Museo", 21.157341, -100.93277, sync),
        evento("3", "Escenario Principal", 21.1583, -100.9341, sync)
    )

    private val sitios = listOf(
        SitioArDto("SITIO-MAUSOLEO", "Mausoleo de José Alfredo Jiménez", "Panteón Municipal", "", latitud = 21.156687, longitud = -100.948773),
        SitioArDto("SITIO-PARROQUIA", "Parroquia de Dolores", "Jardín Principal", "", latitud = 21.158601, longitud = -100.933947),
        SitioArDto("SITIO-HIDALGO", "Monumento a Miguel Hidalgo", "Jardín Principal", "", latitud = 21.157511, longitud = -100.934529)
    )

    @Test
    fun `una sede por escenario y los sitios que no estan a menos de 50 m`() {
        val sedes = construirSedes(cartelera(sync = 1), sitios)
        // La Parroquia queda a ~37 m del Escenario Principal (mismo lugar); el Monumento, a ~98 m.
        assertEquals(
            listOf("Mausoleo de José Alfredo Jiménez", "Casa Museo", "Escenario Principal", "Monumento a Miguel Hidalgo"),
            sedes.map { it.nombre }
        )
        assertEquals(21.156687, sedes[0].lat, 0.0)
        assertEquals(-100.948773, sedes[0].lon, 0.0)
    }

    @Test
    fun `la sede elegida se conserva cuando la cartelera se vuelve a sincronizar`() {
        val antes = construirSedes(cartelera(sync = 1), sitios)
        val despues = construirSedes(cartelera(sync = 5_001), sitios) // mismos datos, otro updatedAt
        assertEquals("Casa Museo", elegirSede(antes, "Casa Museo")?.nombre)
        assertEquals("Casa Museo", elegirSede(despues, "Casa Museo")?.nombre)
    }

    @Test
    fun `sin eleccion o si la sede ya no existe se muestra la primera`() {
        val sedes = construirSedes(cartelera(sync = 1), sitios)
        assertEquals("Mausoleo de José Alfredo Jiménez", elegirSede(sedes, null)?.nombre)
        assertEquals("Mausoleo de José Alfredo Jiménez", elegirSede(sedes, "Sede que ya pasó")?.nombre)
        assertNull(elegirSede(emptyList(), "Casa Museo"))
    }

    @Test
    fun `la ruta a pie usa las coordenadas de la sede`() {
        val sede = construirSedes(cartelera(sync = 1), sitios).first()
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=21.156687,-100.948773&travelmode=walking",
            urlRuta(sede)
        )
    }
}

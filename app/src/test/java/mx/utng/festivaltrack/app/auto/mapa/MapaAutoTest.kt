package mx.utng.festivaltrack.app.auto.mapa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapaAutoTest {

    private val jardin = 21.157511 to -100.934529
    private val mausoleo = 21.156687 to -100.948773

    @Test
    fun `la proyeccion ida y vuelta regresa al mismo punto`() {
        val z = 15.5
        val x = ProyeccionMapa.xMundo(jardin.second, z)
        val y = ProyeccionMapa.yMundo(jardin.first, z)
        assertEquals(jardin.first, ProyeccionMapa.latDe(y, z), 1e-9)
        assertEquals(jardin.second, ProyeccionMapa.lonDe(x, z), 1e-9)
    }

    @Test
    fun `el mosaico del jardin a zoom 16 es el de OpenStreetMap`() {
        // Jardín Principal de Dolores Hidalgo: mosaico 16/14393/28825 (fórmula estándar de OSM).
        assertEquals(14393, ProyeccionMapa.mosaico(ProyeccionMapa.xMundo(jardin.second, 16.0)))
        assertEquals(28825, ProyeccionMapa.mosaico(ProyeccionMapa.yMundo(jardin.first, 16.0)))
    }

    @Test
    fun `encuadrar deja ambos puntos dentro del area con margen`() {
        val vista = ProyeccionMapa.encuadrar(listOf(jardin, mausoleo), 600.0, 400.0, 50.0)
        assertNotNull(vista)
        vista!!
        listOf(jardin, mausoleo).forEach { (lat, lon) ->
            val dx = ProyeccionMapa.xMundo(lon, vista.zoom) - ProyeccionMapa.xMundo(vista.lon, vista.zoom)
            val dy = ProyeccionMapa.yMundo(lat, vista.zoom) - ProyeccionMapa.yMundo(vista.lat, vista.zoom)
            assertTrue("x=$dx", kotlin.math.abs(dx) <= 300 - 50 + 1)
            assertTrue("y=$dy", kotlin.math.abs(dy) <= 200 - 50 + 1)
        }
    }

    @Test
    fun `con un solo punto se usa un zoom cercano`() {
        assertEquals(16.0, ProyeccionMapa.encuadrar(listOf(jardin), 600.0, 400.0, 50.0)!!.zoom, 0.0)
    }

    @Test
    fun `distancia a la ruta y lo que falta por recorrer`() {
        // Ruta recta hacia el oeste sobre la misma latitud, 3 tramos de ~0.001° (~104 m cada uno).
        val ruta = listOf(21.1575 to -100.9340, 21.1575 to -100.9350, 21.1575 to -100.9360, 21.1575 to -100.9370)
        assertEquals(0.0, RutaEnAuto.distanciaARutaM(21.1575, -100.9355, ruta), 1.0)
        assertEquals(111.0, RutaEnAuto.distanciaARutaM(21.1585, -100.9355, ruta), 2.0) // 0.001° de latitud al norte
        val total = RutaEnAuto.restanteM(21.1575, -100.9340, ruta)
        val mitad = RutaEnAuto.restanteM(21.1575, -100.9355, ruta)
        assertEquals(311.0, total, 3.0)
        assertEquals(155.0, mitad, 3.0)
    }

    @Test
    fun `minutos en auto a velocidad de ciudad`() {
        assertEquals(1, RutaEnAuto.minutosEnAuto(100))
        assertEquals(4, RutaEnAuto.minutosEnAuto(1900)) // 1.9 km a 30 km/h = 3.8 min
    }
}

package mx.utng.festivaltrack.app.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RutaPeatonalTest {

    @Test
    fun `lee la ruta por calles de OSRM con latitud y longitud en orden`() {
        val json = """
            {"code":"Ok","routes":[{"distance":797.1,"duration":638,
             "geometry":{"type":"LineString","coordinates":[[-100.9338,21.1555],[-100.9370,21.1560],[-100.9406,21.1571]]}}]}
        """.trimIndent()
        val ruta = RutaPeatonal.parsear(json)!!
        assertTrue(ruta.porCalles)
        assertEquals(797, ruta.distanciaM)
        assertEquals(10, ruta.minutos) // 797 m a 80 m/min
        assertEquals(21.1555 to -100.9338, ruta.puntos.first())
        assertEquals(21.1571 to -100.9406, ruta.puntos.last())
        assertEquals("797 m · 10 min a pie", ruta.resumen)
    }

    @Test
    fun `sin ruta valida regresa null`() {
        assertNull(RutaPeatonal.parsear("""{"code":"NoRoute","routes":[]}"""))
    }

    @Test
    fun `la linea recta usa la distancia real entre los dos puntos`() {
        // Jardín Principal → Mausoleo de José Alfredo: ~720 m en línea recta
        val ruta = RutaPeatonal.lineaRecta(21.1563, -100.9338, 21.1571, -100.9406)
        assertFalse(ruta.porCalles)
        assertTrue(ruta.distanciaM in 690..740)
        assertEquals(2, ruta.puntos.size)
        assertEquals(RutaPeatonal.minutosCaminando(ruta.distanciaM), ruta.minutos)
    }

    @Test
    fun `distancias largas se muestran en kilometros`() {
        val ruta = Ruta(listOf(0.0 to 0.0, 0.0 to 0.1), 1450, 19, true)
        assertEquals("1.5 km · 19 min a pie", ruta.resumen)
    }
}

package mx.utng.festivaltrack.tv.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortadaConciertoTest {

    @Test
    fun `los conciertos de la cartelera tienen cada uno su foto`() {
        // Los cinco conciertos de seed-cartelera (los mismos que hay en Render).
        val fondos = listOf(
            fondoDe("1", "Noche de Bohemia en la Plaza", "Plaza Principal", "Jardín Principal"),
            fondoDe("2", "Serenata en el Mausoleo", "Mausoleo de José Alfredo Jiménez", "Panteón Municipal"),
            fondoDe("3", "Recorrido Casa Museo José Alfredo", "Casa Museo", "Calle Guanajuato 13"),
            fondoDe("4", "Gran Gala Mariachi", "Escenario Principal", "Jardín Principal"),
            fondoDe("5", "Homenaje: El Rey", "Escenario Principal", "Jardín Principal")
        )
        assertEquals(
            listOf(
                FondoConcierto.BOHEMIA, FondoConcierto.MAUSOLEO, FondoConcierto.CASA_MUSEO,
                FondoConcierto.MARIACHI, FondoConcierto.EL_REY
            ),
            fondos
        )
    }

    @Test
    fun `sin palabras clave se usa el lugar sin importar acentos ni mayusculas`() {
        assertEquals(FondoConcierto.MAUSOLEO, fondoDe("x", "Serenata", null, "PANTEÓN MUNICIPAL"))
        assertEquals(FondoConcierto.PLAZA, fondoDe("x", "Concierto de la tarde", null, "Jardín Principal"))
        assertEquals(FondoConcierto.MARIACHI, fondoDe("x", "Concierto", "Escenario Principal", "Centro"))
    }

    @Test
    fun `rey solo cuenta como palabra completa`() {
        assertEquals(FondoConcierto.PLAZA, fondoDe("x", "Reyes de la canción", "Plaza Principal", ""))
    }

    @Test
    fun `un concierto sin pistas siempre recibe la misma foto general`() {
        val primera = fondoDe("EVT-777", "Concierto especial", null, "Teatro")
        repeat(3) { assertEquals(primera, fondoDe("EVT-777", "Concierto especial", null, "Teatro")) }
    }

    @Test
    fun `la url del banner se completa con el servidor cuando es relativa`() {
        assertEquals(
            "https://festivaltrack-backend.onrender.com/uploads/gala.jpg",
            urlBanner("/uploads/gala.jpg", "https://festivaltrack-backend.onrender.com/")
        )
        assertEquals("https://cdn.mx/a.jpg", urlBanner(" https://cdn.mx/a.jpg ", "http://10.0.2.2:3001"))
        assertNull(urlBanner(null, "http://10.0.2.2:3001"))
        assertNull(urlBanner("  ", "http://10.0.2.2:3001"))
    }
}

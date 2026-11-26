package mx.utng.festivaltrack.shared.util

import org.junit.Assert.assertEquals
import org.junit.Test

class EnlacesWebTest {

    @Test
    fun `usa la web configurada sin diagonal final`() {
        assertEquals("https://festival.mx", EnlacesWeb.baseDesde("https://festival.mx/", "", "10.0.2.2"))
    }

    @Test
    fun `sin configurar usa la PC del backend en el puerto de Vite`() {
        assertEquals("http://192.168.1.20:5173", EnlacesWeb.baseDesde("", "", "192.168.1.20"))
    }

    @Test
    fun `si la API apunta a una IP local usa esa misma PC`() {
        assertEquals("http://192.168.0.7:5173", EnlacesWeb.baseDesde("", "http://192.168.0.7:3001/api/v1/", "10.0.2.2"))
    }

    @Test
    fun `una API en la nube no se usa como web`() {
        assertEquals(
            "http://192.168.1.20:5173",
            EnlacesWeb.baseDesde("", "https://festivaltrack-backend.up.railway.app/api/v1/", "192.168.1.20")
        )
    }
}

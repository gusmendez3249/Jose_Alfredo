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

    @Test
    fun `con la API de Render usa la web publicada en Render`() {
        assertEquals(
            "https://festivaltrack-web.onrender.com",
            EnlacesWeb.baseDesde("", "https://festivaltrack-backend.onrender.com/api/v1/", "10.0.2.2")
        )
        assertEquals(
            "https://festivaltrack-web.onrender.com",
            EnlacesWeb.baseDesde("", "https://FestivalTrack-Backend.onrender.com", "10.0.2.2")
        )
    }

    @Test
    fun `la web configurada gana sobre la de Render`() {
        assertEquals(
            "https://boletos.festival.mx",
            EnlacesWeb.baseDesde("https://boletos.festival.mx", "https://festivaltrack-backend.onrender.com/api/v1", "10.0.2.2")
        )
    }

    @Test
    fun `el enlace para vincular la TV lleva el codigo`() {
        assertEquals(
            "https://festivaltrack-web.onrender.com/vincular-tv?codigo=KX7M-3QPA",
            EnlacesWeb.vincularTvDesde("https://festivaltrack-web.onrender.com", "KX7M-3QPA")
        )
    }

    @Test
    fun `otro servicio de Render que no es el backend no se adivina`() {
        assertEquals(
            "http://192.168.1.20:5173",
            EnlacesWeb.baseDesde("", "https://otra-api.onrender.com/api/v1/", "192.168.1.20")
        )
    }
}

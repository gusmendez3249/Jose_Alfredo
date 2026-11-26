package mx.utng.festivaltrack.app.ui.viewmodels

import org.junit.Assert.assertEquals
import org.junit.Test

class VincularTvTest {

    @Test
    fun `el codigo se escribe como lo muestra la TV`() {
        assertEquals("", limpiarCodigoTv(""))
        assertEquals("KX7", limpiarCodigoTv("kx7"))
        assertEquals("KX7M", limpiarCodigoTv("kx7m"))
        assertEquals("KX7M-3", limpiarCodigoTv("kx7m3"))
        assertEquals("KX7M-3QPA", limpiarCodigoTv("kx7m3qpa"))
    }

    @Test
    fun `ignora espacios, guiones y signos y no pasa de 8 caracteres`() {
        assertEquals("KX7M-3QPA", limpiarCodigoTv(" KX7M - 3QPA "))
        assertEquals("KX7M-3QPA", limpiarCodigoTv("KX7M-3QPA-EXTRA"))
        assertEquals("AB12", limpiarCodigoTv("a.b!1?2"))
    }

    @Test
    fun `cada error del servidor tiene su mensaje`() {
        assertEquals("Ese código no existe o ya venció. Revisa el que muestra la TV.", mensajeErrorVincularTv(404))
        assertEquals("Tu sesión expiró. Inicia sesión de nuevo.", mensajeErrorVincularTv(401))
        assertEquals("Demasiados intentos. Espera un minuto e intenta de nuevo.", mensajeErrorVincularTv(429))
        assertEquals("No se pudo vincular la TV. Intenta de nuevo.", mensajeErrorVincularTv(500))
    }
}

package mx.utng.festivaltrack.app.auto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class NavegacionCalculatorTest {

    private val ahora = Instant.parse("2026-11-20T18:00:00Z")
    private val mausoleo = RutaActiva("Mausoleo", 21.1571, -100.9406)

    @Test
    fun `el GPS envia la ruta activa a la pantalla del auto como uri geo`() {
        val resumen = NavegacionCalculator.resumen(mausoleo, 21.1530, -100.9340, ahora)
        assertEquals("Mausoleo", resumen.destino)
        assertEquals("geo:21.1571,-100.9406", resumen.uriNavegacion)
    }

    @Test
    fun `distancia y tiempo coinciden con el calculo de la alerta de salida`() {
        val resumen = NavegacionCalculator.resumen(mausoleo, 21.1530, -100.9340, ahora)
        val km = SalidaCalculator.distanciaKm(21.1530, -100.9340, 21.1571, -100.9406)
        assertEquals(km, resumen.distanciaKm, 1e-9)
        assertEquals(SalidaCalculator.trasladoMin(km), resumen.trasladoMin)
    }

    @Test
    fun `la llegada es ahora mas el tiempo de traslado`() {
        val resumen = NavegacionCalculator.resumen(mausoleo, 21.1530, -100.9340, ahora)
        assertEquals(ahora.plus(Duration.ofMinutes(resumen.trasladoMin.toLong())), resumen.llegada)
    }

    @Test
    fun `estando en el destino el traslado es de al menos un minuto`() {
        val resumen = NavegacionCalculator.resumen(mausoleo, mausoleo.latitud, mausoleo.longitud, ahora)
        assertEquals(0.0, resumen.distanciaKm, 1e-9)
        assertEquals(1, resumen.trasladoMin)
    }

    @Test
    fun `texto de tiempo en minutos y horas`() {
        assertEquals("8 min", NavegacionCalculator.textoTiempo(8))
        assertEquals("1 h", NavegacionCalculator.textoTiempo(60))
        assertEquals("1 h 5 min", NavegacionCalculator.textoTiempo(65))
    }

    @Test
    fun `el aviso por voz menciona destino y minutos`() {
        val resumen = NavegacionCalculator.resumen(mausoleo, 21.1530, -100.9340, ahora)
        val voz = NavegacionCalculator.textoVoz(resumen)
        assertTrue(voz.startsWith("Iniciando navegación a Mausoleo."))
        assertTrue(voz.contains("${resumen.trasladoMin} minutos"))
    }
}

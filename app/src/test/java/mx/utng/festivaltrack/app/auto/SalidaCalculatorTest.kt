package mx.utng.festivaltrack.app.auto

import mx.utng.festivaltrack.shared.data.remote.EventoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SalidaCalculatorTest {

    private val ahora = Instant.parse("2026-11-20T19:00:00Z")
    private val centroLat = 21.1561
    private val centroLon = -100.9318

    private fun evento(
        id: String,
        fecha: String,
        lat: Double? = 21.1600,
        lon: Double? = -100.9350,
        estado: String = "PROGRAMADO"
    ) = EventoDto(id, "Evento $id", fecha, "Plaza Principal", "Escenario Principal", estado, lat, lon, null, null)

    @Test
    fun `distancia entre el mismo punto es cero`() {
        assertEquals(0.0, SalidaCalculator.distanciaKm(centroLat, centroLon, centroLat, centroLon), 0.0001)
    }

    @Test
    fun `distancia de un grado de latitud es de unos 111 km`() {
        assertEquals(111.2, SalidaCalculator.distanciaKm(21.0, -100.0, 22.0, -100.0), 0.5)
    }

    @Test
    fun `traslado aplica factor de ruta y velocidad promedio`() {
        // 10 km * 1.3 = 13 km a 30 km/h = 26 min
        assertEquals(26, SalidaCalculator.trasladoMin(10.0))
        assertEquals(1, SalidaCalculator.trasladoMin(0.0))
    }

    @Test
    fun `descarta eventos pasados y finalizados y ordena por hora`() {
        val eventos = listOf(
            evento("tarde", "2026-11-20T23:00:00.000Z"),
            evento("pasado", "2026-11-20T18:00:00.000Z"),
            evento("finalizado", "2026-11-20T22:00:00.000Z", estado = "FINALIZADO"),
            evento("pronto", "2026-11-20T21:00:00.000Z")
        )
        val resultado = SalidaCalculator.calcular(eventos, centroLat, centroLon, ahora)
        assertEquals(listOf("pronto", "tarde"), resultado.map { it.evento.id })
    }

    @Test
    fun `minutos para salir restan traslado y margen`() {
        val salida = SalidaCalculator.calcular(
            listOf(evento("e", "2026-11-20T19:30:00Z", lat = centroLat, lon = centroLon)),
            centroLat, centroLon, ahora
        ).single()
        // 30 min para el evento - 1 min de traslado - 10 min de margen
        assertEquals(19L, salida.minutosParaSalir)
    }

    @Test
    fun `evento sin coordenadas no tiene distancia ni traslado`() {
        val salida = SalidaCalculator.calcular(
            listOf(evento("e", "2026-11-20T21:00:00Z", lat = null, lon = null)),
            centroLat, centroLon, ahora
        ).single()
        assertNull(salida.distanciaKm)
        assertEquals(0, salida.trasladoMin)
    }

    @Test
    fun `acepta fechas con desfase horario`() {
        val resultado = SalidaCalculator.calcular(
            listOf(evento("e", "2026-11-20T15:00:00-06:00")), centroLat, centroLon, ahora
        )
        assertTrue(resultado.isNotEmpty())
    }

    @Test
    fun `texto de salida`() {
        assertEquals("Sal ya", SalidaCalculator.textoSalida(0))
        assertEquals("Sal ya", SalidaCalculator.textoSalida(-5))
        assertEquals("Sal en 25 min", SalidaCalculator.textoSalida(25))
        assertEquals("Sal en 1 h", SalidaCalculator.textoSalida(60))
        assertEquals("Sal en 1 h 20 min", SalidaCalculator.textoSalida(80))
    }
}

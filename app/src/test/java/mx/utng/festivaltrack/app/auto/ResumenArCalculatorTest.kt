package mx.utng.festivaltrack.app.auto

import mx.utng.festivaltrack.shared.data.remote.SitioResumenDto
import mx.utng.festivaltrack.shared.data.remote.VisitaSitioDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ResumenArCalculatorTest {

    private val zona = ZoneId.of("America/Mexico_City")
    private val ahora = Instant.parse("2026-11-20T22:00:00Z") // 16:00 en Dolores Hidalgo

    private fun visita(sitioId: String, nombre: String, fecha: String) =
        VisitaSitioDto(
            id = "$sitioId-$fecha",
            fechaVisita = fecha,
            confianza = 0.9,
            sitio = SitioResumenDto(sitioId, nombre, "Centro", null)
        )

    @Test
    fun `solo cuenta los sitios visitados hoy en la zona local`() {
        val resumen = ResumenArCalculator.deHoy(
            listOf(
                visita("parroquia", "Parroquia", "2026-11-20T17:00:00.000Z"),
                // 19 nov 23:30 en México aunque en UTC ya sea 20 nov
                visita("museo", "Museo", "2026-11-20T05:30:00.000Z")
            ),
            ahora, zona
        )
        assertEquals(listOf("Parroquia"), resumen.nombres)
    }

    @Test
    fun `un sitio reconocido varias veces cuenta una vez y el mas reciente va primero`() {
        val resumen = ResumenArCalculator.deHoy(
            listOf(
                visita("parroquia", "Parroquia", "2026-11-20T16:00:00.000Z"),
                visita("mausoleo", "Mausoleo", "2026-11-20T20:00:00.000Z"),
                visita("parroquia", "Parroquia", "2026-11-20T18:00:00.000Z")
            ),
            ahora, zona
        )
        assertEquals(listOf("Mausoleo", "Parroquia"), resumen.nombres)
    }

    @Test
    fun `ignora fechas que no se pueden leer`() {
        val resumen = ResumenArCalculator.deHoy(listOf(visita("x", "X", "ayer")), ahora, zona)
        assertEquals(0, resumen.total)
    }

    @Test
    fun `textos de la tarjeta segun cuantos sitios hay`() {
        assertEquals("Aún no descubres sitios hoy", ResumenArCalculator.texto(ResumenAr(emptyList())))
        assertEquals("1 sitio hoy: Parroquia", ResumenArCalculator.texto(ResumenAr(listOf("Parroquia"))))
        assertEquals("2 sitios hoy: A, B", ResumenArCalculator.texto(ResumenAr(listOf("A", "B"))))
        assertEquals("4 sitios hoy: A, B y 2 más", ResumenArCalculator.texto(ResumenAr(listOf("A", "B", "C", "D"))))
    }
}

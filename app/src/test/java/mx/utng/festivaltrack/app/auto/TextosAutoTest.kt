package mx.utng.festivaltrack.app.auto

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class TextosAutoTest {

    /** Miércoles 25 de noviembre, 22:20 en Dolores Hidalgo (ya es jueves en UTC). */
    private val ahora = Instant.parse("2026-11-26T04:20:00Z")

    @Test
    fun `el dia se cuenta en la hora de Dolores Hidalgo y no en UTC`() {
        assertEquals("hoy", TextosAuto.dia(Instant.parse("2026-11-26T05:30:00Z"), ahora)) // 23:30 del miércoles
        assertEquals("mañana", TextosAuto.dia(Instant.parse("2026-11-27T03:30:00Z"), ahora)) // jueves 21:30
        assertEquals("vie 27 nov", TextosAuto.dia(Instant.parse("2026-11-28T03:00:00Z"), ahora))
        assertEquals("sáb 28 nov", TextosAuto.dia(Instant.parse("2026-11-29T03:00:00Z"), ahora))
    }

    @Test
    fun `cuando empieza un evento`() {
        assertEquals("Mañana 21:30 hrs", TextosAuto.cuando(Instant.parse("2026-11-27T03:30:00Z"), ahora))
        assertEquals("Vie 27 nov 21:00 hrs", TextosAuto.cuando(Instant.parse("2026-11-28T03:00:00Z"), ahora))
    }

    @Test
    fun `el inicio no repite el dia si es el mismo que el de la salida`() {
        val inicio = Instant.parse("2026-11-27T03:30:00Z") // jueves 21:30
        assertEquals("inicia 21:30 hrs", TextosAuto.inicia(inicio, Instant.parse("2026-11-27T03:16:00Z"), ahora))
        val pasadaMedianoche = Instant.parse("2026-11-27T06:15:00Z") // viernes 00:15
        assertEquals(
            "inicia vie 27 nov 00:15 hrs",
            TextosAuto.inicia(pasadaMedianoche, Instant.parse("2026-11-27T05:50:00Z"), ahora)
        )
    }

    @Test
    fun `nombres cortos para el panel junto al mapa`() {
        assertEquals("Mausoleo", TextosAuto.nombreCorto("Mausoleo de José Alfredo Jiménez"))
        assertEquals("Casa Museo", TextosAuto.nombreCorto("Casa Museo José Alfredo Jiménez"))
        assertEquals("Parroquia de Dolores", TextosAuto.nombreCorto("Parroquia de Nuestra Señora de los Dolores"))
        assertEquals("Monumento Hidalgo", TextosAuto.nombreCorto("Monumento a Miguel Hidalgo"))
        assertEquals("Escenario Principal", TextosAuto.nombreCorto("Escenario Principal"))
        assertEquals("Mañana 21:30", TextosAuto.cuandoCorto(Instant.parse("2026-11-27T03:30:00Z"), ahora))
        assertEquals("1.5 km · 4 min", TextosAuto.trayecto(1.48, 4, corto = true))
    }

    @Test
    fun `distancia y trayecto`() {
        assertEquals("Estás aquí", TextosAuto.distancia(0.0))
        assertEquals("183 m", TextosAuto.distancia(0.183))
        assertEquals("1.5 km", TextosAuto.distancia(1.48))
        assertEquals("Estás aquí", TextosAuto.trayecto(0.01, 1))
        assertEquals("1.5 km · 4 min en auto", TextosAuto.trayecto(1.48, 4))
        // Sin GPS no se afirma "Estás aquí": se aclara que se mide desde el centro.
        assertEquals("En el centro de Dolores Hidalgo", TextosAuto.trayecto(0.0, 1, desdeCentro = true))
        assertEquals("1.5 km del centro · 4 min en auto", TextosAuto.trayecto(1.48, 4, desdeCentro = true))
    }
}

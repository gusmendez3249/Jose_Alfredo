package mx.utng.festivaltrack.wear.presentation.util

import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AgendaWearTest {

    private val ahora = Instant.parse("2026-11-26T00:00:00Z") // 25/11 18:00 en México

    private fun evento(id: String, fecha: String, estado: String = "PUBLICADO") =
        EventoEntity(id, "Concierto $id", fecha, "Jardín Principal", "Plaza Principal", null, estado, null, "José Alfredo Jiménez")

    @Test
    fun `la hora se muestra en hora de Mexico`() {
        assertEquals("20:00", AgendaWear.hora(evento("a", "2026-11-26T02:00:00Z")))
    }

    @Test
    fun `oculta conciertos terminados, cancelados y borradores`() {
        val lista = listOf(
            evento("pasado", "2026-11-21T01:00:00Z"),
            evento("cancelado", "2026-11-28T02:00:00Z", "CANCELADO"),
            evento("hoy", "2026-11-26T02:00:00Z"),
            evento("manana", "2026-11-27T03:30:00Z")
        )
        assertEquals(listOf("hoy", "manana"), AgendaWear.vigentes(lista, ahora).map { it.id })
    }

    @Test
    fun `faltan con singular y plural`() {
        assertEquals("Faltan 1 día y 3 h", AgendaWear.faltan(evento("a", "2026-11-27T03:00:00Z"), ahora))
        assertEquals("Faltan 2 días y 0 h", AgendaWear.faltan(evento("a", "2026-11-28T00:00:00Z"), ahora))
        assertEquals("Faltan 2 h 0 min", AgendaWear.faltan(evento("a", "2026-11-26T02:00:00Z"), ahora))
        assertEquals("En curso", AgendaWear.faltan(evento("a", "2026-11-25T23:00:00Z"), ahora))
    }

    @Test
    fun `distancia y direccion del Jardin al Mausoleo`() {
        val m = AgendaWear.distanciaM(21.157511, -100.934529, 21.156687, -100.948773)
        assertTrue(m in 1400.0..1500.0)
        assertEquals("al oeste", AgendaWear.direccion(21.157511, -100.934529, 21.156687, -100.948773))
        assertEquals("1.5 km", AgendaWear.textoDistancia(1480.0))
    }

    @Test
    fun `agrupa la agenda por Hoy, Manana y fecha`() {
        val lista = listOf(
            evento("hoy", "2026-11-26T02:00:00Z"),      // 25/11 20:00
            evento("manana", "2026-11-27T03:30:00Z"),   // 26/11 21:30
            evento("viernes", "2026-11-27T17:00:00Z")   // 27/11 11:00
        )
        val grupos = AgendaWear.porDia(lista, ahora)
        assertEquals(listOf("Hoy", "Mañana", "Vie 27 nov"), grupos.map { it.first })
    }
}

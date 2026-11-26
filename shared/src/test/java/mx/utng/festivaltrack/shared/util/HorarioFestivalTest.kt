package mx.utng.festivaltrack.shared.util

import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HorarioFestivalTest {

    private fun evento(id: String, fecha: String, estado: String = "PUBLICADO") =
        EventoEntity(id, "Concierto $id", fecha, "Jardín Principal", "Plaza Principal", null, estado, null, null)

    // 25/11/2026 13:00 en México (19:00 UTC)
    private val ahora = Instant.parse("2026-11-25T19:00:00Z")

    @Test
    fun `la hora sale en hora de Mexico y no en UTC`() {
        // 02:00 UTC del 26 = 20:00 del 25 en México
        val e = evento("a", "2026-11-26T02:00:00.000Z")
        assertEquals("20:00", HorarioFestival.hora(e))
        assertEquals("miércoles 25 de noviembre", HorarioFestival.fechaLarga(e))
        assertEquals("Hoy a las 20:00 hrs", HorarioFestival.cuando(e, ahora))
    }

    @Test
    fun `manana y otros dias`() {
        assertEquals("Mañana a las 21:30 hrs", HorarioFestival.cuando(evento("b", "2026-11-27T03:30:00.000Z"), ahora))
        assertEquals("viernes 27 de noviembre a las 11:00 hrs", HorarioFestival.cuando(evento("c", "2026-11-27T17:00:00.000Z"), ahora))
    }

    @Test
    fun `proximos quita cancelados y pasados y ordena por fecha`() {
        val lista = listOf(
            evento("tarde", "2026-11-28T03:00:00.000Z"),
            evento("cancelado", "2026-11-26T02:00:00.000Z", estado = "CANCELADO"),
            evento("pasado", "2026-11-20T02:00:00.000Z"),
            evento("pronto", "2026-11-26T02:00:00.000Z"),
            evento("enCurso", "2026-11-25T17:30:00.000Z")
        )
        assertEquals(listOf("enCurso", "pronto", "tarde"), HorarioFestival.proximos(lista, ahora).map { it.id })
    }

    @Test
    fun `en curso solo durante las horas del concierto`() {
        assertTrue(HorarioFestival.enCurso(evento("x", "2026-11-25T17:30:00.000Z"), ahora))
        assertFalse(HorarioFestival.enCurso(evento("y", "2026-11-26T02:00:00.000Z"), ahora))
        assertFalse(HorarioFestival.enCurso(evento("z", "2026-11-25T10:00:00.000Z"), ahora))
    }

    @Test
    fun `fecha invalida no rompe`() {
        val e = evento("mal", "no-es-fecha")
        assertEquals("", HorarioFestival.hora(e))
        assertEquals("", HorarioFestival.cuando(e, ahora))
        assertTrue(HorarioFestival.proximos(listOf(e), ahora).isEmpty())
    }
}

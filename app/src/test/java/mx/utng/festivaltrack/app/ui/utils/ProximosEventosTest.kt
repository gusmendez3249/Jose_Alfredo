package mx.utng.festivaltrack.app.ui.utils

import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ProximosEventosTest {

    private val ahora = Instant.parse("2026-11-25T18:00:00Z")

    private fun evento(id: String, fecha: String, estado: String = "PUBLICADO") =
        EventoEntity(id, "Evento $id", fecha, "Plaza", null, null, estado, null, null)

    @Test
    fun `oculta los eventos que ya terminaron`() {
        val lista = listOf(
            evento("pasado", "2026-11-20T01:00:00Z"),
            evento("futuro", "2026-11-28T02:00:00Z")
        )
        assertEquals(listOf("futuro"), ProximosEventos.filtrar(lista, ahora).map { it.id })
    }

    @Test
    fun `un evento que ya empezo sigue visible mientras dura`() {
        val lista = listOf(evento("en-curso", "2026-11-25T16:00:00Z"))
        assertEquals(listOf("en-curso"), ProximosEventos.filtrar(lista, ahora).map { it.id })
    }

    @Test
    fun `oculta finalizados, cancelados y borradores aunque sean futuros`() {
        val lista = listOf(
            evento("fin", "2026-12-01T02:00:00Z", "FINALIZADO"),
            evento("cancel", "2026-12-01T02:00:00Z", "CANCELADO"),
            evento("borrador", "2026-12-01T02:00:00Z", "BORRADOR"),
            evento("ok", "2026-12-01T02:00:00Z", "PUBLICADO")
        )
        assertEquals(listOf("ok"), ProximosEventos.filtrar(lista, ahora).map { it.id })
    }

    @Test
    fun `ordena del mas proximo al mas lejano e ignora fechas invalidas`() {
        val lista = listOf(
            evento("b", "2026-12-05T02:00:00Z"),
            evento("x", "fecha-rota"),
            evento("a", "2026-11-27T02:00:00Z")
        )
        assertEquals(listOf("a", "b"), ProximosEventos.filtrar(lista, ahora).map { it.id })
    }

    @Test
    fun `dia y mes en hora de Mexico`() {
        // 27/11 a las 02:00 UTC es el 26/11 a las 20:00 en Ciudad de México
        val e = evento("a", "2026-11-27T02:00:00Z")
        assertEquals("26", ProximosEventos.dia(e))
        assertEquals("NOV", ProximosEventos.mesCorto(e))
        assertEquals("20:00", ProximosEventos.hora(e))
    }
}

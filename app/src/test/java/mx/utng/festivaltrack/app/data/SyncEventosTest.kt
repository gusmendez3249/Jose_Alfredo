package mx.utng.festivaltrack.app.data

import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.repository.FestivalRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/** Regla de limpieza de Room después de `syncEventos()`. */
class SyncEventosTest {

    private fun evento(id: String, nombre: String = "Evento $id", fecha: String = "2026-11-27T02:00:00.000Z") =
        EventoEntity(id, nombre, fecha, "Plaza", null, null, "PUBLICADO", null, null)

    private fun aEliminar(locales: List<EventoEntity>, remotos: List<EventoEntity>) =
        FestivalRepository.eventosLocalesAEliminar(locales, remotos).map { it.id }.sorted()

    @Test
    fun `no borra eventos del servidor con id cuid`() {
        val delServidor = evento("cmw1abcde0000xyz12345abcd")
        assertEquals(emptyList<String>(), aEliminar(listOf(delServidor), listOf(delServidor)))
    }

    @Test
    fun `borra los que el servidor ya no tiene`() {
        val viejoSeed = evento("EVT-001")
        val viejoCuid = evento("cmw1zzzzz0000xyz12345abcd")
        val vigente = evento("cmw1abcde0000xyz12345abcd")
        assertEquals(listOf("EVT-001", "cmw1zzzzz0000xyz12345abcd"), aEliminar(listOf(viejoSeed, viejoCuid, vigente), listOf(vigente)))
    }

    @Test
    fun `conserva el borrador local que aun no se sube`() {
        val borrador = evento("3f2b9c1e-8a4d-4c6e-9b1a-2d3e4f5a6b7c", nombre = "Nuevo concierto")
        assertEquals(emptyList<String>(), aEliminar(listOf(borrador), emptyList()))
    }

    @Test
    fun `borra el borrador local cuando ya llego del servidor con su id`() {
        val borrador = evento("3f2b9c1e-8a4d-4c6e-9b1a-2d3e4f5a6b7c", nombre = "Nuevo concierto")
        val subido = evento("cmw1abcde0000xyz12345abcd", nombre = "Nuevo concierto")
        assertEquals(listOf("3f2b9c1e-8a4d-4c6e-9b1a-2d3e4f5a6b7c"), aEliminar(listOf(borrador, subido), listOf(subido)))
    }
}

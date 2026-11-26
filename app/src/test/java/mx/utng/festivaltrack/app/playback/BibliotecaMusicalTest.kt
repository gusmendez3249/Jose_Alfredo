package mx.utng.festivaltrack.app.playback

import mx.utng.festivaltrack.shared.data.remote.EventoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class BibliotecaMusicalTest {

    private val ahora = Instant.parse("2026-11-26T04:20:00Z")

    private fun evento(id: String, fecha: String, estado: String = "PUBLICADO") =
        EventoDto(id, "Evento $id", fecha, "Jardín", "Escenario", estado, null, null, null, null)

    @Test
    fun `el id de cada cancion dice a que carpeta pertenece`() {
        val id = BibliotecaMusical.idPista("evento:abc", "123")
        assertEquals("evento:abc/123", id)
        assertEquals("evento:abc", BibliotecaMusical.carpetaDe(id))
        assertEquals(BibliotecaMusical.ID_DISCOGRAFIA, BibliotecaMusical.carpetaDe("discografia/9"))
        assertNull(BibliotecaMusical.carpetaDe("123")) // canción puesta desde el teléfono
    }

    @Test
    fun `solo ofrece conciertos que no han empezado, del mas proximo al mas lejano`() {
        val proximos = BibliotecaMusical.proximos(
            listOf(
                evento("tarde", "2026-11-29T03:00:00Z"),
                evento("pasado", "2026-11-26T02:00:00Z"),
                evento("finalizado", "2026-11-28T03:00:00Z", estado = "FINALIZADO"),
                evento("pronto", "2026-11-27T03:30:00Z")
            ),
            ahora
        )
        assertEquals(listOf("pronto", "tarde"), proximos.map { it.id })
    }

    @Test
    fun `texto de canciones`() {
        assertEquals("1 canción", BibliotecaMusical.textoCanciones(1))
        assertEquals("3 canciones", BibliotecaMusical.textoCanciones(3))
    }
}

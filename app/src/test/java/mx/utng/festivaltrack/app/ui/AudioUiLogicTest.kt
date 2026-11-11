package mx.utng.festivaltrack.app.ui

import mx.utng.festivaltrack.app.ui.utils.ProgressFileBody
import mx.utng.festivaltrack.app.ui.utils.SongUpload
import mx.utng.festivaltrack.app.ui.viewmodels.RepertoireSummary
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AudioUiLogicTest {

    private fun cancion(id: String, portada: String?) = CancionDto(
        id = id, titulo = "Canción $id", artista = "José Alfredo Jiménez", duracion = 180,
        archivoUrl = "https://cdn.ejemplo.mx/$id.mp3", genero = "Ranchera", portadaUrl = portada
    )

    // ─── Tarjeta del concierto ───────────────────────────────────────────────

    @Test
    fun `el resumen cuenta las demos y toma hasta cuatro portadas distintas`() {
        val canciones = listOf(
            cancion("1", "https://cdn.ejemplo.mx/a.jpg"),
            cancion("2", "https://cdn.ejemplo.mx/a.jpg"), // portada repetida
            cancion("3", null),
            cancion("4", "https://cdn.ejemplo.mx/b.jpg"),
            cancion("5", "https://cdn.ejemplo.mx/c.jpg"),
            cancion("6", "https://cdn.ejemplo.mx/d.jpg"),
            cancion("7", "https://cdn.ejemplo.mx/e.jpg")
        )

        val resumen = RepertoireSummary.from(canciones)

        assertEquals(7, resumen.songCount)
        assertEquals(
            listOf("a", "b", "c", "d").map { "https://cdn.ejemplo.mx/$it.jpg" },
            resumen.covers
        )
    }

    @Test
    fun `un concierto sin repertorio da un resumen vacio`() {
        val resumen = RepertoireSummary.from(emptyList())
        assertEquals(0, resumen.songCount)
        assertTrue(resumen.covers.isEmpty())
    }

    // ─── Formulario de subir canción ─────────────────────────────────────────

    @Test
    fun `no se puede publicar sin titulo`() {
        assertNotNull(SongUpload.validate(title = "   ", audioBytes = 1_000, coverBytes = null))
    }

    @Test
    fun `no se puede publicar un audio de mas de 30 MB`() {
        assertNotNull(SongUpload.validate("El Rey", audioBytes = SongUpload.MAX_BYTES + 1, coverBytes = null))
        assertNull(SongUpload.validate("El Rey", audioBytes = SongUpload.MAX_BYTES, coverBytes = null))
    }

    @Test
    fun `se puede publicar sin audio propio ni portada`() {
        assertNull(SongUpload.validate("El Rey", audioBytes = null, coverBytes = null))
    }

    @Test
    fun `el avance total reparte la barra entre audio portada y guardado`() {
        assertEquals(0f, SongUpload.overallProgress(SongUpload.Stage.AUDIO, 0f), 0.0001f)
        assertEquals(0.40f, SongUpload.overallProgress(SongUpload.Stage.AUDIO, 0.5f), 0.0001f)
        assertEquals(0.80f, SongUpload.overallProgress(SongUpload.Stage.COVER, 0f), 0.0001f)
        assertEquals(1f, SongUpload.overallProgress(SongUpload.Stage.SAVING, 1f), 0.0001f)
        assertEquals(0.80f, SongUpload.overallProgress(SongUpload.Stage.AUDIO, 7f), 0.0001f) // nunca se pasa de su etapa
    }

    @Test
    fun `el tamano del archivo se muestra en unidades legibles`() {
        assertEquals("512 B", SongUpload.formatSize(512))
        assertEquals("2 KB", SongUpload.formatSize(2_048))
        assertTrue(SongUpload.formatSize(5L * 1024 * 1024).startsWith("5"))
        assertTrue(SongUpload.formatSize(5L * 1024 * 1024).endsWith("MB"))
    }

    @Test
    fun `al subir un archivo se envia completo y el avance llega al cien por ciento`() {
        val archivo = File.createTempFile("prueba_", ".bin").apply { writeBytes(ByteArray(200_000) { (it % 251).toByte() }) }
        try {
            val avances = mutableListOf<Float>()
            val cuerpo = ProgressFileBody(archivo, null) { avances += it }
            val destino = Buffer()

            cuerpo.writeTo(destino)

            assertEquals(200_000L, cuerpo.contentLength())
            assertEquals(200_000L, destino.size)
            assertTrue("el avance debe ir creciendo", avances.zipWithNext().all { (a, b) -> b >= a })
            assertEquals(1f, avances.last(), 0.0001f)
        } finally {
            archivo.delete()
        }
    }
}

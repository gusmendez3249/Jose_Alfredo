package mx.utng.festivaltrack.app.playback

import mx.utng.festivaltrack.app.ui.screens.formatDuration
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueTrackTest {

    private val cancion = CancionDto(
        id = "c1",
        titulo = "El Rey",
        artista = "José Alfredo Jiménez",
        duracion = 185,
        archivoUrl = "https://cdn.ejemplo.mx/el-rey.mp3",
        genero = "Ranchera",
        portadaUrl = "https://cdn.ejemplo.mx/el-rey.jpg"
    )

    private val evento = EventoEntity(
        id = "EVT-5",
        nombre = "Homenaje: El Rey",
        fechaHora = "2026-11-21T20:00:00.000Z",
        ubicacion = "Plaza Principal",
        escenario = "Escenario Principal",
        bannerUrl = null,
        estado = "PUBLICADO",
        artistaId = null,
        artistaNombre = null
    )

    @Test
    fun `la pista lleva el escenario y el horario del concierto al que pertenece`() {
        val pista = QueueTrack.from(cancion, evento, "EVT-5")

        assertEquals("c1", pista.id)
        assertEquals("El Rey", pista.title)
        assertEquals("Homenaje: El Rey", pista.eventoNombre)
        assertEquals("Escenario Principal", pista.escenario)
        assertEquals("2026-11-21T20:00:00.000Z", pista.fechaHora)
        assertEquals("EVT-5", pista.eventoId)
    }

    @Test
    fun `las URL completas de audio y portada se conservan tal cual`() {
        val pista = QueueTrack.from(cancion, evento, "EVT-5")

        assertEquals("https://cdn.ejemplo.mx/el-rey.mp3", pista.audioUrl)
        assertEquals("https://cdn.ejemplo.mx/el-rey.jpg", pista.coverUrl)
        assertEquals(185, pista.durationSec)
    }

    @Test
    fun `una cancion sin portada no inventa una imagen`() {
        val pista = QueueTrack.from(cancion.copy(portadaUrl = null), evento, "EVT-5")
        assertNull(pista.coverUrl)
    }

    @Test
    fun `si todavia no carga el evento se usa un nombre generico`() {
        val pista = QueueTrack.from(cancion, null, "EVT-5")

        assertEquals("Concierto del festival", pista.eventoNombre)
        assertNull(pista.escenario)
        assertNull(pista.fechaHora)
    }

    @Test
    fun `la duracion se muestra como minutos y segundos`() {
        assertEquals("3:05", formatDuration(185))
        assertEquals("0:07", formatDuration(7))
        assertEquals("10:00", formatDuration(600))
        assertEquals("0:00", formatDuration(-4))
    }
}

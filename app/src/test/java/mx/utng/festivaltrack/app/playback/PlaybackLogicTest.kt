package mx.utng.festivaltrack.app.playback

import mx.utng.festivaltrack.app.playback.PlaybackLogic.ErrorAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlaybackLogicTest {

    // ─── Canción que falla ───────────────────────────────────────────────────

    @Test
    fun `si falla una cancion a media cola se salta a la siguiente`() {
        assertEquals(ErrorAction.SKIP_TO_NEXT, PlaybackLogic.afterError(queueSize = 3, currentIndex = 1, consecutiveErrors = 1))
    }

    @Test
    fun `si falla la ultima cancion se vuelve al inicio de la cola`() {
        assertEquals(ErrorAction.BACK_TO_START, PlaybackLogic.afterError(queueSize = 3, currentIndex = 2, consecutiveErrors = 1))
    }

    @Test
    fun `si la cola tiene una sola cancion y falla se detiene`() {
        assertEquals(ErrorAction.STOP, PlaybackLogic.afterError(queueSize = 1, currentIndex = 0, consecutiveErrors = 1))
    }

    @Test
    fun `si fallan seguidas todas las canciones se detiene en lugar de saltar sin fin`() {
        assertEquals(ErrorAction.SKIP_TO_NEXT, PlaybackLogic.afterError(queueSize = 3, currentIndex = 0, consecutiveErrors = 2))
        assertEquals(ErrorAction.STOP, PlaybackLogic.afterError(queueSize = 3, currentIndex = 1, consecutiveErrors = 3))
    }

    @Test
    fun `si se pierde la conexion no se salta ninguna cancion`() {
        assertEquals(
            ErrorAction.WAIT_FOR_NETWORK,
            PlaybackLogic.afterError(queueSize = 6, currentIndex = 2, consecutiveErrors = 1, networkError = true)
        )
        assertEquals(
            ErrorAction.WAIT_FOR_NETWORK,
            PlaybackLogic.afterError(queueSize = 1, currentIndex = 0, consecutiveErrors = 1, networkError = true)
        )
    }

    // ─── Orden aleatorio ─────────────────────────────────────────────────────

    @Test
    fun `al mezclar la cancion actual queda primero y no se pierde ninguna`() {
        val cola = listOf("a", "b", "c", "d", "e")

        val mezclada = PlaybackLogic.shuffled(cola, currentIndex = 2, random = Random(7))

        assertEquals("c", mezclada.first())
        assertEquals(cola.sorted(), mezclada.sorted())
    }

    @Test
    fun `mezclar una cola de una cancion la deja igual`() {
        val cola = listOf("a")
        assertSame(cola, PlaybackLogic.shuffled(cola, 0))
    }

    @Test
    fun `con un indice invalido se mezcla toda la cola sin perder canciones`() {
        val cola = listOf("a", "b", "c")
        assertEquals(cola, PlaybackLogic.shuffled(cola, currentIndex = -1, random = Random(1)).sorted())
    }

    // ─── Duración y avance de la demo ────────────────────────────────────────

    @Test
    fun `la demo dura lo que informa el reproductor`() {
        assertEquals(8_000L, PlaybackLogic.demoDurationMs(playerDurationMs = 8_000, trackDurationSec = 8, demoMs = 30_000))
    }

    @Test
    fun `mientras carga una cancion larga se estima en el tope de la demo`() {
        assertEquals(30_000L, PlaybackLogic.demoDurationMs(playerDurationMs = Long.MIN_VALUE + 1, trackDurationSec = 185, demoMs = 30_000))
    }

    @Test
    fun `mientras carga una cancion corta se estima en su duracion real`() {
        assertEquals(8_000L, PlaybackLogic.demoDurationMs(playerDurationMs = -1, trackDurationSec = 8, demoMs = 30_000))
    }

    @Test
    fun `sin duracion conocida se usa el tope de la demo`() {
        assertEquals(30_000L, PlaybackLogic.demoDurationMs(playerDurationMs = 0, trackDurationSec = 0, demoMs = 30_000))
    }

    @Test
    fun `el avance va de cero a uno y nunca se sale del rango`() {
        assertEquals(0.5f, PlaybackLogic.progress(15_000, 30_000), 0.0001f)
        assertEquals(1f, PlaybackLogic.progress(40_000, 30_000), 0.0001f)
        assertEquals(0f, PlaybackLogic.progress(-5, 30_000), 0.0001f)
        assertEquals(0f, PlaybackLogic.progress(1_000, 0), 0.0001f)
    }

    @Test
    fun `arrastrar la barra a la mitad de una demo corta salta a la mitad de esa demo`() {
        assertEquals(4_000L, PlaybackLogic.seekPositionMs(0.5f, 8_000))
        assertEquals(8_000L, PlaybackLogic.seekPositionMs(1.7f, 8_000))
        assertEquals(0L, PlaybackLogic.seekPositionMs(-1f, 8_000))
    }

    @Test
    fun `el tiempo se muestra en minutos y segundos`() {
        assertEquals("0:00", PlaybackLogic.formatMs(0))
        assertEquals("0:08", PlaybackLogic.formatMs(8_400))
        assertEquals("1:05", PlaybackLogic.formatMs(65_000))
        assertEquals("0:00", PlaybackLogic.formatMs(-300))
    }

    // ─── Reordenar el repertorio ─────────────────────────────────────────────

    @Test
    fun `mover una cancion la cambia de lugar sin perder las demas`() {
        assertEquals(listOf("b", "c", "a"), PlaybackLogic.move(listOf("a", "b", "c"), from = 0, to = 2))
        assertEquals(listOf("c", "a", "b"), PlaybackLogic.move(listOf("a", "b", "c"), from = 2, to = 0))
    }

    @Test
    fun `mover fuera de la lista no cambia nada`() {
        val lista = listOf("a", "b", "c")
        assertSame(lista, PlaybackLogic.move(lista, from = 0, to = 3))
        assertSame(lista, PlaybackLogic.move(lista, from = -1, to = 1))
        assertSame(lista, PlaybackLogic.move(lista, from = 1, to = 1))
        assertTrue(PlaybackLogic.move(emptyList<String>(), 0, 0).isEmpty())
    }
}

package mx.utng.festivaltrack.app.ui.viewmodels

import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import org.junit.Assert.assertEquals
import org.junit.Test

class EstadoTransmisionTest {

    @Test
    fun `sin transmision muestra el aviso con la fecha del concierto`() {
        val estado = LiveViewModel.estadoPara(StreamStatusDto(isLive = false), "evt1", "2026-11-27T03:00:00Z", "Gran Gala")
        assertEquals(EstadoTransmision.SinTransmision("2026-11-27T03:00:00Z", "Gran Gala"), estado)
    }

    @Test
    fun `en vivo del mismo concierto prueba primero la IP del celular y luego la del emulador`() {
        val status = StreamStatusDto("rtsp://192.168.1.20:1935", "rtsp://10.0.2.2:1935", true, eventoId = "evt1")
        assertEquals(
            EstadoTransmision.EnVivo(listOf("rtsp://192.168.1.20:1935", "rtsp://10.0.2.2:1935")),
            LiveViewModel.estadoPara(status, "evt1")
        )
    }

    @Test
    fun `no repite la direccion si ambas son iguales`() {
        val status = StreamStatusDto("rtsp://10.0.2.2:1935", "rtsp://10.0.2.2:1935", true)
        assertEquals(EstadoTransmision.EnVivo(listOf("rtsp://10.0.2.2:1935")), LiveViewModel.estadoPara(status, "evt1"))
    }

    @Test
    fun `si se transmite otro concierto lo indica`() {
        val status = StreamStatusDto("rtsp://10.0.2.2:1935", "", true, eventoId = "evt2", eventoNombre = "Homenaje: El Rey")
        assertEquals(EstadoTransmision.OtroConcierto("evt2", "Homenaje: El Rey"), LiveViewModel.estadoPara(status, "evt1"))
    }

    @Test
    fun `en vivo sin direccion se trata como sin transmision`() {
        val estado = LiveViewModel.estadoPara(StreamStatusDto("", "", true), "evt1")
        assertEquals(EstadoTransmision.SinTransmision(null, null), estado)
    }
}

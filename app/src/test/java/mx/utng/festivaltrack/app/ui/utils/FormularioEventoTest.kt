package mx.utng.festivaltrack.app.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class FormularioEventoTest {

    private val ahora = Instant.parse("2026-11-25T18:00:00Z")
    private val lugar = FormularioEvento.LUGARES.first()

    @Test
    fun `la hora de Mexico se envia en UTC`() {
        // 27/11 21:00 en Ciudad de México = 28/11 03:00 UTC
        assertEquals("2026-11-28T03:00:00Z", FormularioEvento.fechaIso(LocalDate.of(2026, 11, 27), LocalTime.of(21, 0)))
    }

    @Test
    fun `al editar se recupera la fecha y hora de Mexico`() {
        assertEquals(LocalDate.of(2026, 11, 27) to LocalTime.of(21, 0), FormularioEvento.desdeIso("2026-11-28T03:00:00.000Z"))
        assertNull(FormularioEvento.desdeIso("24 Nov 2024"))
    }

    @Test
    fun `formulario valido`() {
        assertNull(FormularioEvento.validar("Gala", LocalDate.of(2026, 11, 28), LocalTime.of(21, 0), lugar, "2500", false, ahora))
    }

    @Test
    fun `no deja crear conciertos en el pasado ni sin datos`() {
        assertEquals("La fecha y hora ya pasaron", FormularioEvento.validar("Gala", LocalDate.of(2026, 11, 20), LocalTime.of(21, 0), lugar, "100", false, ahora))
        assertEquals("Escribe el nombre del concierto", FormularioEvento.validar(" ", LocalDate.of(2026, 11, 28), LocalTime.of(21, 0), lugar, "100", false, ahora))
        assertEquals("Elige la fecha y la hora", FormularioEvento.validar("Gala", null, null, lugar, "100", false, ahora))
        assertEquals("Elige el lugar del concierto", FormularioEvento.validar("Gala", LocalDate.of(2026, 11, 28), LocalTime.of(21, 0), null, "100", false, ahora))
        assertEquals("La capacidad debe ser un número", FormularioEvento.validar("Gala", LocalDate.of(2026, 11, 28), LocalTime.of(21, 0), lugar, "mil", false, ahora))
    }

    @Test
    fun `al editar la capacidad vacia conserva la actual`() {
        assertNull(FormularioEvento.validar("Gala", LocalDate.of(2026, 11, 20), LocalTime.of(21, 0), lugar, "", true, ahora))
    }

    @Test
    fun `reconoce el lugar por nombre o por coordenadas`() {
        assertEquals("Casa Museo", FormularioEvento.lugarPara("casa museo", null, null)?.escenario)
        assertEquals("Mausoleo de José Alfredo Jiménez", FormularioEvento.lugarPara("Principal", 21.156687, -100.948773)?.escenario)
        assertNull(FormularioEvento.lugarPara("Otro", 20.0, -100.0))
    }
}

package mx.utng.festivaltrack.app.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class FormatoPagoTest {

    private val hoy = YearMonth.of(2026, 11)

    @Test
    fun `precio con separador de miles`() {
        assertEquals("$4,500", FormatoPago.precio(4500))
        assertEquals("$1,200", FormatoPago.precio(1200))
        assertEquals("$10,500", FormatoPago.precio(10500))
    }

    @Test
    fun `tarjeta valida`() {
        assertNull(FormatoPago.validar("Ana López", "4242424242424242", "1228", "123", hoy))
    }

    @Test
    fun `campos vacios`() {
        assertEquals("Llena todos los datos de la tarjeta", FormatoPago.validar("", "4242424242424242", "1228", "123", hoy))
    }

    @Test
    fun `tarjeta vencida o mes invalido`() {
        assertEquals("La tarjeta está vencida", FormatoPago.validar("Ana", "4242424242424242", "1025", "123", hoy))
        assertEquals("El mes de vencimiento no es válido", FormatoPago.validar("Ana", "4242424242424242", "1328", "123", hoy))
    }

    @Test
    fun `numero y cvv incompletos`() {
        assertEquals("El número de tarjeta debe tener 16 dígitos", FormatoPago.validar("Ana", "4242", "1228", "123", hoy))
        assertEquals("El CVV debe tener 3 o 4 dígitos", FormatoPago.validar("Ana", "4242424242424242", "1228", "12", hoy))
    }

    @Test
    fun `solo deja digitos`() {
        assertEquals("4242", FormatoPago.digitos("42-42 x", 16))
        assertEquals("1228", FormatoPago.digitos("12/289", 4))
    }
}

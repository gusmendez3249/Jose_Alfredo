package mx.utng.festivaltrack.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Base64

class TokenJwtTest {

    private fun jwt(payload: String): String {
        val b64 = { s: String -> Base64.getUrlEncoder().withoutPadding().encodeToString(s.toByteArray()) }
        return "${b64("""{"alg":"HS256","typ":"JWT"}""")}.${b64(payload)}.firma"
    }

    private val ahora = Instant.parse("2026-11-26T18:00:00Z")

    @Test
    fun `lee el vencimiento del token`() {
        val token = jwt("""{"sub":"u1","rol":"USUARIO","exp":${ahora.epochSecond + 3600}}""")
        assertEquals(ahora.plusSeconds(3600), TokenJwt.vencimiento(token))
    }

    @Test
    fun `un token que vence despues sigue vigente`() {
        assertTrue(TokenJwt.vigente(jwt("""{"exp":${ahora.epochSecond + 7 * 24 * 3600}}"""), ahora))
    }

    @Test
    fun `un token vencido o por vencer en menos de un minuto no se restaura`() {
        assertFalse(TokenJwt.vigente(jwt("""{"exp":${ahora.epochSecond - 10}}"""), ahora))
        assertFalse(TokenJwt.vigente(jwt("""{"exp":${ahora.epochSecond + 30}}"""), ahora))
    }

    @Test
    fun `sin exp se deja decidir al servidor`() {
        assertNull(TokenJwt.vencimiento(jwt("""{"sub":"u1"}""")))
        assertTrue(TokenJwt.vigente(jwt("""{"sub":"u1"}"""), ahora))
    }

    @Test
    fun `un texto que no es JWT no trae vencimiento y uno vacio no sirve`() {
        assertNull(TokenJwt.vencimiento("no-es-un-token"))
        assertNull(TokenJwt.vencimiento("a.%%%.c"))
        assertFalse(TokenJwt.vigente("", ahora))
    }
}

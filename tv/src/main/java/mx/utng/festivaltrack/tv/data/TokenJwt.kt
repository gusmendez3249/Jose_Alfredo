package mx.utng.festivaltrack.tv.data

import java.time.Instant
import java.util.Base64

/**
 * Lectura local del JWT que entrega el backend (`exp` en segundos desde 1970).
 *
 * No valida la firma (eso lo hace el servidor en cada petición): solo sirve para no restaurar
 * al abrir la app una sesión que ya venció.
 */
object TokenJwt {

    /** Momento en que vence el token, o null si no es un JWT o no trae `exp`. */
    fun vencimiento(token: String): Instant? {
        val partes = token.split('.')
        if (partes.size != 3) return null
        val json = runCatching { String(Base64.getUrlDecoder().decode(partes[1].trimEnd('=')), Charsets.UTF_8) }
            .getOrNull() ?: return null
        val exp = Regex(""""exp"\s*:\s*(\d+)""").find(json)?.groupValues?.get(1)?.toLongOrNull() ?: return null
        return Instant.ofEpochSecond(exp)
    }

    /**
     * true si el token todavía sirve en [ahora], con un margen de un minuto para no entrar con
     * uno que vence enseguida. Un token sin `exp` se da por vigente (el servidor decide).
     */
    fun vigente(token: String, ahora: Instant = Instant.now()): Boolean {
        if (token.isBlank()) return false
        val vence = vencimiento(token) ?: return true
        return vence.isAfter(ahora.plusSeconds(60))
    }
}

package mx.utng.festivaltrack.tv.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Datos de la cuenta que se guardan en la TV. */
data class DatosSesion(val nombre: String, val correo: String, val token: String)

/**
 * Guarda la sesión de la TV para no pedir el correo y la contraseña cada vez que se abre la app.
 *
 * Igual que `TokenManager` del móvil: el JWT se cifra con AES-256-GCM con una llave del Android
 * Keystore (no se puede sacar del dispositivo). Si el token venció o no se puede descifrar, se
 * borra y se vuelve a pedir el inicio de sesión.
 */
class SesionGuardadaTv(context: Context) {

    private val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    /** La sesión guardada si sigue vigente; si no hay o ya venció, null (y se borra lo que hubiera). */
    fun leer(): DatosSesion? {
        val cifrado = prefs.getString(KEY_TOKEN, null) ?: return null
        val token = descifrar(cifrado)
        if (token == null || !TokenJwt.vigente(token)) {
            borrar()
            return null
        }
        return DatosSesion(
            nombre = prefs.getString(KEY_NOMBRE, null).orEmpty(),
            correo = prefs.getString(KEY_CORREO, null).orEmpty(),
            token = token
        )
    }

    fun guardar(datos: DatosSesion) {
        val cifrado = runCatching { cifrar(datos.token) }.getOrNull() ?: return
        prefs.edit()
            .putString(KEY_TOKEN, cifrado)
            .putString(KEY_NOMBRE, datos.nombre)
            .putString(KEY_CORREO, datos.correo)
            .apply()
    }

    fun borrar() {
        prefs.edit().clear().apply()
    }

    private fun cifrar(texto: String): String {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.ENCRYPT_MODE, llave())
        val datos = cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + datos, Base64.NO_WRAP)
    }

    private fun descifrar(guardado: String): String? = try {
        val bytes = Base64.decode(guardado, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(TAG_BITS, bytes.copyOfRange(0, IV_BYTES)))
        String(cipher.doFinal(bytes.copyOfRange(IV_BYTES, bytes.size)), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }

    private fun llave(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS_LLAVE, null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS_LLAVE, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    private companion object {
        const val ARCHIVO = "festivaltrack_tv_sesion"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS_LLAVE = "festivaltrack_tv_sesion"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val KEY_TOKEN = "jwt_cifrado"
        const val KEY_NOMBRE = "nombre"
        const val KEY_CORREO = "correo"
    }
}

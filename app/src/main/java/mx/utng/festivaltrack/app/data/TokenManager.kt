package mx.utng.festivaltrack.app.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda la sesión del usuario.
 *
 * El JWT se cifra con AES-256-GCM usando una llave que vive en el Android Keystore
 * (no se puede extraer del dispositivo). Así, aunque alguien copie el archivo de
 * preferencias, no obtiene un token utilizable. El rol solo decide qué pantallas se
 * muestran: el backend vuelve a validar rol y token en cada petición.
 */
class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("festival_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit()
            .putString(KEY_TOKEN_CIFRADO, cifrar(token))
            .remove(KEY_TOKEN_LEGADO)
            .apply()
    }

    fun getToken(): String? {
        prefs.getString(KEY_TOKEN_CIFRADO, null)?.let { return descifrar(it) }
        // Migración: versiones anteriores guardaban el token en texto plano.
        val legado = prefs.getString(KEY_TOKEN_LEGADO, null) ?: return null
        saveToken(legado)
        return legado
    }

    fun saveUserRole(role: String) {
        prefs.edit().putString("user_role", role).apply()
    }

    fun getUserRole(): String? {
        return prefs.getString("user_role", null)
    }

    fun saveUserId(id: String) {
        prefs.edit().putString("user_id", id).apply()
    }

    fun getUserId(): String? {
        return prefs.getString("user_id", null)
    }

    fun saveUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
    }

    fun getUserName(): String? {
        return prefs.getString("user_name", null)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun cifrar(texto: String): String {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.ENCRYPT_MODE, llave())
        val iv = cipher.iv
        val datos = cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + datos, Base64.NO_WRAP)
    }

    /** Devuelve null si el valor no se puede descifrar (por ejemplo, la llave se borró); obliga a iniciar sesión de nuevo. */
    private fun descifrar(guardado: String): String? = try {
        val bytes = Base64.decode(guardado, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val datos = bytes.copyOfRange(IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(datos), Charsets.UTF_8)
    } catch (e: Exception) {
        prefs.edit().remove(KEY_TOKEN_CIFRADO).apply()
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
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS_LLAVE = "festivaltrack_sesion"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val KEY_TOKEN_CIFRADO = "jwt_token_enc"
        const val KEY_TOKEN_LEGADO = "jwt_token"
    }
}

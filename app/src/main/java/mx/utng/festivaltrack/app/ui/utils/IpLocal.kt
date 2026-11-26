package mx.utng.festivaltrack.app.ui.utils

import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * IP del teléfono dentro de la red local, para que otros teléfonos se conecten a su
 * transmisión RTSP.
 *
 * Antes se usaba `WifiManager.connectionInfo.ipAddress`, que regresa 0 cuando el teléfono
 * comparte su hotspot, usa datos o en varias versiones de Android; entonces se publicaba
 * `10.0.2.2` (dirección que solo existe dentro del emulador) y el otro teléfono no
 * encontraba el video. Ahora se revisan todas las interfaces de red.
 */
object IpLocal {

    /** Interfaces de red: (nombre, IPv4) de las que están activas. */
    fun interfaces(): List<Pair<String, String>> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { ni ->
                ni.inetAddresses.toList()
                    .filterIsInstance<Inet4Address>()
                    .map { ni.name to (it.hostAddress ?: "") }
            }
    }.getOrDefault(emptyList())

    /**
     * Elige la IP a la que pueden llegar otros teléfonos de la misma red:
     * primero Wi-Fi o hotspot (wlan, swlan, ap, softap), luego cualquier IP privada.
     * Las IP de datos móviles (rmnet, ccmni…) no sirven porque el operador las aísla.
     */
    fun elegir(interfaces: List<Pair<String, String>>): String? {
        val privadas = interfaces.filter { (_, ip) -> esPrivada(ip) }
        val wifi = privadas.firstOrNull { (nombre, _) ->
            val n = nombre.lowercase()
            n.startsWith("wlan") || n.startsWith("swlan") || n.startsWith("ap") || n.contains("softap")
        }
        return (wifi ?: privadas.firstOrNull { (nombre, _) -> !esDatosMoviles(nombre) })?.second
    }

    /** 10.x, 172.16–31.x o 192.168.x (sin contar 10.0.2.x del emulador como red real). */
    fun esPrivada(ip: String): Boolean {
        val p = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (p.size != 4) return false
        return p[0] == 10 || (p[0] == 172 && p[1] in 16..31) || (p[0] == 192 && p[1] == 168)
    }

    private fun esDatosMoviles(nombre: String): Boolean {
        val n = nombre.lowercase()
        return n.startsWith("rmnet") || n.startsWith("ccmni") || n.startsWith("pdp") || n.startsWith("v4-rmnet")
    }

    /** true dentro del emulador de Android Studio. */
    fun esEmulador(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.HARDWARE.contains("goldfish") || Build.HARDWARE.contains("ranchu") ||
            Build.PRODUCT.contains("sdk_gphone")

    /**
     * IP para la URL RTSP del administrador; null si el teléfono no está en ninguna red
     * Wi-Fi ni comparte hotspot. En el emulador, `10.0.2.2` (la PC, para pruebas locales).
     */
    fun paraTransmitir(): String? = if (esEmulador()) "10.0.2.2" else elegir(interfaces())
}

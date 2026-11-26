package mx.utng.festivaltrack.app.auto.mapa

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections

/**
 * Mosaicos de OpenStreetMap para el mapa del auto, con caché en memoria.
 *
 * Se descargan en segundo plano (pocos a la vez, como pide la política de uso de OSM) y al
 * llegar cada uno se avisa con [alLlegar] para volver a dibujar.
 */
class MosaicosMapa(private val alLlegar: () -> Unit) {

    private val cache = object : LruCache<String, Bitmap>(MEMORIA_KB) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }
    private val pidiendo = Collections.synchronizedSet(mutableSetOf<String>())
    private val fallidos = Collections.synchronizedSet(mutableSetOf<String>())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val limite = Semaphore(DESCARGAS_SIMULTANEAS)

    /** Mosaico listo para dibujar, o null mientras se descarga (lo pide si hace falta). */
    fun obtener(z: Int, x: Int, y: Int): Bitmap? {
        val n = 1 shl z
        if (y < 0 || y >= n) return null
        val xx = ((x % n) + n) % n
        val clave = "$z/$xx/$y"
        cache.get(clave)?.let { return it }
        if (clave !in fallidos && pidiendo.add(clave)) {
            scope.launch {
                limite.withPermit {
                    val bitmap = runCatching { descargar(clave) }.getOrNull()
                    if (bitmap != null) cache.put(clave, bitmap) else fallidos.add(clave)
                    pidiendo.remove(clave)
                    if (bitmap != null) alLlegar()
                }
            }
        }
        return null
    }

    /** Mosaico de un nivel menor ya descargado, para no dejar huecos mientras llega el bueno. */
    fun respaldo(z: Int, x: Int, y: Int): Pair<Bitmap, Int>? {
        var nivel = z - 1
        var px = x
        var py = y
        var escala = 2
        while (nivel >= z - 3 && nivel >= 0) {
            px = Math.floorDiv(px, 2)
            py = Math.floorDiv(py, 2)
            val n = 1 shl nivel
            cache.get("$nivel/${((px % n) + n) % n}/$py")?.let { return it to escala }
            nivel--
            escala *= 2
        }
        return null
    }

    fun liberar() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        cache.evictAll()
    }

    private fun descargar(clave: String): Bitmap? {
        val conexion = URL("https://tile.openstreetmap.org/$clave.png").openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 6000
            conexion.readTimeout = 8000
            conexion.setRequestProperty("User-Agent", "FestivalTrack-App (mx.utng.festivaltrack.app)")
            if (conexion.responseCode != 200) return null
            conexion.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            conexion.disconnect()
        }
    }

    private companion object {
        const val MEMORIA_KB = 24 * 1024
        const val DESCARGAS_SIMULTANEAS = 2
    }
}

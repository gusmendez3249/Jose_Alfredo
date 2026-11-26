package mx.utng.festivaltrack.app.auto

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.car.app.CarContext
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.app.playback.PlaybackService
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections

/**
 * Portadas de las canciones para las filas del auto (las mismas que muestra el teléfono).
 * Se descargan una vez y se guardan en memoria; mientras llegan se muestra la nota dorada.
 */
object PortadasAuto {

    private const val LADO_PX = 192
    private val cache = LruCache<String, Bitmap>(40)
    private val pidiendo = Collections.synchronizedSet(mutableSetOf<String>())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Portada lista, o null (y la pide) si aún no se descarga. [alLlegar] se llama al tenerla. */
    fun obtener(url: String?, alLlegar: () -> Unit): Bitmap? {
        if (url.isNullOrBlank()) return null
        cache.get(url)?.let { return it }
        if (pidiendo.add(url)) {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) { runCatching { descargar(url) }.getOrNull() }
                if (bitmap != null) {
                    cache.put(url, bitmap)
                    alLlegar()
                }
            }
        }
        return null
    }

    fun icono(bitmap: Bitmap): CarIcon = CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()

    private fun descargar(url: String): Bitmap? {
        val conexion = URL(url).openConnection() as HttpURLConnection
        return try {
            conexion.connectTimeout = 6000
            conexion.readTimeout = 8000
            if (conexion.responseCode != 200) return null
            val original = conexion.inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
            Bitmap.createScaledBitmap(original, LADO_PX, LADO_PX, true)
        } finally {
            conexion.disconnect()
        }
    }
}

/**
 * Abre el reproductor del auto (la pantalla "Reproduciendo" del sistema) con la música de
 * FestivalTrack. Solo existe en Android Automotive; en Android Auto el reproductor lo abre el
 * conductor desde la barra del auto, así que ahí no se hace nada.
 */
object ReproductorAuto {

    private const val ACCION = "android.car.intent.action.MEDIA_TEMPLATE"
    private const val EXTRA_COMPONENTE = "android.car.intent.extra.MEDIA_COMPONENT"

    fun abrir(carContext: CarContext): Boolean {
        if (!carContext.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) return false
        val componente = ComponentName(carContext, PlaybackService::class.java).flattenToString()
        return runCatching {
            carContext.startActivity(
                Intent(ACCION)
                    .putExtra(EXTRA_COMPONENTE, componente)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess
    }
}

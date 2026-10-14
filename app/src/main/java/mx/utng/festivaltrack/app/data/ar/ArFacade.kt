package mx.utng.festivaltrack.app.data.ar

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.ReconocimientoDto
import mx.utng.festivaltrack.shared.data.remote.VisitaSitioDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

/**
 * Facade del módulo AR en la app.
 *
 * Oculta a la UI los tres subsistemas del flujo "estilo Google Lens":
 *  1. Cámara: captura la foto con CameraX, la endereza y la reduce.
 *  2. Reconocimiento visual: el backend la analiza con IA (Claude).
 *  3. Catálogo: el backend devuelve el sitio del catálogo y registra la visita.
 *
 * La UI solo llama a [reconocer] y recibe un [ReconocimientoDto].
 * La API key de IA vive en el backend; la app nunca la conoce.
 */
class ArFacade(
    private val context: Context,
    private val api: FestivalApiService = FestivalApiService.create(),
    private val tokenManager: TokenManager = TokenManager(context)
) {

    /**
     * Captura la foto actual de la cámara y devuelve el sitio reconocido.
     * @param onFotoCapturada se invoca al terminar la captura, antes de enviar a la IA (para la UI).
     */
    suspend fun reconocer(imageCapture: ImageCapture, onFotoCapturada: () -> Unit = {}): ReconocimientoDto {
        val token = requireToken()
        val jpeg = capturarJpeg(imageCapture)
        onFotoCapturada()
        val parte = MultipartBody.Part.createFormData(
            "foto", "captura.jpg", jpeg.toRequestBody("image/jpeg".toMediaType())
        )
        return api.reconocerSitio("Bearer $token", parte)
    }

    /** Historial de sitios visitados por el usuario. */
    suspend fun historial(): List<VisitaSitioDto> = api.getHistorialSitios("Bearer ${requireToken()}")

    private fun requireToken(): String =
        tokenManager.getToken() ?: throw IllegalStateException("Inicia sesión para usar el módulo AR")

    private suspend fun capturarJpeg(imageCapture: ImageCapture): ByteArray {
        val bitmap = suspendCancellableCoroutine<Bitmap> { cont ->
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val rotacion = image.imageInfo.rotationDegrees
                        val original = image.toBitmap()
                        image.close()
                        cont.resume(rotar(original, rotacion))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cont.resumeWithException(exception)
                    }
                }
            )
        }
        return withContext(Dispatchers.Default) { comprimir(bitmap) }
    }

    private fun rotar(bitmap: Bitmap, grados: Int): Bitmap {
        if (grados == 0) return bitmap
        val m = Matrix().apply { postRotate(grados.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    /** Reduce el lado mayor a [LADO_MAXIMO] px: suficiente para reconocer y ligero de subir. */
    private fun comprimir(bitmap: Bitmap): ByteArray {
        val escala = LADO_MAXIMO.toFloat() / max(bitmap.width, bitmap.height)
        val reducido = if (escala < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true)
        } else bitmap
        return ByteArrayOutputStream().use { out ->
            reducido.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.toByteArray()
        }
    }

    private companion object {
        const val LADO_MAXIMO = 1280
    }
}

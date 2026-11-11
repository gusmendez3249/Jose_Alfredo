package mx.utng.festivaltrack.app.ui.utils

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.source
import java.io.File

/** Reglas del formulario de alta de canciones, separadas de la pantalla para poder probarlas. */
object SongUpload {
    /** Tamaño máximo que acepta el servidor por archivo. */
    const val MAX_BYTES = 30L * 1024 * 1024

    /** Etapas de la publicación, con la parte del avance total que ocupa cada una. */
    enum class Stage(val label: String, val start: Float, val end: Float) {
        AUDIO("Subiendo el audio", 0f, 0.80f),
        COVER("Subiendo la portada", 0.80f, 0.95f),
        SAVING("Guardando en el catálogo", 0.95f, 1f)
    }

    /** Avance total (0 a 1) a partir de la etapa y de cuánto lleva esa etapa (0 a 1). */
    fun overallProgress(stage: Stage, stageFraction: Float): Float =
        stage.start + (stage.end - stage.start) * stageFraction.coerceIn(0f, 1f)

    /** Mensaje de error del formulario, o null si se puede publicar. [audioBytes] null = sin audio propio. */
    fun validate(title: String, audioBytes: Long?, coverBytes: Long?): String? = when {
        title.isBlank() -> "Escribe el título de la canción"
        audioBytes != null && audioBytes > MAX_BYTES -> "El audio pesa más de 30 MB; elige un archivo más ligero"
        coverBytes != null && coverBytes > MAX_BYTES -> "La portada pesa más de 30 MB; elige una imagen más ligera"
        else -> null
    }

    /** 3_400_000 -> "3.2 MB" */
    fun formatSize(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
        bytes >= 1024 -> "%d KB".format(bytes / 1024)
        else -> "$bytes B"
    }
}

/** Cuerpo de una petición que va avisando cuánto del archivo se ha enviado (0 a 1). */
class ProgressFileBody(
    private val file: File,
    private val mediaType: MediaType?,
    private val onProgress: (Float) -> Unit
) : RequestBody() {
    override fun contentType(): MediaType? = mediaType
    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = file.length().coerceAtLeast(1)
        var sent = 0L
        file.source().use { source ->
            val buffer = Buffer()
            while (true) {
                val read = source.read(buffer, 64 * 1024L)
                if (read == -1L) break
                sink.write(buffer, read)
                sent += read
                onProgress(sent.toFloat() / total)
            }
        }
    }
}

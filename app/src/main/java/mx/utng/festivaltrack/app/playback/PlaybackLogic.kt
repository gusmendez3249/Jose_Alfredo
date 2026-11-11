package mx.utng.festivaltrack.app.playback

import kotlin.random.Random

/**
 * Reglas de la cola de reproducción que no dependen de Android, para poder probarlas con pruebas
 * unitarias: orden aleatorio, avance y duración de la demo, y qué hacer cuando una canción falla.
 */
object PlaybackLogic {

    /** Qué hace el reproductor cuando una canción no se puede reproducir. */
    enum class ErrorAction {
        /** Saltar a la siguiente canción de la cola. */
        SKIP_TO_NEXT,
        /** Era la última: volver al inicio de la cola y quedar en pausa. */
        BACK_TO_START,
        /** Nada de la cola se pudo reproducir: detener y vaciar. */
        STOP,
        /** No hay conexión: quedarse en la canción, en pausa, para reintentar al volver la red. */
        WAIT_FOR_NETWORK
    }

    /**
     * Decide cómo seguir tras un error en la canción [currentIndex].
     * [consecutiveErrors] cuenta los fallos seguidos, incluido el actual; si ya falló toda la cola
     * se detiene en lugar de saltar sin fin. Si el fallo es de conexión ([networkError]) no se salta
     * nada: el problema no es la canción.
     */
    fun afterError(queueSize: Int, currentIndex: Int, consecutiveErrors: Int, networkError: Boolean = false): ErrorAction = when {
        networkError -> ErrorAction.WAIT_FOR_NETWORK
        queueSize <= 1 || consecutiveErrors >= queueSize -> ErrorAction.STOP
        currentIndex in 0 until queueSize - 1 -> ErrorAction.SKIP_TO_NEXT
        else -> ErrorAction.BACK_TO_START
    }

    /**
     * Cola en orden aleatorio: la canción [currentIndex] queda primero (sigue sonando sin cortes)
     * y el resto se mezcla detrás. Con un índice fuera de rango se mezcla toda la lista.
     */
    fun <T> shuffled(items: List<T>, currentIndex: Int, random: Random = Random.Default): List<T> {
        if (items.size < 2) return items
        val current = items.getOrNull(currentIndex) ?: return items.shuffled(random)
        val rest = items.filterIndexed { i, _ -> i != currentIndex }.shuffled(random)
        return listOf(current) + rest
    }

    /** Duración real de la demo: la que informa el reproductor o, mientras carga, la estimada. */
    fun demoDurationMs(playerDurationMs: Long, trackDurationSec: Int, demoMs: Long): Long = when {
        playerDurationMs > 0 -> playerDurationMs
        trackDurationSec > 0 -> minOf(trackDurationSec * 1000L, demoMs)
        else -> demoMs
    }

    /** Avance de 0 a 1. */
    fun progress(positionMs: Long, durationMs: Long): Float =
        if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    /** Posición a la que hay que saltar cuando el usuario arrastra la barra hasta [fraction]. */
    fun seekPositionMs(fraction: Float, durationMs: Long): Long =
        (fraction.coerceIn(0f, 1f) * durationMs.coerceAtLeast(0)).toLong()

    /** 65_000 ms -> "1:05" */
    fun formatMs(ms: Long): String {
        val s = (ms.coerceAtLeast(0) / 1000).toInt()
        return "%d:%02d".format(s / 60, s % 60)
    }

    /** Mueve el elemento [from] a la posición [to] (para ordenar el repertorio arrastrando). */
    fun <T> move(items: List<T>, from: Int, to: Int): List<T> {
        if (from !in items.indices || to !in items.indices || from == to) return items
        return items.toMutableList().apply { add(to, removeAt(from)) }
    }
}

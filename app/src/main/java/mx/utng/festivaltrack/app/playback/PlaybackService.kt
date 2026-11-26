package mx.utng.festivaltrack.app.playback

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.R

/**
 * Servicio de reproducción de las demos del concierto (#31).
 *
 * Aloja el [ExoPlayer] dentro de una sesión multimedia: así el audio sigue sonando cuando la app pasa a
 * segundo plano o el usuario navega a otras secciones, y Android muestra la notificación multimedia con
 * los controles (pausa, anterior, siguiente) y la pantalla de bloqueo. La app lo controla con un
 * `MediaController` desde [PlaybackManager].
 *
 * Además publica la [BibliotecaMusical] (discografía y repertorio de los próximos conciertos) para
 * que el reproductor del auto la muestre y se pueda elegir y controlar desde el volante, tanto en
 * Android Automotive como en Android Auto.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private var session: MediaLibrarySession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        // Canal propio, con nombre en español, e icono de la app en la barra de estado.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.playback_channel_name)
                .build()
                .apply { setSmallIcon(R.drawable.ic_stat_demo) }
        )

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true) // pausa si se desconectan los audífonos
            .build()
        // Al cerrar el reproductor la cola queda vacía: el servicio ya no hace falta y se detiene.
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_TIMELINE_CHANGED) && player.mediaItemCount == 0) stopSelf()
            }
        })

        val builder = MediaLibrarySession.Builder(this, player, BibliotecaCallback())
        // Al tocar la notificación se vuelve a la app tal como estaba.
        packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            builder.setSessionActivity(
                PendingIntent.getActivity(
                    this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
        session = builder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    /**
     * Árbol que ve el reproductor del auto: raíz → carpetas (discografía y conciertos) → canciones.
     * Al elegir una canción se pone en cola toda su carpeta, para que "siguiente" funcione desde el volante.
     */
    private inner class BibliotecaCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(carpetaItem(ID_RAIZ, "FestivalTrack", null), params))

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> = enSegundoPlano {
            val item = if (BibliotecaMusical.carpetaDe(mediaId) == null) {
                BibliotecaMusical.carpeta(mediaId)?.let { carpetaItem(it.id, it.titulo, it.subtitulo) }
            } else {
                buscarPista(mediaId)
            }
            if (item != null) LibraryResult.ofItem(item, null) else LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = enSegundoPlano {
            val hijos = if (parentId == ID_RAIZ) {
                BibliotecaMusical.carpetas().map { carpetaItem(it.id, it.titulo, it.subtitulo) }
            } else {
                BibliotecaMusical.carpeta(parentId)?.pistas?.map(PlaybackManager::toMediaItem).orEmpty()
            }
            LibraryResult.ofItemList(ImmutableList.copyOf(hijos), params)
        }

        /** Acepta las canciones que manda la app con su URL; las del auto llegan solo con su id. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> = enSegundoPlano {
            mediaItems.map { resolver(it) }.toMutableList()
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = enSegundoPlano {
            // Una sola canción elegida en el reproductor del auto: se pone en cola toda su carpeta.
            val unica = mediaItems.singleOrNull()?.takeIf { urlDe(it) == null }
            val carpeta = unica?.let { BibliotecaMusical.carpetaDe(it.mediaId) }?.let { BibliotecaMusical.carpeta(it) }
            val indice = carpeta?.pistas?.indexOfFirst { it.id == unica?.mediaId } ?: -1
            if (carpeta != null && indice >= 0) {
                MediaSession.MediaItemsWithStartPosition(carpeta.pistas.map(PlaybackManager::toMediaItem), indice, 0L)
            } else {
                MediaSession.MediaItemsWithStartPosition(mediaItems.map { resolver(it) }, startIndex, startPositionMs)
            }
        }
    }

    private fun urlDe(item: MediaItem) = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri

    private suspend fun resolver(item: MediaItem): MediaItem {
        val uri = urlDe(item)
        return if (uri != null) item.buildUpon().setUri(uri).build() else buscarPista(item.mediaId) ?: item
    }

    private suspend fun buscarPista(mediaId: String): MediaItem? {
        val carpetaId = BibliotecaMusical.carpetaDe(mediaId) ?: return null
        return BibliotecaMusical.carpeta(carpetaId)?.pistas?.firstOrNull { it.id == mediaId }
            ?.let(PlaybackManager::toMediaItem)
    }

    private fun carpetaItem(id: String, titulo: String, subtitulo: String?): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(titulo)
                    .setSubtitle(subtitulo)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                    .build()
            )
            .build()

    /** Responde al reproductor sin bloquear el hilo principal mientras se consulta el servidor. */
    private fun <T> enSegundoPlano(bloque: suspend () -> T): ListenableFuture<T> {
        val futuro = SettableFuture.create<T>()
        scope.launch {
            try {
                futuro.set(bloque())
            } catch (e: Exception) {
                futuro.setException(e)
            }
        }
        return futuro
    }

    /** Si el usuario cierra la app desde recientes y no hay nada sonando, el servicio se detiene. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            // En pausa la notificación ya no pertenece a un servicio en primer plano: hay que
            // quitarla a mano o se queda colgada cuando el sistema cierra la app.
            getSystemService(NotificationManager::class.java)
                ?.cancel(DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID)
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "demos_festival"
        const val ID_RAIZ = "raiz"
    }
}

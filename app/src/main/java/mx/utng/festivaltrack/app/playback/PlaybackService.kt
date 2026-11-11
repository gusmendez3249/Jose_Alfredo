package mx.utng.festivaltrack.app.playback

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import mx.utng.festivaltrack.app.R

/**
 * Servicio de reproducción de las demos del concierto (#31).
 *
 * Aloja el [ExoPlayer] dentro de una [MediaSession]: así el audio sigue sonando cuando la app pasa a
 * segundo plano o el usuario navega a otras secciones, y Android muestra la notificación multimedia con
 * los controles (pausa, anterior, siguiente) y la pantalla de bloqueo. La app lo controla con un
 * `MediaController` desde [PlaybackManager].
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    @OptIn(UnstableApi::class)
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

        val builder = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                /** Acepta las canciones que manda la app con su URL de audio. */
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> {
                    val resolved = mediaItems.map { item ->
                        val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri
                        if (uri != null) item.buildUpon().setUri(uri).build() else item
                    }.toMutableList()
                    return Futures.immediateFuture(resolved)
                }
            })
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Si el usuario cierra la app desde recientes y no hay nada sonando, el servicio se detiene. */
    @OptIn(UnstableApi::class)
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
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "demos_festival"
    }
}

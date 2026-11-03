package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import mx.utng.festivaltrack.app.playback.PlaybackManager
import mx.utng.festivaltrack.app.playback.QueueTrack
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PlayerBg = Color(0xFF16221A)
private val CoverBg = Color(0xFF26352B)

/**
 * Minirreproductor global: se ve en cualquier pantalla mientras hay una demo en la cola, de modo que el
 * audio se controla sin volver al concierto. Al tocarlo abre [NowPlayingSheet].
 */
@Composable
fun GlobalMiniPlayer(onOpen: () -> Unit) {
    val queue by PlaybackManager.queue.collectAsState()
    val index by PlaybackManager.currentIndex.collectAsState()
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val isBuffering by PlaybackManager.isBuffering.collectAsState()
    val progress by PlaybackManager.progress.collectAsState()
    val track = queue.getOrNull(index) ?: return

    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF2C5A3E), PlayerBg)))
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackCover(track.coverUrl, track.title, 44.dp, 6.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${track.artist} · ${track.eventoNombre}",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = PlaybackManager::previous) { Icon(Icons.Default.SkipPrevious, "Anterior", tint = Color.White) }
            PlayPauseButton(isPlaying, isBuffering, 40.dp, onClick = PlaybackManager::playPause)
            IconButton(onClick = PlaybackManager::next) { Icon(Icons.Default.SkipNext, "Siguiente", tint = Color.White) }
            IconButton(onClick = PlaybackManager::stop) {
                Icon(Icons.Default.Close, "Cerrar reproductor", tint = Color.White.copy(alpha = 0.7f))
            }
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = PrimaryGold,
            trackColor = Color.White.copy(alpha = 0.18f)
        )
    }
}

/** Pantalla "Reproduciendo": portada grande, concierto con escenario y horario, controles y cola. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(onDismiss: () -> Unit) {
    val queue by PlaybackManager.queue.collectAsState()
    val index by PlaybackManager.currentIndex.collectAsState()
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val isBuffering by PlaybackManager.isBuffering.collectAsState()
    val progress by PlaybackManager.progress.collectAsState()
    val shuffle by PlaybackManager.shuffle.collectAsState()
    val track = queue.getOrNull(index)

    // Si la cola se vacía (terminó o se cerró), la hoja se cierra sola.
    LaunchedEffect(track == null) { if (track == null) onDismiss() }
    if (track == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color(0xFF0F1712)) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    TrackCover(track.coverUrl, track.title, 240.dp, 16.dp)
                    Spacer(Modifier.height(18.dp))
                    Text(track.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Text(track.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    ConcertInfo(track)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = PrimaryGold,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDuration((progress * PlaybackManager.DEMO_MS / 1000).toInt()), color = Color.Gray, fontSize = 11.sp)
                        Text("demo de ${PlaybackManager.DEMO_MS / 1000} s", color = Color.Gray, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconButton(onClick = PlaybackManager::toggleShuffle) {
                            Icon(
                                Icons.Default.Shuffle, if (shuffle) "Aleatorio activado" else "Activar aleatorio",
                                tint = if (shuffle) PrimaryGold else Color.White.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = PlaybackManager::previous, modifier = Modifier.size(52.dp)) {
                            Icon(Icons.Default.SkipPrevious, "Anterior", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        PlayPauseButton(isPlaying, isBuffering, 64.dp, PrimaryGold, Color.Black, PlaybackManager::playPause)
                        IconButton(onClick = PlaybackManager::next, modifier = Modifier.size(52.dp)) {
                            Icon(Icons.Default.SkipNext, "Siguiente", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        Spacer(Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        if (shuffle) "COLA · ORDEN ALEATORIO" else "COLA",
                        color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
            itemsIndexed(queue, key = { _, t -> t.id }) { i, t ->
                QueueRow(t, isCurrent = i == index, isPlaying = isPlaying && i == index) {
                    if (i == index) PlaybackManager.playPause() else PlaybackManager.skipTo(i)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ConcertInfo(track: QueueTrack) {
    val details = listOfNotNull(
        track.fechaHora?.let(::formatConcertDateTime)?.takeIf { it.isNotBlank() },
        track.escenario?.takeIf { it.isNotBlank() }
    ).joinToString(" • ")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Place, null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(track.eventoNombre, color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        if (details.isNotBlank()) {
            Text(details, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun QueueRow(track: QueueTrack, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) PrimaryGold.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackCover(track.coverUrl, track.title, 44.dp, 6.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title, color = if (isCurrent) PrimaryGold else Color.White,
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(track.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isCurrent) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                null, tint = PrimaryGold, modifier = Modifier.size(22.dp)
            )
        } else {
            Text(formatDuration(track.durationSec), color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    size: Dp,
    background: Color = Color.White,
    tint: Color = Color.Black,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(size).background(background, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isBuffering) {
            CircularProgressIndicator(color = tint, strokeWidth = 2.dp, modifier = Modifier.size(size * 0.45f))
        } else {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (isPlaying) "Pausar" else "Reproducir", tint = tint, modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

@Composable
private fun TrackCover(url: String?, title: String, size: Dp, radius: Dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(radius)).background(CoverBg),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = "Portada de $title", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.size(size * 0.5f))
        }
    }
}

private fun formatConcertDateTime(iso: String): String = try {
    val fmt = DateTimeFormatter.ofPattern("d 'de' MMMM, HH:mm 'hrs'", Locale("es", "MX"))
    fmt.format(Instant.parse(iso).atZone(ZoneId.systemDefault()))
} catch (e: Exception) {
    ""
}

package mx.utng.festivaltrack.app.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.playback.PlaybackManager
import mx.utng.festivaltrack.app.playback.QueueTrack
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import kotlin.math.abs

/**
 * Estado de la hoja "Reproduciendo": [fraction] va de 0 (minirreproductor) a 1 (pantalla completa).
 * Lo comparten el minirreproductor y la pantalla completa, así el paso de uno a otra sigue al dedo.
 */
@Stable
class PlayerSheetState internal constructor(expanded: Boolean, private val scope: CoroutineScope) {
    private val anim = Animatable(if (expanded) 1f else 0f)

    val fraction: Float get() = anim.value
    val isExpanded: Boolean get() = anim.targetValue == 1f

    fun expand() {
        scope.launch { anim.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) }
    }

    fun collapse() {
        scope.launch { anim.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)) }
    }

    /** Arrastre vertical: [deltaPx] negativo sube (abre). */
    internal fun dragBy(deltaPx: Float, heightPx: Float) {
        if (heightPx <= 0f) return
        scope.launch { anim.snapTo((anim.value - deltaPx / heightPx).coerceIn(0f, 1f)) }
    }

    /** Al soltar: decide abrir o cerrar según la velocidad y hasta dónde llegó. */
    internal fun settle(velocityPx: Float) {
        when {
            velocityPx < -900f -> expand()
            velocityPx > 900f -> collapse()
            anim.value > 0.5f -> expand()
            else -> collapse()
        }
    }
}

@Composable
fun rememberPlayerSheetState(): PlayerSheetState {
    val scope = rememberCoroutineScope()
    var expanded by rememberSaveable { mutableStateOf(false) } // sobrevive al girar la pantalla
    val state = remember { PlayerSheetState(expanded, scope) }
    LaunchedEffect(state.isExpanded) { expanded = state.isExpanded }
    return state
}

/**
 * Minirreproductor global: se ve en cualquier pantalla mientras hay una demo en la cola, de modo que el
 * audio se controla sin volver al concierto. Al tocarlo o deslizarlo hacia arriba abre [NowPlayingScreen].
 */
@Composable
fun GlobalMiniPlayer(sheet: PlayerSheetState, screenHeightPx: Float) {
    val queue by PlaybackManager.queue.collectAsState()
    val index by PlaybackManager.currentIndex.collectAsState()
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val isBuffering by PlaybackManager.isBuffering.collectAsState()
    val progress by PlaybackManager.progress.collectAsState()
    val track = queue.getOrNull(index) ?: return
    val haptic = rememberHaptic()

    val accent by animateColorAsState(rememberCoverColor(track.coverUrl), tween(600), label = "miniAccent")
    val smooth by animateFloatAsState(progress, tween(120), label = "miniProgress")
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .graphicsLayer { alpha = (1f - sheet.fraction * 3f).coerceIn(0f, 1f) }
            .shadow(10.dp, shape)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(accent, AudioSurface)))
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> sheet.dragBy(delta, screenHeightPx) },
                onDragStopped = { velocity -> sheet.settle(velocity) }
            )
            .clickable(onClickLabel = "Abrir el reproductor", onClick = sheet::expand)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverArt(track.coverUrl, track.title, Modifier.size(46.dp), radius = 8.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = track,
                    transitionSpec = { (fadeIn(tween(250)) + slideInHorizontally { it / 4 }) togetherWith fadeOut(tween(150)) },
                    contentKey = { it.id },
                    label = "miniTitle"
                ) { t ->
                    Column {
                        Text(t.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${t.artist} · ${t.eventoNombre}",
                            color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (isPlaying) {
                Equalizer(playing = true, modifier = Modifier.padding(horizontal = 6.dp).size(width = 16.dp, height = 14.dp))
            }
            PlayPauseButton(isPlaying, isBuffering, 42.dp, onClick = { haptic(); PlaybackManager.playPause() })
            PlayerIconButton(
                Icons.Default.SkipNext, "Siguiente", enabled = index < queue.lastIndex,
                onClick = { haptic(); PlaybackManager.next() }
            )
            PlayerIconButton(
                Icons.Default.Close, "Cerrar reproductor", tint = Color.White.copy(alpha = 0.75f),
                onClick = { haptic(); PlaybackManager.stop() }
            )
        }
        LinearProgressIndicator(
            progress = smooth,
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = PrimaryGold,
            trackColor = Color.White.copy(alpha = 0.16f)
        )
    }
}

/**
 * Pantalla completa "Reproduciendo": fondo con el color de la portada, portada grande, concierto con
 * escenario y horario, barra de progreso para moverse en la demo, controles y la cola.
 * Se cierra con el botón, con "atrás" o deslizando hacia abajo.
 */
@Composable
fun NowPlayingScreen(
    sheet: PlayerSheetState,
    screenHeightPx: Float,
    onOpenConcert: (String) -> Unit = {},
    onBuyTickets: (String) -> Unit = {}
) {
    val queue by PlaybackManager.queue.collectAsState()
    val index by PlaybackManager.currentIndex.collectAsState()
    val track = queue.getOrNull(index)

    // Si la cola se vacía (se cerró el reproductor), la pantalla se cierra sola.
    LaunchedEffect(track == null) { if (track == null && sheet.fraction > 0f) sheet.collapse() }
    if (track == null || sheet.fraction <= 0f) return
    BackHandler(enabled = sheet.isExpanded) { sheet.collapse() }

    val accent by animateColorAsState(rememberCoverColor(track.coverUrl), tween(800), label = "accent")
    val dragToClose = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { delta -> sheet.dragBy(delta, screenHeightPx) },
        onDragStopped = { velocity -> sheet.settle(velocity) }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = (1f - sheet.fraction) * size.height
                alpha = (sheet.fraction * 2f).coerceIn(0f, 1f)
            }
            .background(AudioBg)
            // Consume los toques para que no lleguen a la pantalla de abajo.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
    ) {
        PlayerBackdrop(track.coverUrl, accent)
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            if (maxWidth > maxHeight) {
                // Horizontal: portada a la izquierda y controles a la derecha.
                Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(0.42f).fillMaxHeight().then(dragToClose)) {
                        PlayerTopBar(onCollapse = sheet::collapse)
                        Box(Modifier.fillMaxSize().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                            AnimatedCover(track, index, Modifier.fillMaxHeight().aspectRatio(1f))
                        }
                    }
                    Spacer(Modifier.width(24.dp))
                    Column(Modifier.weight(0.58f), verticalArrangement = Arrangement.Center) {
                        TrackTitles(track, onOpenConcert = { sheet.collapse(); onOpenConcert(track.eventoId) })
                        Spacer(Modifier.height(8.dp))
                        PlayerProgressAndControls(track, onBuyTickets = { sheet.collapse(); onBuyTickets(track.eventoId) })
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxWidth().then(dragToClose).padding(horizontal = 24.dp)) {
                        PlayerTopBar(onCollapse = sheet::collapse)
                        Spacer(Modifier.height(8.dp))
                        AnimatedCover(
                            track, index,
                            Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.80f).aspectRatio(1f)
                        )
                        Spacer(Modifier.height(22.dp))
                        TrackTitles(track, onOpenConcert = { sheet.collapse(); onOpenConcert(track.eventoId) })
                    }
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Spacer(Modifier.height(6.dp))
                        PlayerProgressAndControls(track, onBuyTickets = { sheet.collapse(); onBuyTickets(track.eventoId) })
                    }
                    QueueList(queue, index, Modifier.weight(1f))
                }
            }
        }
    }
}

/** Fondo: la portada desenfocada (Android 12+) bajo un degradado con su color dominante. */
@Composable
private fun PlayerBackdrop(coverUrl: String?, accent: Color) {
    if (coverUrl != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Crossfade(coverUrl, animationSpec = tween(700), label = "backdrop") { url ->
            AsyncImage(
                model = url, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(70.dp).graphicsLayer { alpha = 0.55f }
            )
        }
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(accent.copy(alpha = 0.85f), AudioBg.copy(alpha = 0.80f), AudioBg))
        )
    )
}

@Composable
private fun PlayerTopBar(onCollapse: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        PlayerIconButton(Icons.Default.KeyboardArrowDown, "Minimizar el reproductor", iconSize = 32.dp, onClick = onCollapse)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("REPRODUCIENDO DEMO", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Box(Modifier.padding(top = 5.dp).size(width = 36.dp, height = 4.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
        }
        PlayerIconButton(Icons.Default.Close, "Cerrar reproductor", onClick = PlaybackManager::stop)
    }
}

/** Portada grande: se encoge un poco en pausa y se desliza al cambiar de canción. */
@Composable
private fun AnimatedCover(track: QueueTrack, index: Int, modifier: Modifier) {
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val scale by animateFloatAsState(
        if (isPlaying) 1f else 0.86f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow), label = "coverScale"
    )
    AnimatedContent(
        targetState = index to track,
        transitionSpec = {
            val forward = targetState.first >= initialState.first
            (slideInHorizontally(tween(380)) { if (forward) it else -it } + fadeIn(tween(380))) togetherWith
                (slideOutHorizontally(tween(380)) { if (forward) -it else it } + fadeOut(tween(250)))
        },
        contentKey = { it.second.id },
        modifier = modifier,
        label = "cover"
    ) { (_, t) ->
        Box(Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }, contentAlignment = Alignment.Center) {
            CoverArt(
                t.coverUrl, t.title,
                Modifier.fillMaxSize().shadow(28.dp, RoundedCornerShape(18.dp), spotColor = Color.Black),
                radius = 18.dp
            )
        }
    }
}

@Composable
private fun TrackTitles(track: QueueTrack, onOpenConcert: () -> Unit) {
    AnimatedContent(
        targetState = track,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        contentKey = { it.id },
        label = "titles"
    ) { t ->
        Column(Modifier.fillMaxWidth()) {
            Text(t.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(t.artist, color = Color.White.copy(alpha = 0.78f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            // Concierto al que pertenece la demo: lleva a su detalle.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable(onClickLabel = "Ver el concierto", role = Role.Button, onClick = onOpenConcert)
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Place, null, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        t.eventoNombre, color = Color.White.copy(alpha = 0.95f), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    val meta = concertMeta(t.fechaHora, t.escenario)
                    if (meta.isNotBlank()) {
                        Text(meta, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerProgressAndControls(track: QueueTrack, onBuyTickets: () -> Unit) {
    val queue by PlaybackManager.queue.collectAsState()
    val index by PlaybackManager.currentIndex.collectAsState()
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val isBuffering by PlaybackManager.isBuffering.collectAsState()
    val progress by PlaybackManager.progress.collectAsState()
    val durationMs by PlaybackManager.durationMs.collectAsState()
    val shuffle by PlaybackManager.shuffle.collectAsState()
    val haptic = rememberHaptic()

    DemoProgress(progress, durationMs, onSeek = PlaybackManager::seekTo, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        PlayerIconButton(
            Icons.Default.Shuffle, if (shuffle) "Quitar orden aleatorio" else "Activar orden aleatorio",
            tint = if (shuffle) PrimaryGold else Color.White.copy(alpha = 0.8f), enabled = queue.size > 1,
            onClick = { haptic(); PlaybackManager.toggleShuffle() }
        )
        PlayerIconButton(Icons.Default.SkipPrevious, "Anterior", iconSize = 38.dp, size = 56.dp, onClick = { haptic(); PlaybackManager.previous() })
        PlayPauseButton(isPlaying, isBuffering, 72.dp, PrimaryGold, Color.Black) { haptic(); PlaybackManager.playPause() }
        PlayerIconButton(
            Icons.Default.SkipNext, "Siguiente", iconSize = 38.dp, size = 56.dp, enabled = index < queue.lastIndex,
            onClick = { haptic(); PlaybackManager.next() }
        )
        PlayerIconButton(Icons.Default.ConfirmationNumber, "Comprar boletos de ${track.eventoNombre}", tint = Color.White.copy(alpha = 0.8f), onClick = onBuyTickets)
    }
}

@Composable
private fun QueueList(queue: List<QueueTrack>, index: Int, modifier: Modifier) {
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val shuffle by PlaybackManager.shuffle.collectAsState()
    val haptic = rememberHaptic()
    Column(
        modifier = modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color.Black.copy(alpha = 0.28f))
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (shuffle) "COLA · ORDEN ALEATORIO" else "COLA DEL CONCIERTO",
                color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Text("${index + 1} de ${queue.size}", color = TextMuted, fontSize = 11.sp)
        }
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            itemsIndexed(queue, key = { _, t -> t.id }) { i, t ->
                QueueRow(t, isCurrent = i == index, isPlaying = isPlaying && i == index) {
                    haptic()
                    if (i == index) PlaybackManager.playPause() else PlaybackManager.skipTo(i)
                }
            }
        }
    }
}

@Composable
private fun QueueRow(track: QueueTrack, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) PrimaryGold.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClickLabel = if (isCurrent) "Pausar o reanudar" else "Reproducir", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(track.coverUrl, null, Modifier.size(44.dp), radius = 6.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title, color = if (isCurrent) PrimaryGold else Color.White,
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(track.artist, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        if (isCurrent) {
            Equalizer(isPlaying, Modifier.size(width = 18.dp, height = 16.dp))
        } else {
            Text(formatDuration(track.durationSec), color = TextMuted, fontSize = 12.sp)
        }
    }
}

/** Botón redondo de reproducir/pausar; se hunde un poco al tocarlo y muestra la carga. */
@Composable
internal fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    size: Dp,
    background: Color = Color.White,
    tint: Color = Color.Black,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "press")
    val label = if (isPlaying) "Pausar" else "Reproducir"
    Box(
        modifier = Modifier
            .size(maxOf(size, 48.dp)) // área táctil mínima
            .semantics { contentDescription = label }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(if (size >= 60.dp) 12.dp else 0.dp, CircleShape)
                .background(background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(color = tint, strokeWidth = 2.5.dp, modifier = Modifier.size(size * 0.45f))
            } else {
                Crossfade(isPlaying, animationSpec = tween(160), label = "playPauseIcon") { playing ->
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null, tint = tint, modifier = Modifier.size(size * 0.58f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlayerIconButton(
    icon: ImageVector,
    description: String,
    tint: Color = Color.White,
    iconSize: Dp = 26.dp,
    size: Dp = 48.dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(size)) {
        Icon(icon, description, tint = if (enabled) tint else tint.copy(alpha = 0.3f), modifier = Modifier.size(iconSize))
    }
}

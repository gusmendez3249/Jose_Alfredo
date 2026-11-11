package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.playback.PlaybackManager
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.utils.mediaUrl
import mx.utng.festivaltrack.app.ui.viewmodels.EventDemosViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.EventosViewModel
import mx.utng.festivaltrack.shared.data.remote.CancionDto

/**
 * Detalle de un concierto con su playlist de demos, al estilo de un reproductor de música:
 * cabecera con portadas y color dinámico, botón grande de reproducir, lista de canciones con
 * ecualizador en la que suena y acceso a la compra de boletos. El audio vive en la cola global
 * ([PlaybackManager]); esta pantalla solo lo muestra y lo controla.
 */
@Composable
fun EventDetailScreen(
    eventoId: String,
    eventosViewModel: EventosViewModel? = null,
    onNavigateBack: () -> Unit = {},
    onBuyTickets: (String) -> Unit = {},
    viewModel: EventDemosViewModel = viewModel()
) {
    val eventos by eventosViewModel?.eventosLocales?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val evento = eventos.firstOrNull { it.id == eventoId }

    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val queue by PlaybackManager.queue.collectAsState()
    val queueIndex by PlaybackManager.currentIndex.collectAsState()
    val isPlayingNow by PlaybackManager.isPlaying.collectAsState()
    val shuffle by PlaybackManager.shuffle.collectAsState()
    val activeTrack = queue.getOrNull(queueIndex)
    val thisConcertActive = activeTrack?.eventoId == eventoId
    val currentId = if (thisConcertActive) activeTrack?.id else null
    val isPlaying = thisConcertActive && isPlayingNow

    val listState = rememberLazyListState()
    val haptic = rememberHaptic()

    val current = songs.firstOrNull { it.id == currentId }
    val covers = remember(songs) { songs.mapNotNull { mediaUrl(it.portadaUrl) }.distinct() }
    val accent by animateColorAsState(
        targetValue = rememberCoverColor(current?.let { mediaUrl(it.portadaUrl) } ?: covers.firstOrNull()),
        animationSpec = tween(700),
        label = "accent"
    )
    val showTopBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 520 } }

    LaunchedEffect(eventoId) { viewModel.load(eventoId) }

    Scaffold(
        containerColor = AudioBg,
        bottomBar = {
            Button(
                onClick = { haptic(); onBuyTickets(eventoId) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AudioBg)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(50.dp)
            ) {
                Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("COMPRAR BOLETOS DEL CONCIERTO", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, fontSize = 13.sp)
            }
        }
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                item {
                    PlaylistHeader(
                        title = evento?.nombre ?: "Concierto del festival",
                        date = formatConcertDateTime(evento?.fechaHora),
                        stage = evento?.escenario.orEmpty(),
                        songs = songs,
                        covers = covers,
                        accent = accent,
                        isLoading = isLoading,
                        isPlaying = isPlaying,
                        shuffle = shuffle,
                        onPlayAll = { haptic(); viewModel.playAll(evento, thisConcertActive) },
                        onShuffle = { haptic(); PlaybackManager.toggleShuffle() }
                    )
                }

                when {
                    isLoading -> items(5) { TrackRowSkeleton() }
                    loadError != null -> item {
                        StatusMessage(
                            icon = Icons.Default.CloudOff,
                            title = "No pudimos cargar las canciones",
                            text = "Revisa tu conexión a internet e inténtalo de nuevo.",
                            action = "Reintentar",
                            onAction = viewModel::retry
                        )
                    }
                    songs.isEmpty() -> item {
                        StatusMessage(
                            icon = Icons.Default.MusicOff,
                            title = "Aún no hay demos para este concierto",
                            text = "El repertorio se publica antes del concierto. Vuelve pronto para escuchar un " +
                                "adelanto; mientras tanto ya puedes comprar tus boletos."
                        )
                    }
                    else -> itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                        TrackRow(
                            index = index + 1,
                            song = song,
                            isCurrent = currentId == song.id,
                            isPlaying = currentId == song.id && isPlaying,
                            onClick = { haptic(); viewModel.toggle(song, evento, currentId) }
                        )
                    }
                }
            }

            // Botón de regreso flotante; al bajar se vuelve una barra con el título.
            val barColor by animateColorAsState(if (showTopBar) AudioBg else Color.Transparent, label = "bar")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barColor)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.background(Color.Black.copy(alpha = if (showTopBar) 0f else 0.4f), CircleShape)
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Color.White) }
                if (showTopBar) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        evento?.nombre ?: "Concierto",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─── Cabecera ────────────────────────────────────────────────────────────────

@Composable
private fun PlaylistHeader(
    title: String,
    date: String,
    stage: String,
    songs: List<CancionDto>,
    covers: List<String>,
    accent: Color,
    isLoading: Boolean,
    isPlaying: Boolean,
    shuffle: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.5f), AudioBg)))
            .statusBarsPadding()
            .padding(top = 56.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
    ) {
        CoverCollage(
            covers, loading = isLoading, size = if (!isLoading && songs.isEmpty()) 132.dp else 214.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(22.dp))
        Text("CONCIERTO DEL FESTIVAL", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Text(title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 31.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (date.isNotBlank()) InfoChip(Icons.Default.Event, date)
            if (stage.isNotBlank()) InfoChip(Icons.Default.Place, stage)
        }
        Spacer(Modifier.height(10.dp))
        if (isLoading) {
            SkeletonBlock(Modifier.size(width = 190.dp, height = 14.dp))
        } else {
            Text(
                when (songs.size) {
                    0 -> "Sin demos por ahora"
                    1 -> "1 canción · demo de hasta 30 s"
                    else -> "${songs.size} canciones · demos de hasta 30 s cada una"
                },
                color = Color.White.copy(alpha = 0.66f), fontSize = 12.sp
            )
        }
        // Sin demos no hay nada que reproducir: se ocultan los controles y sube el aviso.
        if (!isLoading && songs.isEmpty()) return@Column
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val canShuffle = songs.size > 1
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (shuffle) PrimaryGold.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f))
                    .border(1.dp, if (shuffle) PrimaryGold else Color.Transparent, RoundedCornerShape(50))
                    .clickable(enabled = canShuffle, role = Role.Switch, onClick = onShuffle)
                    .semantics { contentDescription = if (shuffle) "Orden aleatorio activado" else "Activar orden aleatorio" }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tint = when {
                    !canShuffle -> Color.White.copy(alpha = 0.3f)
                    shuffle -> PrimaryGold
                    else -> Color.White.copy(alpha = 0.85f)
                }
                Icon(Icons.Default.Shuffle, null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Aleatorio", color = tint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.graphicsLayer { alpha = if (songs.isEmpty()) 0.35f else 1f }) {
                PlayPauseButton(isPlaying, isBuffering = false, size = 62.dp, background = PrimaryGold, tint = Color.Black) {
                    if (songs.isNotEmpty()) onPlayAll()
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.28f)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Portadas del concierto: mosaico de 2×2 con cuatro o más; con menos, la primera al frente y las
 * demás asomando detrás como discos apilados.
 */
@Composable
private fun CoverCollage(covers: List<String>, loading: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            loading -> SkeletonBlock(Modifier.fillMaxSize(), shape)
            covers.size >= 4 -> Column(Modifier.fillMaxSize().shadow(20.dp, shape).clip(shape)) {
                for (row in 0..1) {
                    Row(Modifier.weight(1f)) {
                        for (col in 0..1) CoverArt(covers[row * 2 + col], null, Modifier.weight(1f).fillMaxHeight(), radius = 0.dp)
                    }
                }
            }
            else -> {
                covers.getOrNull(2)?.let { CoverArt(it, null, Modifier.fillMaxSize(0.84f).rotate(-9f).graphicsLayer { translationX = -34f; alpha = 0.75f }, radius = 14.dp) }
                covers.getOrNull(1)?.let { CoverArt(it, null, Modifier.fillMaxSize(0.84f).rotate(8f).graphicsLayer { translationX = 34f; alpha = 0.85f }, radius = 14.dp) }
                CoverArt(
                    covers.firstOrNull(), null,
                    Modifier.fillMaxSize(if (covers.size > 1) 0.9f else 1f).shadow(20.dp, shape),
                    radius = 16.dp
                )
            }
        }
    }
}

// ─── Lista de canciones ──────────────────────────────────────────────────────

@Composable
private fun TrackRow(index: Int, song: CancionDto, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(if (isCurrent) PrimaryGold.copy(alpha = 0.10f) else Color.Transparent, label = "rowBg")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(
                onClickLabel = if (isCurrent) (if (isPlaying) "Pausar" else "Reanudar") else "Reproducir la demo",
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
            if (isCurrent) Equalizer(isPlaying, Modifier.size(width = 18.dp, height = 16.dp))
            else Text("$index", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        CoverArt(mediaUrl(song.portadaUrl), song.titulo, Modifier.size(52.dp), radius = 8.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.titulo,
                color = if (isCurrent) PrimaryGold else Color.White,
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(song.artista, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(formatDuration(song.duracion), color = TextMuted, fontSize = 13.sp)
    }
}

/** Fila fantasma que se muestra mientras cargan las canciones. */
@Composable
private fun TrackRowSkeleton() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        SkeletonBlock(Modifier.size(width = 14.dp, height = 14.dp))
        Spacer(Modifier.width(14.dp))
        SkeletonBlock(Modifier.size(52.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(14.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.4f).height(11.dp))
        }
        SkeletonBlock(Modifier.size(width = 30.dp, height = 12.dp))
    }
}

// ─── Estados ─────────────────────────────────────────────────────────────────

/** Estado vacío o de error: ilustración, explicación y, si aplica, un botón para reintentar. */
@Composable
internal fun StatusMessage(icon: ImageVector, title: String, text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(112.dp).background(PrimaryGold.copy(alpha = 0.06f), CircleShape))
            Box(Modifier.size(80.dp).background(PrimaryGold.copy(alpha = 0.10f), CircleShape))
            Icon(icon, null, tint = PrimaryGold, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, color = TextMuted, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                shape = RoundedCornerShape(50),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(action, fontWeight = FontWeight.Bold)
            }
        }
    }
}

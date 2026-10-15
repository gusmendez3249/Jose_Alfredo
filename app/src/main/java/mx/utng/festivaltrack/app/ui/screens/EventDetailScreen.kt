package mx.utng.festivaltrack.app.ui.screens

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.utils.mediaUrl
import mx.utng.festivaltrack.app.ui.viewmodels.EventDemosViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.EventosViewModel
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ScreenBg = Color(0xFF0E120F)
private val TileBg = Color(0xFF26352B)
private val DefaultAccent = Color(0xFF2C5A3E)

/**
 * Detalle de un concierto con su playlist de demos, al estilo de un reproductor de música:
 * cabecera con portadas y color dinámico, botón grande de reproducir, lista de canciones con
 * ecualizador en la que suena, minirreproductor con progreso y acceso a la compra de boletos.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val currentId by viewModel.currentId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val playbackError by viewModel.playbackError.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val shuffle by viewModel.shuffle.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val current = songs.firstOrNull { it.id == currentId }
    val covers = remember(songs) { songs.mapNotNull { mediaUrl(it.portadaUrl) }.distinct() }
    val accent by animateColorAsState(
        targetValue = rememberAccentColor(current?.let { mediaUrl(it.portadaUrl) } ?: covers.firstOrNull()),
        animationSpec = tween(700),
        label = "accent"
    )
    val showTopBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 520 } }

    LaunchedEffect(eventoId) { viewModel.load(eventoId) }
    LaunchedEffect(playbackError) {
        playbackError?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissPlaybackError()
        }
    }
    // Al salir de la pantalla se detiene la reproducción.
    DisposableEffect(Unit) { onDispose { viewModel.stop() } }

    Scaffold(
        containerColor = ScreenBg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(modifier = Modifier.background(ScreenBg)) {
                AnimatedVisibility(
                    visible = current != null,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    if (current != null) {
                        MiniPlayer(
                            song = current,
                            accent = accent,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            progress = progress,
                            onPlayPause = viewModel::playPause,
                            onNext = viewModel::next,
                            onPrevious = viewModel::previous
                        )
                    }
                }
                Button(
                    onClick = { onBuyTickets(eventoId) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("COMPRAR BOLETOS DEL CONCIERTO", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, fontSize = 13.sp)
                }
            }
        }
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                item {
                    PlaylistHeader(
                        title = evento?.nombre ?: "Concierto del festival",
                        meta = listOfNotNull(
                            evento?.fechaHora?.let(::formatEventDate)?.takeIf { it.isNotBlank() },
                            evento?.escenario?.takeIf { it.isNotBlank() }
                        ).joinToString(" • "),
                        songs = songs,
                        covers = covers,
                        accent = accent,
                        isPlaying = isPlaying && currentId != null,
                        isBuffering = isBuffering,
                        shuffle = shuffle,
                        onPlayAll = viewModel::playAll,
                        onShuffle = viewModel::toggleShuffle
                    )
                }

                when {
                    isLoading -> item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryGold)
                        }
                    }
                    loadError != null -> item { StatusMessage(loadError!!, "Reintentar", viewModel::retry) }
                    songs.isEmpty() -> item { StatusMessage("Aún no hay demos para este concierto") }
                    else -> itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                        TrackRow(
                            index = index + 1,
                            song = song,
                            isCurrent = currentId == song.id,
                            isPlaying = currentId == song.id && isPlaying,
                            onClick = { viewModel.toggle(song) }
                        )
                    }
                }
            }

            // Botón de regreso flotante; al bajar se vuelve una barra con el título.
            val barColor by animateColorAsState(if (showTopBar) ScreenBg else Color.Transparent, label = "bar")
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
    meta: String,
    songs: List<CancionDto>,
    covers: List<String>,
    accent: Color,
    isPlaying: Boolean,
    isBuffering: Boolean,
    shuffle: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.55f), ScreenBg)))
            .statusBarsPadding()
            .padding(top = 56.dp, start = 20.dp, end = 20.dp, bottom = 8.dp)
    ) {
        CoverCollage(covers, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(20.dp))
        Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
        if (meta.isNotBlank()) {
            Text(meta, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        val totalMin = songs.sumOf { it.duracion } / 60
        Text(
            "Playlist del concierto · ${songs.size} ${if (songs.size == 1) "canción" else "canciones"}" +
                if (songs.isNotEmpty()) " · ${formatTotal(totalMin)}" else "",
            color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onShuffle, enabled = songs.size > 1) {
                Icon(
                    Icons.Default.Shuffle, if (shuffle) "Aleatorio activado" else "Activar aleatorio",
                    tint = if (shuffle) PrimaryGold else Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(10.dp, CircleShape)
                    .background(PrimaryGold, CircleShape)
                    .clickable(enabled = songs.isNotEmpty(), onClick = onPlayAll),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (isPlaying) "Pausar" else "Reproducir todo",
                    tint = Color.Black, modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

/** Mosaico de 2×2 con las portadas de las canciones; con menos de 4 se muestra una sola portada grande. */
@Composable
private fun CoverCollage(covers: List<String>, modifier: Modifier = Modifier) {
    val size = 210.dp
    val shape = RoundedCornerShape(14.dp)
    Box(modifier = modifier.size(size).shadow(18.dp, shape).clip(shape).background(TileBg)) {
        when {
            covers.size >= 4 -> Column {
                Row(Modifier.weight(1f)) {
                    CoverTile(covers[0], Modifier.weight(1f).fillMaxHeight())
                    CoverTile(covers[1], Modifier.weight(1f).fillMaxHeight())
                }
                Row(Modifier.weight(1f)) {
                    CoverTile(covers[2], Modifier.weight(1f).fillMaxHeight())
                    CoverTile(covers[3], Modifier.weight(1f).fillMaxHeight())
                }
            }
            covers.isNotEmpty() -> CoverTile(covers[0], Modifier.fillMaxSize())
            else -> Box(
                modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF3A7A52), Color(0xFF1B3A28)))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.MusicNote, null, tint = PrimaryGold, modifier = Modifier.size(84.dp)) }
        }
    }
}

@Composable
private fun CoverTile(url: String, modifier: Modifier) {
    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}

// ─── Lista de canciones ──────────────────────────────────────────────────────

@Composable
private fun TrackRow(index: Int, song: CancionDto, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(26.dp), contentAlignment = Alignment.CenterStart) {
            if (isPlaying) Equalizer(PrimaryGold)
            else Text("$index", color = if (isCurrent) PrimaryGold else Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Cover(song, size = 50.dp, radius = 6.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.titulo,
                color = if (isCurrent) PrimaryGold else Color.White,
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(song.artista, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatDuration(song.duracion), color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
private fun Cover(song: CancionDto, size: androidx.compose.ui.unit.Dp, radius: androidx.compose.ui.unit.Dp) {
    val url = mediaUrl(song.portadaUrl)
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(radius)).background(TileBg),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = "Portada de ${song.titulo}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** Tres barras animadas, como el indicador de "sonando" de los reproductores. */
@Composable
private fun Equalizer(color: Color) {
    val t = rememberInfiniteTransition(label = "eq")
    val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(430, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(1f, 0.3f, infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    val c by t.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "c")
    Row(modifier = Modifier.height(16.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        listOf(a, b, c).forEach { h ->
            Box(Modifier.width(3.dp).fillMaxHeight(h).background(color, RoundedCornerShape(1.dp)))
        }
    }
}

// ─── Minirreproductor ────────────────────────────────────────────────────────

@Composable
private fun MiniPlayer(
    song: CancionDto,
    accent: Color,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.95f), Color(0xFF16221A))))
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(song, size = 44.dp, radius = 6.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artista, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Anterior", tint = Color.White) }
            Box(
                modifier = Modifier.size(42.dp).background(Color.White, CircleShape).clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                if (isBuffering) CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (isPlaying) "Pausar" else "Reproducir", tint = Color.Black, modifier = Modifier.size(26.dp)
                )
            }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Siguiente", tint = Color.White) }
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = PrimaryGold,
            trackColor = Color.White.copy(alpha = 0.18f)
        )
    }
}

// ─── Estados y utilidades ────────────────────────────────────────────────────

@Composable
private fun StatusMessage(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(8.dp))
        Text(text, color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                shape = RoundedCornerShape(50)
            ) { Text(action, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Color dominante (promedio, oscurecido) de una portada, para teñir la cabecera. */
@Composable
private fun rememberAccentColor(url: String?): Color {
    val context = LocalContext.current
    var color by remember(url) { mutableStateOf(DefaultAccent) }
    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        val result = context.imageLoader.execute(
            ImageRequest.Builder(context).data(url).allowHardware(false).size(48).build()
        )
        val bmp = (result.drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
        val px = Bitmap.createScaledBitmap(bmp, 1, 1, true).getPixel(0, 0)
        val r = android.graphics.Color.red(px) * 0.62f
        val g = android.graphics.Color.green(px) * 0.62f
        val b = android.graphics.Color.blue(px) * 0.62f
        color = Color(r / 255f, g / 255f, b / 255f)
    }
    return color
}

/** 185 -> "3:05" */
internal fun formatDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun formatTotal(minutes: Int): String =
    if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "${minutes.coerceAtLeast(1)} min"

private fun formatEventDate(iso: String): String = try {
    val fmt = DateTimeFormatter.ofPattern("d 'de' MMMM, HH:mm 'hrs'", Locale("es", "MX"))
    fmt.format(Instant.parse(iso).atZone(ZoneId.systemDefault()))
} catch (e: Exception) {
    ""
}

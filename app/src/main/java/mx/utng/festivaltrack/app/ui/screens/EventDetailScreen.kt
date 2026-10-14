package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.EventDemosViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.EventosViewModel
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardBg = Color(0xFF1E2720)
private val CardBorder = Color(0xFF2E3D31)

/**
 * Detalle de un concierto: datos del evento, demos de audio de las canciones del tributo
 * (una a la vez) y acceso a la compra de boletos de ese concierto.
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

    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(eventoId) { viewModel.load(eventoId) }
    LaunchedEffect(playbackError) {
        playbackError?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissPlaybackError()
        }
    }
    // Al salir de la pantalla se detiene la demo.
    DisposableEffect(Unit) { onDispose { viewModel.stop() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Concierto", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 4.dp) {
                Button(
                    onClick = { onBuyTickets(eventoId) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(52.dp)
                ) {
                    Text("COMPRAR BOLETOS DEL CONCIERTO", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
                    Text(
                        evento?.nombre ?: "Concierto del festival",
                        color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold
                    )
                    val details = listOfNotNull(
                        evento?.fechaHora?.let(::formatEventDate),
                        evento?.escenario?.takeIf { it.isNotBlank() },
                        evento?.ubicacion?.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    if (details.isNotBlank()) {
                        Text(details, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("CANCIONES DEL TRIBUTO", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(
                        "Toca una canción para escuchar un fragmento de ${EventDemosViewModel.DEMO_MS / 1000} segundos",
                        color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            when {
                isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryGold)
                    }
                }
                loadError != null -> item {
                    StatusMessage(
                        text = loadError!!,
                        action = "Reintentar",
                        onAction = viewModel::retry
                    )
                }
                songs.isEmpty() -> item {
                    StatusMessage(text = "Aún no hay demos para este concierto")
                }
                else -> itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                    DemoRow(
                        index = index + 1,
                        song = song,
                        isCurrent = currentId == song.id,
                        isPlaying = currentId == song.id && isPlaying,
                        isBuffering = currentId == song.id && isBuffering,
                        onClick = { viewModel.toggle(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoRow(
    index: Int,
    song: CancionDto,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) PrimaryGold.copy(alpha = 0.12f) else CardBg, shape)
            .border(1.dp, if (isCurrent) PrimaryGold else CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(if (isCurrent) PrimaryGold else Color(0xFF2A3A2E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                isBuffering -> CircularProgressIndicator(
                    color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp)
                )
                isPlaying -> Icon(Icons.Default.Pause, "Pausar ${song.titulo}", tint = Color.Black)
                else -> Icon(Icons.Default.PlayArrow, "Reproducir ${song.titulo}", tint = if (isCurrent) Color.Black else PrimaryGold)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "$index. ${song.titulo}",
                color = if (isCurrent) PrimaryGold else Color.White,
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(song.artista, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatDuration(song.duracion), color = Color.LightGray, fontSize = 13.sp)
    }
}

@Composable
private fun StatusMessage(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(8.dp))
        Text(text, color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) { Text(action, fontWeight = FontWeight.Bold) }
        }
    }
}

/** 185 -> "3:05" */
internal fun formatDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun formatEventDate(iso: String): String = try {
    val fmt = DateTimeFormatter.ofPattern("d 'de' MMMM, HH:mm 'hrs'", Locale("es", "MX"))
    fmt.format(Instant.parse(iso).atZone(ZoneId.systemDefault()))
} catch (e: Exception) {
    ""
}

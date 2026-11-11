package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.playback.PlaybackManager
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.ConcertsViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.EventosViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.RepertoireSummary
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity

/**
 * Lista de conciertos del festival. Cada tarjeta muestra las portadas de su repertorio, la fecha,
 * el escenario y si ya tiene demos. Al elegir uno se abre su detalle, donde se escuchan las demos
 * y se compran los boletos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcertsScreen(
    eventosViewModel: EventosViewModel? = null,
    onOpenEvent: (String) -> Unit = {},
    viewModel: ConcertsViewModel = viewModel()
) {
    val eventos by eventosViewModel?.eventosLocales?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val summaries by viewModel.summaries.collectAsState()
    val queue by PlaybackManager.queue.collectAsState()
    val queueIndex by PlaybackManager.currentIndex.collectAsState()
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val activeEventId = queue.getOrNull(queueIndex)?.eventoId
    val haptic = rememberHaptic()

    val ids = eventos.map { it.id }
    LaunchedEffect(ids) { viewModel.load(ids) }

    Scaffold(
        containerColor = AudioBg,
        topBar = {
            TopAppBar(
                title = { Text("Conciertos", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Icon(
                        Icons.Default.Audiotrack, null, tint = PrimaryGold,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AudioBg)
            )
        }
    ) { inner ->
        if (eventos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                StatusMessage(
                    icon = Icons.Default.EventBusy,
                    title = "Aún no hay conciertos",
                    text = "Cuando haya conciertos programados aparecerán aquí para que escuches sus demos."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Icon(Icons.Default.Headphones, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Escucha un fragmento de las canciones de cada concierto y decide a cuál ir.",
                            color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp
                        )
                    }
                }
                items(eventos, key = { it.id }) { evento ->
                    ConcertCard(
                        evento = evento,
                        summary = summaries[evento.id] ?: RepertoireSummary.Loading,
                        isActive = evento.id == activeEventId,
                        isPlaying = evento.id == activeEventId && isPlaying,
                        onClick = { haptic(); onOpenEvent(evento.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConcertCard(
    evento: EventoEntity,
    summary: RepertoireSummary,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val meta = concertMeta(evento.fechaHora, evento.escenario)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AudioCard)
            .border(1.dp, if (isActive) PrimaryGold.copy(alpha = 0.7f) else AudioCardBorder, shape)
            .clickable(onClickLabel = "Abrir el concierto", onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(92.dp)) {
            CardArtwork(summary)
            concertDayAndMonth(evento.fechaHora)?.let { (day, month) ->
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "$day de $month" },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(day, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 16.sp)
                    Text(month, color = PrimaryGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 10.sp)
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                evento.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            if (meta.isNotBlank()) {
                Text(
                    meta, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            RepertoireBadge(summary, isActive, isPlaying)
        }
        Icon(Icons.Default.ChevronRight, null, tint = PrimaryGold.copy(alpha = 0.8f))
    }
}

/** Portadas del repertorio: mosaico 2×2 con cuatro, una sola con menos, o un marcador si no hay. */
@Composable
private fun CardArtwork(summary: RepertoireSummary) {
    val shape = RoundedCornerShape(12.dp)
    when {
        summary is RepertoireSummary.Loading -> SkeletonBlock(Modifier.fillMaxSize(), shape)
        summary is RepertoireSummary.Loaded && summary.covers.size >= 4 -> Column(Modifier.fillMaxSize().clip(shape)) {
            for (row in 0..1) {
                Row(Modifier.weight(1f)) {
                    for (col in 0..1) CoverArt(summary.covers[row * 2 + col], null, Modifier.weight(1f).fillMaxHeight(), radius = 0.dp)
                }
            }
        }
        summary is RepertoireSummary.Loaded && summary.songCount > 0 ->
            CoverArt(summary.covers.firstOrNull(), null, Modifier.fillMaxSize(), radius = 12.dp)
        else -> Box(
            Modifier.fillMaxSize().clip(shape).background(Brush.linearGradient(listOf(Color(0xFF233128), Color(0xFF151D18)))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Audiotrack, null, tint = Color.White.copy(alpha = 0.22f), modifier = Modifier.size(38.dp)) }
    }
}

/** Estado del repertorio: sonando, con demos, sin repertorio o sin poder consultarlo. */
@Composable
private fun RepertoireBadge(summary: RepertoireSummary, isActive: Boolean, isPlaying: Boolean) {
    when {
        isActive -> Badge(if (isPlaying) "Sonando ahora" else "En pausa", PrimaryGold, filled = true) {
            Equalizer(isPlaying, Modifier.size(width = 13.dp, height = 11.dp), color = Color.Black)
        }
        summary is RepertoireSummary.Loading -> SkeletonBlock(Modifier.size(width = 96.dp, height = 22.dp), RoundedCornerShape(50))
        summary is RepertoireSummary.Loaded && summary.songCount > 0 ->
            Badge(if (summary.songCount == 1) "1 demo para escuchar" else "${summary.songCount} demos para escuchar", PrimaryGold) {
                Icon(Icons.Default.Headphones, null, tint = PrimaryGold, modifier = Modifier.size(13.dp))
            }
        summary is RepertoireSummary.Loaded -> Badge("Repertorio por anunciar", Color.White.copy(alpha = 0.6f))
        else -> Badge("Ver concierto", Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun Badge(text: String, color: Color, filled: Boolean = false, leading: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color else color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = if (filled) Color.Black else color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

package mx.utng.festivaltrack.app.ui.screens

import android.graphics.drawable.BitmapDrawable
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import mx.utng.festivaltrack.app.playback.PlaybackLogic
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// Piezas visuales que comparten las pantallas de audio: colores, portada, ecualizador,
// barra de progreso, esqueletos de carga y formatos de fecha y duración.

internal val AudioBg = Color(0xFF0D120E)
internal val AudioSurface = Color(0xFF16221A)
internal val AudioCard = Color(0xFF1A241D)
internal val AudioCardBorder = Color(0xFF2A3A2E)
internal val AudioTile = Color(0xFF26352B)
internal val AudioAccent = Color(0xFF2C5A3E)
internal val TextMuted = Color.White.copy(alpha = 0.62f)

/** Vibración corta y suave para confirmar un toque en los controles del reproductor. */
@Composable
internal fun rememberHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } }
}

/** Portada de una canción; sin imagen muestra un degradado con una nota musical. */
@Composable
internal fun CoverArt(url: String?, title: String?, modifier: Modifier = Modifier, radius: Dp = 8.dp) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(listOf(Color(0xFF3A7A52), Color(0xFF1B3A28)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.MusicNote, null, tint = PrimaryGold.copy(alpha = 0.85f), modifier = Modifier.fillMaxSize(0.45f))
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = title?.let { "Portada de $it" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Indicador de "sonando": barras que suben y bajan. En pausa se quedan quietas y bajas, de modo
 * que se distingue la canción activa aunque no esté sonando.
 */
@Composable
internal fun Equalizer(playing: Boolean, modifier: Modifier = Modifier, color: Color = PrimaryGold, bars: Int = 4) {
    val transition = rememberInfiniteTransition(label = "eq")
    val t by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "t"
    )
    val activity by animateFloatAsState(if (playing) 1f else 0f, tween(350), label = "activity")
    Canvas(
        modifier = modifier.semantics { stateDescription = if (playing) "Sonando" else "En pausa" }
    ) {
        val gap = size.width / (bars * 2 - 1)
        for (i in 0 until bars) {
            // Frecuencias enteras: el ciclo se repite sin saltos.
            val wave = abs(sin(2 * PI * (t * (i % 3 + 1) + i * 0.23))).toFloat()
            val level = 0.2f + (0.25f + 0.75f * wave - 0.2f) * activity
            val h = size.height * level
            drawRoundRect(
                color = color,
                topLeft = Offset(i * gap * 2, size.height - h),
                size = Size(gap, h),
                cornerRadius = CornerRadius(gap / 2, gap / 2)
            )
        }
    }
}

/**
 * Barra de progreso de la demo que se puede tocar o arrastrar para moverse dentro del fragmento.
 * Mientras se arrastra muestra la posición elegida; al soltar llama a [onSeek].
 */
@Composable
internal fun DemoProgress(
    progress: Float,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = PrimaryGold
) {
    var scrub by remember { mutableStateOf<Float?>(null) }
    val smooth by animateFloatAsState(progress, tween(120, easing = LinearEasing), label = "progress")
    val shown = scrub ?: smooth
    val thumb by animateDpAsState(if (scrub != null) 9.dp else 6.dp, label = "thumb")
    val seek by rememberUpdatedState(onSeek)
    val haptic = rememberHaptic()

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp) // área táctil cómoda aunque la barra sea delgada
                .semantics {
                    contentDescription = "Avance de la demo"
                    progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                    setProgress { value -> seek(value); true }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        haptic()
                        seek((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> scrub = (offset.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            scrub?.let { seek(it) }
                            scrub = null
                            haptic()
                        },
                        onDragCancel = { scrub = null }
                    ) { change, _ -> scrub = (change.position.x / size.width).coerceIn(0f, 1f) }
                }
        ) {
            val y = size.height / 2
            val stroke = 4.dp.toPx()
            val radius = CornerRadius(stroke / 2, stroke / 2)
            drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(0f, y - stroke / 2), Size(size.width, stroke), radius)
            drawRoundRect(color, Offset(0f, y - stroke / 2), Size(size.width * shown, stroke), radius)
            drawCircle(Color.White, thumb.toPx(), Offset(size.width * shown, y))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                PlaybackLogic.formatMs(PlaybackLogic.seekPositionMs(shown, durationMs)),
                color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium
            )
            Text(PlaybackLogic.formatMs(durationMs), color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Brillo que recorre un bloque mientras se carga el contenido (esqueleto). */
internal fun Modifier.shimmer(shape: RoundedCornerShape = RoundedCornerShape(8.dp)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        -1f, 2f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "x"
    )
    clip(shape).background(
        Brush.linearGradient(
            colors = listOf(AudioTile, Color(0xFF3A4C40), AudioTile),
            start = Offset(x * 600f, 0f),
            end = Offset(x * 600f + 400f, 200f)
        )
    )
}

/** Bloque gris de un esqueleto de carga. */
@Composable
internal fun SkeletonBlock(modifier: Modifier, shape: RoundedCornerShape = RoundedCornerShape(8.dp)) {
    Box(modifier.shimmer(shape))
}

/**
 * Color dominante de una portada, oscurecido para usarlo de fondo bajo texto blanco.
 * Mientras carga (o sin portada) devuelve el verde de la app.
 */
@Composable
internal fun rememberCoverColor(url: String?): Color {
    val context = LocalContext.current
    var color by remember(url) { mutableStateOf(AudioAccent) }
    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        val result = context.imageLoader.execute(
            ImageRequest.Builder(context).data(url).allowHardware(false).size(24).build()
        )
        val bmp = (result.drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
        val pixels = IntArray(bmp.width * bmp.height)
        bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        color = dominantColor(pixels)
    }
    return color
}

/** Promedio de los pixeles, dando más peso a los colores vivos, y limitado a un tono oscuro. */
internal fun dominantColor(pixels: IntArray): Color {
    if (pixels.isEmpty()) return AudioAccent
    var r = 0f; var g = 0f; var b = 0f; var total = 0f
    for (px in pixels) {
        val pr = (px shr 16 and 0xFF) / 255f
        val pg = (px shr 8 and 0xFF) / 255f
        val pb = (px and 0xFF) / 255f
        val weight = 0.15f + (maxOf(pr, pg, pb) - minOf(pr, pg, pb)) // saturación aproximada
        r += pr * weight; g += pg * weight; b += pb * weight; total += weight
    }
    r /= total; g /= total; b /= total
    // Se limita el brillo para que el texto blanco siempre se lea encima.
    val max = maxOf(r, g, b)
    val scale = if (max > 0.55f) 0.55f / max else 1f
    return Color(r * scale, g * scale, b * scale)
}

/** 185 -> "3:05" */
internal fun formatDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/** "2026-11-21T02:00:00Z" -> "20 de noviembre, 20:00 hrs" (hora local); vacío si no se puede leer. */
internal fun formatConcertDateTime(iso: String?): String = try {
    val fmt = DateTimeFormatter.ofPattern("d 'de' MMMM, HH:mm 'hrs'", Locale("es", "MX"))
    fmt.format(Instant.parse(iso).atZone(ZoneId.systemDefault()))
} catch (e: Exception) {
    ""
}

/** Día y mes abreviado para la etiqueta de fecha de una tarjeta: ("20", "NOV"). */
internal fun concertDayAndMonth(iso: String?): Pair<String, String>? = try {
    val date = Instant.parse(iso).atZone(ZoneId.systemDefault())
    val month = DateTimeFormatter.ofPattern("MMM", Locale("es", "MX")).format(date).trimEnd('.').uppercase()
    date.dayOfMonth.toString() to month
} catch (e: Exception) {
    null
}

/** "Escenario Principal · 20 de noviembre, 20:00 hrs" con los datos que existan. */
internal fun concertMeta(fechaHora: String?, escenario: String?): String = listOfNotNull(
    formatConcertDateTime(fechaHora).takeIf { it.isNotBlank() },
    escenario?.takeIf { it.isNotBlank() }
).joinToString(" • ")

package mx.utng.festivaltrack.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.app.playback.PlaybackLogic
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.utils.mediaUrl
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.RepertorioDto

private val RowHeight = 60.dp

/**
 * Diálogo del administrador para definir el repertorio de un concierto: se agregan canciones del
 * catálogo y se ordenan arrastrándolas por el asa (o con las flechas). Lo guardado es lo que los
 * asistentes escuchan como demos en el detalle del evento, en ese orden.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepertoireDialog(
    eventoId: String,
    eventoNombre: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { FestivalApiService.create() }
    val haptic = rememberHaptic()
    val rowHeightPx = with(LocalDensity.current) { RowHeight.toPx() }

    var catalogo by remember { mutableStateOf<List<CancionDto>>(emptyList()) }
    /** Ids elegidos, en el orden de reproducción. */
    var seleccion by remember { mutableStateOf<List<String>>(emptyList()) }
    var guardado by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    // Fila que se está arrastrando y cuánto se ha desplazado respecto a su lugar actual.
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    fun load() {
        scope.launch {
            isLoading = true
            loadFailed = false
            try {
                catalogo = api.getCanciones()
                seleccion = api.getCancionesEvento(eventoId).map { it.id }
                guardado = seleccion
            } catch (e: Exception) {
                loadFailed = true
            } finally {
                isLoading = false
            }
        }
    }
    LaunchedEffect(eventoId) { load() }

    fun move(id: String, delta: Int) {
        val i = seleccion.indexOf(id)
        seleccion = PlaybackLogic.move(seleccion, i, i + delta)
    }

    fun save() {
        val token = TokenManager(context).getToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(context, "Inicia sesión como administrador", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            isSaving = true
            try {
                api.setRepertorioEvento("Bearer $token", eventoId, RepertorioDto(seleccion))
                Toast.makeText(context, "Repertorio guardado (${seleccion.size} canciones)", Toast.LENGTH_SHORT).show()
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo guardar el repertorio", Toast.LENGTH_LONG).show()
            } finally {
                isSaving = false
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(24.dp))
                .background(AudioSurface)
        ) {
            // Encabezado
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("REPERTORIO DEL CONCIERTO", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(eventoNombre, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onDismiss, enabled = !isSaving) { Icon(Icons.Default.Close, "Cerrar", tint = Color.White.copy(alpha = 0.8f)) }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    isLoading -> Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        repeat(5) {
                            Row(Modifier.fillMaxWidth().height(RowHeight), verticalAlignment = Alignment.CenterVertically) {
                                SkeletonBlock(Modifier.size(42.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    SkeletonBlock(Modifier.fillMaxWidth(0.6f).height(13.dp))
                                    Spacer(Modifier.height(7.dp))
                                    SkeletonBlock(Modifier.fillMaxWidth(0.35f).height(10.dp))
                                }
                            }
                        }
                    }
                    loadFailed -> StatusMessage(
                        icon = Icons.Default.LibraryMusic,
                        title = "No se pudo cargar el catálogo",
                        text = "Revisa tu conexión e inténtalo de nuevo.",
                        action = "Reintentar",
                        onAction = { load() }
                    )
                    catalogo.isEmpty() -> StatusMessage(
                        icon = Icons.Default.LibraryMusic,
                        title = "El catálogo está vacío",
                        text = "Primero da de alta canciones en la sección Subir; después podrás asignarlas a este concierto."
                    )
                    else -> {
                        val porId = remember(catalogo) { catalogo.associateBy { it.id } }
                        val elegidas = seleccion.mapNotNull { porId[it] }
                        val disponibles = catalogo.filter { it.id !in seleccion }
                        LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                            item(key = "h-sel") {
                                SectionTitle(
                                    "EN EL CONCIERTO · ${elegidas.size}",
                                    if (elegidas.size > 1) "Arrastra el asa para cambiar el orden en que suenan." else null
                                )
                            }
                            if (elegidas.isEmpty()) {
                                item(key = "empty-sel") {
                                    Text(
                                        "Todavía no hay canciones. Agrega del catálogo con el botón +.",
                                        color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                                    )
                                }
                            }
                            items(elegidas, key = { "sel-" + it.id }) { c ->
                                val index = seleccion.indexOf(c.id)
                                val dragging = draggedId == c.id
                                SelectedRow(
                                    song = c,
                                    position = index + 1,
                                    dragging = dragging,
                                    canUp = index > 0,
                                    canDown = index < seleccion.lastIndex,
                                    onUp = { haptic(); move(c.id, -1) },
                                    onDown = { haptic(); move(c.id, +1) },
                                    onRemove = { haptic(); seleccion = seleccion - c.id },
                                    modifier = (if (dragging) Modifier else Modifier.animateItemPlacement())
                                        .zIndex(if (dragging) 1f else 0f)
                                        .graphicsLayer { translationY = if (dragging) dragOffset else 0f },
                                    handleModifier = Modifier.pointerInput(c.id) {
                                        detectDragGestures(
                                            onDragStart = { haptic(); draggedId = c.id; dragOffset = 0f },
                                            onDragEnd = { draggedId = null; dragOffset = 0f },
                                            onDragCancel = { draggedId = null; dragOffset = 0f }
                                        ) { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                            // Al pasar de la mitad de la fila vecina, intercambian lugar.
                                            val from = seleccion.indexOf(c.id)
                                            val steps = (dragOffset / rowHeightPx).let { if (it > 0.5f) 1 else if (it < -0.5f) -1 else 0 }
                                            if (steps != 0 && from + steps in seleccion.indices) {
                                                seleccion = PlaybackLogic.move(seleccion, from, from + steps)
                                                dragOffset -= steps * rowHeightPx
                                                haptic()
                                            }
                                        }
                                    }
                                )
                            }
                            item(key = "h-cat") {
                                Spacer(Modifier.height(10.dp))
                                SectionTitle("CATÁLOGO · ${disponibles.size}", if (disponibles.isEmpty()) "Todas las canciones ya están en el concierto." else null)
                            }
                            items(disponibles, key = { "cat-" + it.id }) { c ->
                                CatalogRow(c, Modifier.animateItemPlacement()) { haptic(); seleccion = seleccion + c.id }
                            }
                        }
                    }
                }
            }

            // Acciones
            val changed = seleccion != guardado
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (changed) "Cambios sin guardar" else "Sin cambios",
                    color = if (changed) PrimaryGold else TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar", color = Color.White.copy(alpha = 0.7f)) }
                Spacer(Modifier.width(6.dp))
                Button(
                    enabled = !isLoading && !loadFailed && !isSaving,
                    onClick = { haptic(); save() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    if (isSaving) CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Text("GUARDAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, hint: String?) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
        Text(title, color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        if (hint != null) Text(hint, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Canción ya elegida: asa para arrastrar, posición, portada, flechas y quitar. */
@Composable
private fun SelectedRow(
    song: CancionDto,
    position: Int,
    dragging: Boolean,
    canUp: Boolean,
    canDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier,
    handleModifier: Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .padding(vertical = 3.dp)
            .shadow(if (dragging) 12.dp else 0.dp, shape)
            .clip(shape)
            .background(if (dragging) Color(0xFF2B3A30) else PrimaryGold.copy(alpha = 0.09f))
            .border(1.dp, if (dragging) PrimaryGold else Color.Transparent, shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = handleModifier.size(width = 40.dp, height = RowHeight).semantics { contentDescription = "Arrastrar para reordenar ${song.titulo}" },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.DragHandle, null, tint = Color.White.copy(alpha = 0.7f)) }
        Text("$position", color = PrimaryGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
        CoverArt(mediaUrl(song.portadaUrl), null, Modifier.size(40.dp), radius = 6.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.titulo, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artista} · ${formatDuration(song.duracion)}", color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onUp, enabled = canUp, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, "Subir ${song.titulo}", tint = if (canUp) PrimaryGold else Color.White.copy(alpha = 0.18f))
        }
        IconButton(onClick = onDown, enabled = canDown, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, "Bajar ${song.titulo}", tint = if (canDown) PrimaryGold else Color.White.copy(alpha = 0.18f))
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Close, "Quitar ${song.titulo} del concierto", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

/** Canción del catálogo que aún no está en el concierto: se agrega al final con un toque. */
@Composable
private fun CatalogRow(song: CancionDto, modifier: Modifier, onAdd: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "Agregar al concierto", onClick = onAdd)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(mediaUrl(song.portadaUrl), null, Modifier.size(40.dp), radius = 6.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.titulo, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artista} · ${formatDuration(song.duracion)}", color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(32.dp).background(PrimaryGold.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, null, tint = PrimaryGold, modifier = Modifier.size(18.dp))
        }
    }
}

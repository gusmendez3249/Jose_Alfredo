package mx.utng.festivaltrack.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import mx.utng.festivaltrack.app.R
import mx.utng.festivaltrack.app.data.factory.InteractiveBiographyItem
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.ForumPostType
import mx.utng.festivaltrack.shared.data.remote.ComentarioDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.PublicacionDto
import java.time.Duration
import java.time.Instant

internal val ForumBg = Color(0xFF0D120E)
internal val ForumCard = Color(0xFF141C16)
internal val ForumBorder = Color(0xFF28382C)
internal val ForumField = Color(0xFF1B261E)

internal const val MAX_CONTENT = 1000
internal const val MAX_TITLE = 80
internal const val MAX_PLACE = 60
internal const val MAX_COMMENT = 400

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** Convierte la ruta `/uploads/x.jpg` del servidor en una URL absoluta; deja intactas las https. */
internal fun resolveImageUrl(url: String?): String? = when {
    url.isNullOrBlank() -> null
    url.startsWith("/") -> FestivalApiService.SERVER_URL + url
    else -> url
}

internal fun timeAgo(iso: String): String {
    val then = try { Instant.parse(iso) } catch (e: Exception) { return "" }
    val d = Duration.between(then, Instant.now())
    return when {
        d.isNegative || d.seconds < 60 -> "ahora"
        d.toMinutes() < 60 -> "hace ${d.toMinutes()} min"
        d.toHours() < 24 -> "hace ${d.toHours()} h"
        d.toDays() < 7 -> "hace ${d.toDays()} d"
        d.toDays() < 30 -> "hace ${d.toDays() / 7} sem"
        else -> "hace ${d.toDays() / 30} mes"
    }
}

internal fun typeColor(type: ForumPostType): Color = when (type) {
    ForumPostType.EXPERIENCIA -> Color(0xFFE6C27A)
    ForumPostType.RECOMENDACION -> Color(0xFF7FD1A0)
    ForumPostType.FOTO -> Color(0xFF8FB8F0)
}

private val avatarPalette = listOf(
    Color(0xFFC4A059), Color(0xFF4F8F6B), Color(0xFF5C7FB8), Color(0xFFB0605A), Color(0xFF8A6BB0), Color(0xFF3F9AA6)
)

/**
 * Comparte la publicación: texto y, si tiene foto, la imagen adjunta (se descarga a caché
 * y se entrega vía FileProvider). Si la descarga falla se comparte solo el texto.
 */
private suspend fun sharePost(context: Context, post: PublicacionDto) {
    val body = buildString {
        post.titulo?.takeIf { it.isNotBlank() }?.let { append(it).append("\n") }
        append(post.contenido)
        append("\n\n— ${post.autorNombre} · Festival José Alfredo Jiménez")
    }.trim()

    val imageUri: Uri? = resolveImageUrl(post.imagenUrl)?.let { url ->
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                val file = File(dir, "post_${post.id}.jpg")
                if (!file.exists()) {
                    URL(url).openStream().use { input -> file.outputStream().use { input.copyTo(it) } }
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                null
            }
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, body)
        if (imageUri != null) {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
        }
    }
    context.startActivity(Intent.createChooser(intent, "Compartir publicación"))
}

@Composable
internal fun Avatar(name: String, size: Int = 40) {
    val color = avatarPalette[(name.hashCode() and Int.MAX_VALUE) % avatarPalette.size]
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(color.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().take(1).uppercase().ifBlank { "?" },
            color = Color.Black.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.42f).sp
        )
    }
}

@Composable
internal fun TypeBadge(type: ForumPostType) {
    val c = typeColor(type)
    Box(
        modifier = Modifier
            .background(c.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(type.label.uppercase(), color = c, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    }
}

@Composable
internal fun forumFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = PrimaryGold,
    unfocusedBorderColor = ForumBorder,
    focusedLabelColor = PrimaryGold,
    unfocusedLabelColor = Color.Gray,
    cursorColor = PrimaryGold,
    focusedContainerColor = ForumField,
    unfocusedContainerColor = ForumField
)

// ─── Tarjeta de publicación ──────────────────────────────────────────────────

@Composable
internal fun PostCard(
    post: PublicacionDto,
    canDelete: Boolean,
    onLike: () -> Unit,
    onComments: () -> Unit,
    onDelete: () -> Unit,
    onOpenImage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val type = ForumPostType.fromApi(post.tipo)
    val imageUrl = resolveImageUrl(post.imagenUrl)
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ForumCard),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ForumBorder)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(post.autorNombre)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.autorNombre, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(timeAgo(post.createdAt), color = Color.Gray, fontSize = 11.sp)
                        if (!post.lugar.isNullOrBlank()) {
                            Text(" · ", color = Color.Gray, fontSize = 11.sp)
                            Icon(Icons.Default.Place, null, tint = Color.Gray, modifier = Modifier.size(11.dp))
                            Text(post.lugar!!, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                TypeBadge(type)
                if (canDelete) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, "Más opciones", tint = Color.Gray)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Eliminar publicación") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = { menuOpen = false; confirmDelete = true }
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.width(10.dp))
                }
            }

            if (imageUrl != null) {
                var heart by remember { mutableStateOf(false) }
                val heartScale = remember { Animatable(0f) }
                LaunchedEffect(heart) {
                    if (heart) {
                        heartScale.snapTo(0.4f)
                        heartScale.animateTo(1.2f, spring(Spring.DampingRatioMediumBouncy))
                        kotlinx.coroutines.delay(250)
                        heartScale.animateTo(0f)
                        heart = false
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 420.dp)
                        .background(ForumField)
                        .pointerInput(post.id, post.likedByMe) {
                            detectTapGestures(
                                onTap = { onOpenImage(imageUrl) },
                                onDoubleTap = {
                                    if (!post.likedByMe) onLike()
                                    heart = true
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                        contentDescription = post.titulo ?: "Foto de ${post.autorNombre}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 420.dp)
                    )
                    if (heartScale.value > 0f) {
                        Icon(
                            Icons.Default.Favorite, null,
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(84.dp).scale(heartScale.value)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!post.titulo.isNullOrBlank()) {
                    Text(post.titulo!!, color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                }
                if (post.contenido.isNotBlank()) {
                    val long = post.contenido.length > 220
                    Text(
                        post.contenido,
                        color = Color(0xFFE4E4E4),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = if (expanded || !long) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (long) {
                        Text(
                            if (expanded) "Ver menos" else "Ver más",
                            color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp).clickable { expanded = !expanded }
                        )
                    }
                }
            }

            HorizontalDivider(color = ForumBorder)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onLike) {
                    Icon(
                        if (post.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (post.likedByMe) "Quitar me gusta" else "Me gusta",
                        tint = if (post.likedByMe) Color(0xFFE5615B) else Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("${post.likes}", color = Color.LightGray, fontSize = 13.sp)
                }
                TextButton(onClick = onComments) {
                    Icon(Icons.Default.ChatBubbleOutline, "Comentarios", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${post.comentarios}", color = Color.LightGray, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { scope.launch { sharePost(context, post) } }) {
                    Icon(Icons.Default.Share, "Compartir", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Color(0xFF161F18),
            title = { Text("¿Eliminar publicación?", color = Color.White) },
            text = { Text("Esta acción no se puede deshacer.", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Eliminar", color = Color(0xFFE5615B)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = Color.Gray) } }
        )
    }
}

// ─── Visor de imagen con zoom ────────────────────────────────────────────────

@Composable
internal fun ImageViewerDialog(url: String, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = url,
                contentDescription = "Foto ampliada",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale == 1f) { offsetX = 0f; offsetY = 0f } else { offsetX += pan.x; offsetY += pan.y }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            if (scale > 1f) { scale = 1f; offsetX = 0f; offsetY = 0f } else scale = 2.5f
                        })
                    }
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            ) { Icon(Icons.Default.Close, "Cerrar", tint = Color.White) }
        }
    }
}

// ─── Comentarios ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommentsSheet(
    comments: List<ComentarioDto>,
    isLoading: Boolean,
    isLoggedIn: Boolean,
    currentUserId: String?,
    isAdmin: Boolean,
    onSend: (String, () -> Unit) -> Unit,
    onDelete: (ComentarioDto) -> Unit,
    onLoginRequired: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color(0xFF111812)) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).imePadding()) {
            Text(
                "Comentarios", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    isLoading -> CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.align(Alignment.Center))
                    comments.isEmpty() -> Text(
                        "Aún no hay comentarios.\n¡Sé el primero en responder!",
                        color = Color.Gray, fontSize = 13.sp, lineHeight = 19.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                    else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(comments, key = { it.id }) { c ->
                            Row(verticalAlignment = Alignment.Top) {
                                Avatar(c.autorNombre, size = 32)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(c.autorNombre, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(timeAgo(c.createdAt), color = Color.Gray, fontSize = 11.sp)
                                    }
                                    Text(c.texto, color = Color(0xFFDDDDDD), fontSize = 14.sp, lineHeight = 19.sp)
                                }
                                if (isAdmin || c.autorId == currentUserId) {
                                    IconButton(onClick = { onDelete(c) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, "Eliminar comentario", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = ForumBorder)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= MAX_COMMENT) text = it },
                    placeholder = { Text(if (isLoggedIn) "Escribe un comentario…" else "Inicia sesión para comentar", color = Color.Gray) },
                    enabled = isLoggedIn,
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = forumFieldColors(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (!isLoggedIn) onLoginRequired()
                        else onSend(text) { text = "" }
                    },
                    enabled = !isLoggedIn || text.isNotBlank(),
                    modifier = Modifier.size(48.dp).background(if (text.isNotBlank() || !isLoggedIn) PrimaryGold else ForumBorder, CircleShape)
                ) { Icon(Icons.Default.Send, "Enviar comentario", tint = Color.Black) }
            }
        }
    }
}

// ─── Compositor de publicaciones ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ComposerSheet(
    isPublishing: Boolean,
    onPublish: (ForumPostType, String, String, String, Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(ForumPostType.EXPERIENCIA) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showErrors by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            imageUri = uri
            if (type == ForumPostType.EXPERIENCIA && content.isBlank()) type = ForumPostType.FOTO
        }
    }

    val needsPhoto = type == ForumPostType.FOTO && imageUri == null
    val empty = content.isBlank() && imageUri == null
    val hints = mapOf(
        ForumPostType.EXPERIENCIA to "Cuéntanos cómo viviste el festival…",
        ForumPostType.RECOMENDACION to "¿Qué recomiendas? Un lugar, platillo, ruta, horario…",
        ForumPostType.FOTO to "Pon un pie de foto (opcional)"
    )

    ModalBottomSheet(
        onDismissRequest = { if (!isPublishing) onDismiss() },
        sheetState = sheetState,
        containerColor = Color(0xFF111812)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text("Compartir con la comunidad", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Tu experiencia puede ayudar a otros asistentes", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForumPostType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ForumField, labelColor = Color.LightGray,
                            selectedContainerColor = typeColor(t).copy(alpha = 0.22f), selectedLabelColor = typeColor(t)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = type == t,
                            borderColor = ForumBorder, selectedBorderColor = typeColor(t).copy(alpha = 0.7f)
                        )
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            val shape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (imageUri != null) 200.dp else 110.dp)
                    .clip(shape)
                    .background(ForumField)
                    .border(1.dp, if (showErrors && needsPhoto) Color(0xFFE5615B) else PrimaryGold.copy(alpha = 0.5f), shape)
                    .clickable(enabled = !isPublishing) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri, contentDescription = "Foto seleccionada",
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape).size(32.dp)
                    ) { Icon(Icons.Default.Close, "Quitar foto", tint = Color.White, modifier = Modifier.size(18.dp)) }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, null, tint = PrimaryGold, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (type == ForumPostType.FOTO) "Agrega tu foto (obligatoria)" else "Agregar una foto (opcional)",
                            color = Color.LightGray, fontSize = 13.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= MAX_TITLE) title = it },
                label = { Text("Título (opcional)") },
                singleLine = true,
                colors = forumFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= MAX_CONTENT) content = it },
                label = { Text(if (type == ForumPostType.FOTO) "Pie de foto" else "Tu publicación") },
                placeholder = { Text(hints.getValue(type), color = Color.Gray) },
                minLines = 4,
                maxLines = 8,
                isError = showErrors && empty,
                supportingText = {
                    Text(
                        if (showErrors && empty) "Escribe algo o agrega una foto" else "${content.length}/$MAX_CONTENT",
                        color = if (showErrors && empty) Color(0xFFE5615B) else Color.Gray
                    )
                },
                colors = forumFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = place,
                onValueChange = { if (it.length <= MAX_PLACE) place = it },
                label = { Text("Lugar (opcional)") },
                placeholder = { Text("Ej: Mausoleo, Dolores Hidalgo", color = Color.Gray) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Place, null, tint = Color.Gray) },
                colors = forumFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    if (empty || needsPhoto) showErrors = true
                    else onPublish(type, title, content, place, imageUri)
                },
                enabled = !isPublishing,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Publicando…", fontWeight = FontWeight.Bold)
                } else {
                    Text("PUBLICAR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
            TextButton(
                onClick = { scope.launch { sheetState.hide(); onDismiss() } },
                enabled = !isPublishing,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("Cancelar", color = Color.Gray) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Archivo histórico (contenido oficial) ───────────────────────────────────

/** Modelo para Coil: URL remota/local real, o el drawable empacado en la app. */
internal fun archiveImageModel(item: InteractiveBiographyItem): Any {
    val url = item.storageImageUrl.orEmpty()
    val isRealUrl = url.startsWith("http") || url.startsWith("content://") ||
        (url.startsWith("file://") && !url.startsWith("file:///android_asset"))
    if (isRealUrl) return url
    return when {
        "gal_el_rey_escenario" in url -> R.drawable.gal_el_rey_escenario
        "gal_caminos_guanajuato" in url -> R.drawable.gal_caminos_guanajuato
        "gal_plaza_dolores" in url -> R.drawable.gal_plaza_dolores
        "gal_casa_museo" in url -> R.drawable.gal_casa_museo
        "gal_mausoleo_sarape" in url -> R.drawable.gal_mausoleo_sarape
        "gal_estudio_xew" in url -> R.drawable.gal_estudio_xew
        "gal_banner_festival" in url -> R.drawable.gal_banner_festival
        "gal_servilleta_cantina" in url -> R.drawable.gal_servilleta_cantina
        "jose_alfredo_portrait" in url -> R.drawable.jose_alfredo_portrait
        "hero_dolores_hidalgo" in url -> R.drawable.hero_dolores_hidalgo
        "mariachi_gala_stage" in url -> R.drawable.mariachi_gala_stage
        else -> R.drawable.ranchera_guitar
    }
}

@Composable
internal fun ArchiveCarousel(items: List<InteractiveBiographyItem>, onOpen: (InteractiveBiographyItem) -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ARCHIVO HISTÓRICO", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.weight(1f))
            Text("${items.size} fotos", color = Color.Gray, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                Box(
                    modifier = Modifier
                        .width(230.dp)
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ForumField)
                        .border(1.dp, ForumBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpen(item) }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(archiveImageModel(item)).crossfade(true).build(),
                        contentDescription = item.titulo,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))
                        )
                    )
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                        Text(item.anoOEepoca, color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(item.titulo, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ArchiveDetailDialog(item: InteractiveBiographyItem, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                AsyncImage(
                    model = archiveImageModel(item),
                    contentDescription = item.titulo,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 460.dp)
                )
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(item.anoOEepoca.uppercase(), color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(item.titulo, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (!item.ubicacionHistorica.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.Place, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Text(item.ubicacionHistorica!!, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(item.descripcion, color = Color(0xFFE4E4E4), fontSize = 14.sp, lineHeight = 21.sp)
                    if (!item.citaCelebre.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            item.citaCelebre!!, color = PrimaryGold, fontSize = 14.sp, lineHeight = 21.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            ) { Icon(Icons.Default.Close, "Cerrar", tint = Color.White) }
        }
    }
}

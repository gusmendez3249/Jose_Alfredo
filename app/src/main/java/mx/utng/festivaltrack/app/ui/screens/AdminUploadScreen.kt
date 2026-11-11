package mx.utng.festivaltrack.app.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.utils.ProgressFileBody
import mx.utng.festivaltrack.app.ui.utils.SongUpload
import mx.utng.festivaltrack.shared.data.remote.CancionCreateDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import java.io.File
import java.io.IOException

/** Archivo elegido en el formulario, con lo necesario para mostrarlo y validarlo antes de subir. */
private data class PickedFile(val uri: Uri, val name: String, val bytes: Long?, val durationSec: Int? = null)

/**
 * Alta de una canción en el catálogo (administrador): portada con vista previa, título, intérprete y
 * audio. Muestra cómo se verá la canción en la app y el avance real de la subida.
 */
@Composable
fun AdminUploadScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { FestivalApiService.create() }
    val haptic = rememberHaptic()

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var cover by remember { mutableStateOf<PickedFile?>(null) }
    var audio by remember { mutableStateOf<PickedFile?>(null) }
    var stage by remember { mutableStateOf<SongUpload.Stage?>(null) }
    var stageFraction by remember { mutableFloatStateOf(0f) }
    var formError by remember { mutableStateOf<String?>(null) }
    var published by remember { mutableStateOf<String?>(null) }
    val isUploading = stage != null

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { cover = describeFile(context, uri); formError = null; published = null }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            audio = describeFile(context, uri).copy(durationSec = audioDurationSeconds(context, uri))
            formError = null; published = null
        }
    }

    fun publish() {
        val error = SongUpload.validate(title, audio?.bytes, cover?.bytes)
        if (error != null) { formError = error; return }
        val token = TokenManager(context).getToken()
        if (token.isNullOrBlank()) { formError = "Inicia sesión como administrador para publicar"; return }
        formError = null
        published = null
        scope.launch {
            try {
                val bearer = "Bearer $token"
                // 1. Audio (si se eligió) · 2. Portada (si se eligió) · 3. Canción en el catálogo
                stage = SongUpload.Stage.AUDIO; stageFraction = 0f
                val audioUrl = audio?.let { uploadSongFile(context, api, bearer, it.uri, "audio/mpeg") { f -> stageFraction = f } }
                stage = SongUpload.Stage.COVER; stageFraction = 0f
                val coverUrl = cover?.let { uploadSongFile(context, api, bearer, it.uri, "image/jpeg") { f -> stageFraction = f } }
                stage = SongUpload.Stage.SAVING; stageFraction = 0.5f
                val defaults = CancionCreateDto(titulo = "")
                api.createCancion(
                    bearer,
                    CancionCreateDto(
                        titulo = title.trim(),
                        artista = artist.trim().ifBlank { defaults.artista },
                        duracion = audio?.durationSec ?: defaults.duracion,
                        archivoUrl = audioUrl ?: defaults.archivoUrl,
                        portadaUrl = coverUrl
                    )
                )
                published = title.trim()
                title = ""; artist = ""; cover = null; audio = null
            } catch (e: Exception) {
                formError = "No se pudo publicar: ${e.localizedMessage ?: "revisa tu conexión"}"
            } finally {
                stage = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AudioBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("CATÁLOGO DE CANCIONES", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Text("Subir nueva canción", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Publícala en el catálogo y después asígnala a un concierto en Gestionar → Eventos → Repertorio.",
            color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(20.dp))

        // Portada con vista previa
        Row(verticalAlignment = Alignment.CenterVertically) {
            val shape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(shape)
                    .background(AudioTile)
                    .border(1.dp, if (cover != null) PrimaryGold else AudioCardBorder, shape)
                    .clickable(enabled = !isUploading, onClickLabel = "Elegir la portada", role = Role.Button) { coverPicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val picked = cover
                if (picked != null) {
                    AsyncImage(model = picked.uri, contentDescription = "Vista previa de la portada", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(6.dp).size(30.dp).background(Color.Black.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, null, tint = PrimaryGold, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Agregar portada", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Portada", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    cover?.let { listOfNotNull(it.name, it.bytes?.let(SongUpload::formatSize)).joinToString(" · ") }
                        ?: "Opcional. JPG, PNG o WEBP, de preferencia cuadrada.",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 3, overflow = TextOverflow.Ellipsis
                )
                if (cover != null) {
                    TextButton(onClick = { cover = null }, enabled = !isUploading, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Quitar portada", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        FormField("Título de la canción", title, { title = it; formError = null }, "Ej. El Rey", enabled = !isUploading)
        Spacer(Modifier.height(14.dp))
        FormField("Artista o intérprete", artist, { artist = it }, "José Alfredo Jiménez", enabled = !isUploading, last = true)
        Spacer(Modifier.height(18.dp))

        // Audio
        val pickedAudio = audio
        val audioShape = RoundedCornerShape(14.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(audioShape)
                .background(AudioCard)
                .border(1.dp, if (pickedAudio != null) PrimaryGold.copy(alpha = 0.7f) else AudioCardBorder, audioShape)
                .clickable(enabled = !isUploading, onClickLabel = "Elegir el archivo de audio", role = Role.Button) { audioPicker.launch("audio/*") }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(46.dp).background(PrimaryGold.copy(alpha = 0.16f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(if (pickedAudio != null) Icons.Default.CheckCircle else Icons.Default.AudioFile, null, tint = PrimaryGold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(pickedAudio?.name ?: "Archivo de audio", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pickedAudio?.let {
                        listOfNotNull(it.durationSec?.let(::formatDuration), it.bytes?.let(SongUpload::formatSize)).joinToString(" · ")
                            .ifBlank { "Listo para subir" }
                    } ?: "MP3, M4A, WAV u OGG · máximo 30 MB",
                    color = TextMuted, fontSize = 12.sp
                )
            }
            Text(if (pickedAudio != null) "CAMBIAR" else "ELEGIR", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        if (pickedAudio == null) {
            Text(
                "Sin audio propio se usará una pista de muestra.",
                color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }

        // Vista previa de cómo se verá en la lista del concierto
        Spacer(Modifier.height(20.dp))
        Text("ASÍ SE VERÁ EN LA APP", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverArt(cover?.uri?.toString(), null, Modifier.size(52.dp), radius = 8.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title.ifBlank { "Título de la canción" }, color = if (title.isBlank()) TextMuted else Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist.ifBlank { "José Alfredo Jiménez" }, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(formatDuration(pickedAudio?.durationSec ?: 180), color = TextMuted, fontSize = 13.sp)
        }

        // Avance de la subida
        AnimatedVisibility(visible = isUploading) {
            val current = stage ?: SongUpload.Stage.SAVING
            val overall by animateFloatAsState(SongUpload.overallProgress(current, stageFraction), label = "upload")
            Column(Modifier.padding(top = 20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${current.label}…", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${(overall * 100).toInt()} %", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = overall,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = PrimaryGold,
                    trackColor = AudioTile
                )
            }
        }

        formError?.let { Notice(it, isError = true) }
        published?.let { Notice("«$it» ya está en el catálogo. Asígnala a un concierto en Gestionar → Eventos.", isError = false) }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { haptic(); publish() },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGold, contentColor = Color.Black,
                disabledContainerColor = PrimaryGold.copy(alpha = 0.45f), disabledContentColor = Color.Black
            ),
            shape = RoundedCornerShape(50)
        ) {
            if (isUploading) {
                CircularProgressIndicator(color = Color.Black, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("PUBLICANDO…", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            } else {
                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("PUBLICAR EN EL CATÁLOGO", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    // Aviso breve además del mensaje en pantalla, por si el formulario quedó fuera de vista.
    LaunchedEffect(published) {
        if (published != null) Toast.makeText(context, "¡Canción publicada!", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun FormField(label: String, value: String, onChange: (String) -> Unit, placeholder: String, enabled: Boolean, last: Boolean = false) {
    Text(label.uppercase(), color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.35f)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = if (last) ImeAction.Done else ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AudioCard,
            unfocusedContainerColor = AudioCard,
            disabledContainerColor = AudioCard,
            focusedBorderColor = PrimaryGold,
            unfocusedBorderColor = AudioCardBorder,
            disabledBorderColor = AudioCardBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color.White.copy(alpha = 0.6f),
            cursorColor = PrimaryGold
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun Notice(text: String, isError: Boolean) {
    val color = if (isError) Color(0xFFFF8A80) else Color(0xFF8EDB9B)
    Row(
        Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (isError) Icons.Default.Close else Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = color, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

/** Nombre y tamaño del archivo elegido. */
private fun describeFile(context: Context, uri: Uri): PickedFile {
    var name = uri.lastPathSegment ?: "archivo"
    var bytes: Long? = null
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { i -> c.getString(i)?.let { name = it } }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !c.isNull(it) }?.let { i -> bytes = c.getLong(i) }
            }
        }
    }
    return PickedFile(uri, name, bytes)
}

/** Sube un archivo (audio o portada) al servidor, avisando el avance, y devuelve su ruta `/uploads/...`. */
private suspend fun uploadSongFile(
    context: Context,
    api: FestivalApiService,
    bearer: String,
    uri: Uri,
    fallbackMime: String,
    onProgress: (Float) -> Unit
): String = withContext(Dispatchers.IO) {
    val mime = context.contentResolver.getType(uri) ?: fallbackMime
    val tmp = File.createTempFile("cancion_", ".tmp", context.cacheDir)
    try {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("No se pudo leer el archivo")
        input.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
        val body = ProgressFileBody(tmp, mime.toMediaType(), onProgress)
        val part = MultipartBody.Part.createFormData("file", tmp.name, body)
        api.subirArchivoCancion(bearer, part).url
    } finally {
        tmp.delete()
    }
}

/** Duración del audio en segundos (null si no se puede leer). */
private fun audioDurationSeconds(context: Context, uri: Uri): Int? {
    val retriever = android.media.MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()?.let { (it / 1000).toInt().coerceAtLeast(1) }
    } catch (e: Exception) {
        null
    } finally {
        retriever.release()
    }
}

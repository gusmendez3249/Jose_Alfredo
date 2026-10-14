package mx.utng.festivaltrack.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.RepertorioDto

/**
 * Diálogo del administrador para definir el repertorio de un concierto: se eligen
 * canciones del catálogo (una por una, marcándolas) y se ordenan con las flechas.
 * Lo guardado es lo que los asistentes escuchan como demos en el detalle del evento.
 */
@Composable
fun RepertoireDialog(
    eventoId: String,
    eventoNombre: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { FestivalApiService.create() }

    var catalogo by remember { mutableStateOf<List<CancionDto>>(emptyList()) }
    /** Ids elegidos, en el orden de reproducción. */
    var seleccion by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            isLoading = true
            loadFailed = false
            try {
                catalogo = api.getCanciones()
                seleccion = api.getCancionesEvento(eventoId).map { it.id }
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
        val j = i + delta
        if (i < 0 || j !in seleccion.indices) return
        seleccion = seleccion.toMutableList().apply { add(j, removeAt(i)) }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = Color(0xFF161F18),
        title = {
            Column {
                Text("REPERTORIO DEL CONCIERTO", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(eventoNombre, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            when {
                isLoading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGold)
                }
                loadFailed -> Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No se pudo cargar el catálogo.", color = Color.LightGray)
                    TextButton(onClick = { load() }) { Text("Reintentar", color = PrimaryGold) }
                }
                catalogo.isEmpty() -> Text(
                    "El catálogo está vacío. Primero da de alta canciones en la sección de carga.",
                    color = Color.LightGray
                )
                else -> Column {
                    Text(
                        "${seleccion.size} canciones elegidas · marca las que sonarán y ordénalas con las flechas",
                        color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // Primero las elegidas (en su orden), luego el resto del catálogo.
                    val porId = catalogo.associateBy { it.id }
                    val elegidas = seleccion.mapNotNull { porId[it] }
                    val disponibles = catalogo.filter { it.id !in seleccion }
                    LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                        items(elegidas, key = { "sel-" + it.id }) { c ->
                            SongPickRow(
                                song = c,
                                checked = true,
                                orderLabel = "${seleccion.indexOf(c.id) + 1}.",
                                onToggle = { seleccion = seleccion - c.id },
                                onUp = { move(c.id, -1) },
                                onDown = { move(c.id, +1) },
                                canUp = seleccion.indexOf(c.id) > 0,
                                canDown = seleccion.indexOf(c.id) < seleccion.lastIndex
                            )
                        }
                        items(disponibles, key = { "cat-" + it.id }) { c ->
                            SongPickRow(song = c, checked = false, onToggle = { seleccion = seleccion + c.id })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isLoading && !loadFailed && !isSaving,
                onClick = {
                    val token = TokenManager(context).getToken()
                    if (token.isNullOrBlank()) {
                        Toast.makeText(context, "Inicia sesión como administrador", Toast.LENGTH_SHORT).show()
                        return@Button
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
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Text("GUARDAR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar", color = Color.Gray) }
        }
    )
}

@Composable
private fun SongPickRow(
    song: CancionDto,
    checked: Boolean,
    onToggle: () -> Unit,
    orderLabel: String? = null,
    onUp: () -> Unit = {},
    onDown: () -> Unit = {},
    canUp: Boolean = false,
    canDown: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (checked) PrimaryGold.copy(alpha = 0.10f) else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = PrimaryGold, checkmarkColor = Color.Black, uncheckedColor = Color.Gray)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                (orderLabel?.let { "$it " } ?: "") + song.titulo,
                color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text("${song.artista} · ${formatDuration(song.duracion)}", color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (checked) {
            IconButton(onClick = onUp, enabled = canUp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, "Subir", tint = if (canUp) PrimaryGold else Color(0xFF3A3A3A))
            }
            IconButton(onClick = onDown, enabled = canDown, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, "Bajar", tint = if (canDown) PrimaryGold else Color(0xFF3A3A3A))
            }
        }
    }
}

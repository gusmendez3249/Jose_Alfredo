package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.PublicacionDto
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.presentation.components.TvAviso
import mx.utng.festivaltrack.tv.presentation.viewmodel.Cargable
import mx.utng.festivaltrack.tv.ui.theme.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIPOS = listOf(null to "Todo", "EXPERIENCIA" to "Experiencias", "RECOMENDACION" to "Recomendaciones", "FOTO" to "Fotos")
private val TIPO_SINGULAR = mapOf("EXPERIENCIA" to "Experiencia", "RECOMENDACION" to "Recomendación", "FOTO" to "Foto")
private val FECHA = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("es", "MX")).withZone(HorarioFestival.ZONA)

private fun urlMedia(ruta: String): String = if (ruta.startsWith("/")) FestivalApiService.SERVER_URL + ruta else ruta

/**
 * Comunidad del festival (la "Galería" de publicaciones del móvil): experiencias,
 * recomendaciones y fotos de los asistentes. En la TV se leen; para publicar o comentar se usa
 * la app del celular.
 */
@Composable
fun TvComunidadScreen(comunidad: Cargable<List<PublicacionDto>>, onRecargar: () -> Unit) {
    var tipo by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(FestivalDarkBg).padding(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Comunidad del festival", color = FestivalGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Fotos, recomendaciones y anécdotas de los asistentes · publica desde la app en tu celular", color = FestivalTextSecondary, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onRecargar, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Actualizar", color = Color.White)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TIPOS.forEach { (valor, nombre) ->
                FilterChip(
                    selected = tipo == valor,
                    onClick = { tipo = valor },
                    label = { Text(nombre) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FestivalGold, selectedLabelColor = Color.Black,
                        containerColor = FestivalCardBg, labelColor = Color.White
                    )
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        when (comunidad) {
            Cargable.Cargando -> TvAviso(Icons.Default.Forum, "Cargando publicaciones…", "", cargando = true)
            Cargable.SinConexion -> TvAviso(Icons.Default.CloudOff, "Sin conexión con el servidor", "No se pudieron cargar las publicaciones.", "Intentar de nuevo", onRecargar)
            is Cargable.Listo -> {
                val visibles = comunidad.datos.filter { tipo == null || it.tipo.equals(tipo, ignoreCase = true) }
                if (visibles.isEmpty()) {
                    TvAviso(Icons.Default.Forum, "Todavía no hay publicaciones", "Sé el primero en compartir tu experiencia desde la app del celular.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(visibles, key = { it.id }) { TarjetaPublicacion(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaPublicacion(p: PublicacionDto) {
    var enfocada by remember { mutableStateOf(false) }
    Surface(
        onClick = {},
        color = if (enfocada) Color(0xFF2E3D30) else FestivalCardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { enfocada = it.isFocused }
            .border(if (enfocada) 2.dp else 0.dp, if (enfocada) FestivalGold else Color.Transparent, RoundedCornerShape(12.dp))
    ) {
        Row(Modifier.padding(14.dp)) {
            p.imagenUrl?.takeIf { it.isNotBlank() }?.let { url ->
                AsyncImage(
                    model = urlMedia(url), contentDescription = p.titulo ?: "Foto", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(110.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = FestivalSidebarBg, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            TIPO_SINGULAR[p.tipo.uppercase()] ?: p.tipo,
                            color = FestivalGold, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(p.autorNombre, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                p.titulo?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(p.contenido, color = Color.LightGray, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = FestivalBadgeLive, modifier = Modifier.size(14.dp))
                    Text(" ${p.likes}", color = FestivalTextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.ChatBubble, contentDescription = null, tint = FestivalTextSecondary, modifier = Modifier.size(14.dp))
                    Text(" ${p.comentarios}", color = FestivalTextSecondary, fontSize = 12.sp)
                    p.lugar?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Default.Place, contentDescription = null, tint = FestivalTextSecondary, modifier = Modifier.size(14.dp))
                        Text(" $it", color = FestivalTextSecondary, fontSize = 12.sp, maxLines = 1)
                    }
                    Spacer(Modifier.weight(1f))
                    runCatching { FECHA.format(Instant.parse(p.createdAt)) }.getOrNull()?.let {
                        Text(it, color = FestivalTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

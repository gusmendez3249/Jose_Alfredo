package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import mx.utng.festivaltrack.shared.data.remote.BiografiaDto
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.tv.R
import mx.utng.festivaltrack.tv.presentation.components.TvAviso
import mx.utng.festivaltrack.tv.presentation.components.TvFilaCancion
import mx.utng.festivaltrack.tv.presentation.viewmodel.Cargable
import mx.utng.festivaltrack.tv.presentation.viewmodel.EstadoMusica
import mx.utng.festivaltrack.tv.ui.theme.*

/** Nombre del origen de la cola cuando suena el catálogo del artista (igual que en el móvil). */
const val ORIGEN_DISCOGRAFIA = "Discografía de José Alfredo"

/** Lee una lista guardada como JSON (`["El Rey","Caminos de Guanajuato"]`); vacía si no se puede. */
private fun listaJson(json: String?): List<String> =
    runCatching { Gson().fromJson(json, Array<String>::class.java)?.toList() }.getOrNull().orEmpty().filter { it.isNotBlank() }

/**
 * Biografía y música de José Alfredo, como en el móvil:
 * - Vida y obra, cita célebre e hitos que vienen del servidor.
 * - El botón de la cita reproduce «El Rey»; la discografía reproduce la canción del catálogo
 *   que corresponde a cada título.
 * - El catálogo completo de canciones para escuchar (demos de 30 s).
 */
@Composable
fun TvBiografiaScreen(
    biografia: Cargable<BiografiaDto?>,
    canciones: Cargable<List<CancionDto>>,
    musica: EstadoMusica,
    onReproducir: (List<CancionDto>, Int) -> Unit,
    onReintentar: () -> Unit
) {
    val bio = (biografia as? Cargable.Listo)?.datos
    val catalogo = (canciones as? Cargable.Listo)?.datos.orEmpty()
    val sonandoCatalogo = musica.origen == ORIGEN_DISCOGRAFIA && musica.sonando

    fun reproducirTitulo(titulo: String?) {
        if (catalogo.isEmpty()) return
        val i = titulo?.let { t -> catalogo.indexOfFirst { it.titulo.contains(t, ignoreCase = true) || t.contains(it.titulo, ignoreCase = true) } }
            ?.takeIf { it >= 0 } ?: 0
        onReproducir(catalogo, i)
    }

    if (biografia is Cargable.SinConexion && canciones is Cargable.SinConexion) {
        TvAviso(Icons.Default.CloudOff, "Sin conexión con el servidor", "No se pudo cargar la biografía.", "Intentar de nuevo", onReintentar)
        return
    }

    Row(
        modifier = Modifier.fillMaxSize().background(FestivalDarkBg).padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Vida y obra
        LazyColumn(modifier = Modifier.weight(0.55f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.jose_alfredo_portrait),
                        contentDescription = "José Alfredo Jiménez",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("PATRIMONIO MUSICAL DE MÉXICO", color = FestivalGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(bio?.artista?.nombre ?: "José Alfredo Jiménez", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Dolores Hidalgo, Gto. · 1926–1973", color = FestivalTextSecondary, fontSize = 12.sp)
                    }
                }
            }
            if (biografia is Cargable.Cargando) item { LinearProgressIndicator(color = FestivalGold, modifier = Modifier.fillMaxWidth()) }

            item {
                // Cita célebre: igual que en el móvil, al pulsarla suena «El Rey».
                val sonandoRey = sonandoCatalogo && musica.actual?.titulo?.contains("El Rey", ignoreCase = true) == true
                var enfocada by remember { mutableStateOf(false) }
                Surface(
                    onClick = { reproducirTitulo("El Rey") },
                    enabled = catalogo.isNotEmpty(),
                    color = Color(0xFF2C2214),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { enfocada = it.isFocused }
                        .border(if (enfocada) 2.dp else 0.dp, if (enfocada) FestivalGold else Color.Transparent, RoundedCornerShape(14.dp))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (sonandoRey) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = null, tint = FestivalGold, modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("CITA CÉLEBRE DEL MAESTRO", color = FestivalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                bio?.citaCelebre ?: "\"No tengo trono ni reina, ni nadie que me comprenda, pero sigo siendo el Rey.\"",
                                color = Color.White, fontSize = 15.sp, fontStyle = FontStyle.Italic
                            )
                            Text(if (sonandoRey) "Sonando: «El Rey»" else "Pulsa OK para escuchar «El Rey»", color = FestivalTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            bio?.descripcion?.takeIf { it.isNotBlank() }?.let { texto ->
                item {
                    Text("Vida y obra", color = FestivalGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(texto, color = Color.White, fontSize = 14.sp, lineHeight = 21.sp)
                }
            }

            val hitos = listaJson(bio?.hitos)
            if (hitos.isNotEmpty()) {
                item { Text("Momentos clave", color = FestivalGold, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                itemsIndexed(hitos) { _, hito ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(hito, color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            val discos = listaJson(bio?.discografia)
            if (discos.isNotEmpty()) {
                item { Text("Discografía", color = FestivalGold, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        discos.forEach { disco ->
                            val sonando = sonandoCatalogo && musica.actual?.titulo?.contains(disco, ignoreCase = true) == true
                            var enfocado by remember { mutableStateOf(false) }
                            Surface(
                                onClick = { reproducirTitulo(disco) },
                                enabled = catalogo.isNotEmpty(),
                                color = if (enfocado) Color(0xFF2E3D30) else FestivalCardBg,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .onFocusChanged { enfocado = it.isFocused }
                                    .border(if (enfocado) 2.dp else 0.dp, if (enfocado) FestivalGold else Color.Transparent, RoundedCornerShape(10.dp))
                            ) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (sonando) Icons.Default.GraphicEq else Icons.Default.Album, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(disco, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Canciones del catálogo
        Column(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
            Text("Escucha sus canciones", color = FestivalGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Fragmentos de 30 s · pulsa OK para escuchar", color = FestivalTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            when (canciones) {
                Cargable.Cargando -> CircularProgressIndicator(color = FestivalGold)
                Cargable.SinConexion -> Text("No se pudieron cargar las canciones.", color = FestivalTextSecondary)
                is Cargable.Listo -> if (catalogo.isEmpty()) {
                    Text("Todavía no hay canciones en el catálogo.", color = FestivalTextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(catalogo, key = { _, c -> c.id }) { i, cancion ->
                            TvFilaCancion(
                                numero = i + 1,
                                cancion = cancion,
                                sonando = sonandoCatalogo && musica.actual?.id == cancion.id,
                                onClick = { onReproducir(catalogo, i) }
                            )
                        }
                    }
                }
            }
        }
    }
}

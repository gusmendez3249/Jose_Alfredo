package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.tv.presentation.viewmodel.EstadoGaleria
import mx.utng.festivaltrack.tv.ui.theme.*

/** Foto de la galería lista para mostrarse. */
private data class FotoTv(val id: String, val url: String, val titulo: String, val galeria: String)

/** Convierte una ruta del servidor (`/uploads/x.jpg`) en URL completa; deja intactas las URL completas. */
private fun urlMedia(ruta: String): String = if (ruta.startsWith("/")) FestivalApiService.SERVER_URL + ruta else ruta

/**
 * Galería con las fotos reales del servidor (las que administra el panel), filtrables por
 * galería. Pulsar OK sobre una la abre en grande.
 */
@Composable
fun TvGalleryScreen(estado: EstadoGaleria, onReintentar: () -> Unit) {
    var filtro by remember { mutableStateOf<String?>(null) }
    var abierta by remember { mutableStateOf<FotoTv?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
            .padding(32.dp)
    ) {
        when (estado) {
            EstadoGaleria.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FestivalGold)
            }
            EstadoGaleria.SinConexion -> AvisoGaleria(
                icono = Icons.Default.CloudOff,
                titulo = "Sin conexión con el servidor",
                texto = "No se pudieron cargar las fotos.",
                boton = "Intentar de nuevo",
                onBoton = onReintentar
            )
            is EstadoGaleria.Lista -> {
                val fotos = estado.galerias.flatMap { g ->
                    g.imagenes.sortedBy { it.orden }.map { FotoTv(it.id, urlMedia(it.url), it.titulo ?: g.nombre, g.nombre) }
                }
                val galerias = estado.galerias.filter { it.imagenes.isNotEmpty() }.map { it.nombre }.distinct()
                val visibles = if (filtro == null) fotos else fotos.filter { it.galeria == filtro }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Galería del festival", color = FestivalGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("${fotos.size} fotos", color = FestivalTextSecondary, fontSize = 12.sp)
                    }
                    if (galerias.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (listOf<String?>(null) + galerias).forEach { nombre ->
                                FilterChip(
                                    selected = filtro == nombre,
                                    onClick = { filtro = nombre },
                                    label = { Text(nombre ?: "Todo") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FestivalGold,
                                        selectedLabelColor = Color.Black,
                                        containerColor = FestivalCardBg,
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                if (fotos.isEmpty()) {
                    AvisoGaleria(
                        icono = Icons.Default.PhotoLibrary,
                        titulo = "Todavía no hay fotos",
                        texto = "Cuando el equipo del festival suba fotos aparecerán aquí."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(visibles, key = { it.id }) { foto -> FotoCard(foto) { abierta = foto } }
                    }
                }
            }
        }
    }

    abierta?.let { foto ->
        Dialog(onDismissRequest = { abierta = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                onClick = { abierta = null },
                color = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.85f)
            ) {
                Box {
                    AsyncImage(
                        model = foto.url,
                        contentDescription = foto.titulo,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                            .padding(20.dp)
                    ) {
                        Text(foto.titulo, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("${foto.galeria} · Pulsa OK o Atrás para cerrar", color = FestivalGold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FotoCard(foto: FotoTv, onClick: () -> Unit) {
    var enfocada by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        color = FestivalCardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .onFocusChanged { enfocada = it.isFocused }
            .border(if (enfocada) 3.dp else 0.dp, if (enfocada) FestivalGold else Color.Transparent, RoundedCornerShape(12.dp))
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = foto.url,
                contentDescription = foto.titulo,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(foto.titulo, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(foto.galeria, color = FestivalGold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AvisoGaleria(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    texto: String,
    boton: String? = null,
    onBoton: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icono, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(titulo, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(texto, color = FestivalTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (boton != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBoton, colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)) {
                Text(boton, fontWeight = FontWeight.Bold)
            }
        }
    }
}

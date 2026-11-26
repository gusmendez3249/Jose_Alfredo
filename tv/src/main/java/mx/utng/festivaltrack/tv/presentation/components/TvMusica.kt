package mx.utng.festivaltrack.tv.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.tv.presentation.viewmodel.EstadoMusica
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * Barra del reproductor: aparece abajo en cualquier sección mientras hay música en la cola.
 * Todos los botones se manejan con el control (anterior, reproducir/pausar, siguiente, cerrar).
 */
@Composable
fun TvBarraMusica(
    estado: EstadoMusica,
    onAnterior: () -> Unit,
    onAlternar: () -> Unit,
    onSiguiente: () -> Unit,
    onCerrar: () -> Unit
) {
    val actual = estado.actual ?: return
    Surface(color = Color(0xFF1B2A1E), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(actual.titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        estado.error ?: "${actual.artista} · ${estado.origen} · ${estado.indice + 1} de ${estado.cola.size}",
                        color = if (estado.error != null) FestivalBadgeLive else FestivalTextSecondary,
                        fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                BotonMusica(Icons.Default.SkipPrevious, "Anterior", onAnterior)
                BotonMusica(if (estado.sonando) Icons.Default.Pause else Icons.Default.PlayArrow, if (estado.sonando) "Pausar" else "Reproducir", onAlternar, destacado = true)
                BotonMusica(Icons.Default.SkipNext, "Siguiente", onSiguiente)
                BotonMusica(Icons.Default.Close, "Cerrar reproductor", onCerrar)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (estado.posicionMs.toFloat() / estado.duracionMs.coerceAtLeast(1)).coerceIn(0f, 1f) },
                color = FestivalGold,
                trackColor = Color(0xFF2E3D30),
                modifier = Modifier.fillMaxWidth().height(3.dp)
            )
        }
    }
}

@Composable
private fun BotonMusica(icono: ImageVector, descripcion: String, onClick: () -> Unit, destacado: Boolean = false) {
    var enfocado by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = when {
            destacado -> FestivalGold
            enfocado -> Color.White.copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(44.dp)
            .onFocusChanged { enfocado = it.isFocused }
            .border(if (enfocado) 2.dp else 0.dp, if (enfocado) Color.White else Color.Transparent, RoundedCornerShape(50))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = descripcion, tint = if (destacado) Color.Black else Color.White)
        }
    }
}

/**
 * Fila de una canción: número, título, artista y duración. Pulsar OK la reproduce;
 * si es la que suena, muestra el ícono de pausa.
 */
@Composable
fun TvFilaCancion(numero: Int, cancion: CancionDto, sonando: Boolean, onClick: () -> Unit) {
    var enfocada by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        color = when {
            enfocada -> Color(0xFF2E3D30)
            sonando -> Color(0xFF2C2214)
            else -> FestivalCardBg
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { enfocada = it.isFocused }
            .border(if (enfocada) 2.dp else 0.dp, if (enfocada) FestivalGold else Color.Transparent, RoundedCornerShape(10.dp))
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                if (sonando) Icon(Icons.Default.GraphicEq, contentDescription = "Sonando", tint = FestivalGold, modifier = Modifier.size(20.dp))
                else Text("$numero", color = FestivalTextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(cancion.titulo, color = if (sonando) FestivalGold else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(cancion.artista, color = FestivalTextSecondary, fontSize = 12.sp, maxLines = 1)
            }
            Text(
                "%d:%02d".format(cancion.duracion / 60, cancion.duracion % 60),
                color = FestivalTextSecondary, fontSize = 12.sp
            )
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(22.dp))
        }
    }
}

/** Aviso a pantalla completa para "cargando", "sin conexión" o "vacío". */
@Composable
fun TvAviso(
    icono: ImageVector,
    titulo: String,
    texto: String,
    boton: String? = null,
    onBoton: () -> Unit = {},
    cargando: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (cargando) CircularProgressIndicator(color = FestivalGold)
        else Icon(icono, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(titulo, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (texto.isNotEmpty()) Text(texto, color = FestivalTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (boton != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBoton, colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)) {
                Text(boton, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Botón de acción de la TV con borde dorado al enfocarlo. */
@Composable
fun TvBoton(texto: String, icono: ImageVector?, onClick: () -> Unit, principal: Boolean = true, modifier: Modifier = Modifier) {
    if (principal) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
        ) {
            if (icono != null) { Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
            Text(texto, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(8.dp), modifier = modifier) {
            if (icono != null) { Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
            Text(texto, color = Color.White)
        }
    }
}

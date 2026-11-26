package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.R
import mx.utng.festivaltrack.tv.ui.theme.*
import java.time.Instant

/**
 * Inicio de la TV.
 *
 * - Banner: si de verdad hay transmisión, "EN VIVO AHORA: <concierto>" con su botón; si no,
 *   el próximo concierto con su fecha y hora de México y el botón para comprar boletos.
 * - Carrusel con los próximos conciertos reales (sin cancelados ni pasados).
 * - Arriba, la hora actual en Dolores Hidalgo.
 *
 * @param onComprar Abre la compra con QR; recibe el concierto a preseleccionar (o null).
 */
@Composable
fun TvMainScreen(
    proximos: List<EventoEntity>,
    stream: StreamStatusDto?,
    ahora: Instant,
    usuario: String?,
    onVerEnVivo: () -> Unit,
    onVerProgramacion: () -> Unit,
    onComprar: (String?) -> Unit,
    onAbrirConcierto: (String) -> Unit,
    onEscucharMusica: () -> Unit
) {
    val enVivo = stream?.isLive == true
    val siguiente = proximos.firstOrNull()

    // Se desplaza (con el foco del control) cuando la barra de música le quita espacio.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        // Barra superior: saludo y hora real
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (usuario != null) "Hola, $usuario" else "Bienvenido al festival",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Text(
                "Dolores Hidalgo · ${HorarioFestival.fechaActual(ahora)} · ${HorarioFestival.horaActual(ahora)}",
                color = FestivalTextSecondary, fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Banner principal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.hero_dolores_hidalgo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.95f), Color.Black.copy(alpha = 0.45f))))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        if (enVivo) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(FestivalBadgeLive))
                                Spacer(Modifier.width(8.dp))
                                Text("EN VIVO AHORA", color = FestivalBadgeLive, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stream?.eventoNombre ?: "Transmisión del festival",
                                color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold
                            )
                            Text("Míralo en esta pantalla y platica en el chat.", color = FestivalTextSecondary, fontSize = 13.sp)
                        } else if (siguiente != null) {
                            Text(
                                if (HorarioFestival.enCurso(siguiente, ahora)) "EN CURSO" else "PRÓXIMO CONCIERTO",
                                color = FestivalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(siguiente.nombre, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${HorarioFestival.cuando(siguiente, ahora)} • ${siguiente.escenario ?: siguiente.ubicacion}",
                                color = FestivalTextSecondary, fontSize = 14.sp
                            )
                        } else {
                            Text("FESTIVAL JOSÉ ALFREDO JIMÉNEZ", color = FestivalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("El Rey vive en su tierra", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text("Todavía no hay conciertos programados.", color = FestivalTextSecondary, fontSize = 14.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (enVivo) {
                            Button(
                                onClick = onVerEnVivo,
                                colors = ButtonDefaults.buttonColors(containerColor = FestivalBadgeLive, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Ver en vivo", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (siguiente != null) {
                            Button(
                                onClick = { onComprar(siguiente.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Comprar boletos", fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(onClick = onVerProgramacion, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Ver programación", color = Color.White)
                        }
                        // Como la tarjeta "Audio" del inicio del móvil: suena el catálogo del artista.
                        OutlinedButton(onClick = onEscucharMusica, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Escuchar música", color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("PRÓXIMOS EN EL ESCENARIO", color = FestivalGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(12.dp))

        if (proximos.isEmpty()) {
            Text(
                "Cuando haya conciertos programados aparecerán aquí.",
                color = FestivalTextSecondary, fontSize = 14.sp
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                items(proximos, key = { it.id }) { evento ->
                    TvEventCard(evento = evento, ahora = ahora, enVivo = enVivo && stream?.eventoId == evento.id, onClick = {
                        if (enVivo && stream?.eventoId == evento.id) onVerEnVivo() else onAbrirConcierto(evento.id)
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Elige un concierto y pulsa OK para escuchar su repertorio y comprar boletos",
            color = FestivalTextSecondary, fontSize = 11.sp, modifier = Modifier.align(Alignment.End)
        )
    }
}

/**
 * Tarjeta de concierto del carrusel; se resalta con borde dorado al enfocarla con el control.
 */
@Composable
fun TvEventCard(evento: EventoEntity, ahora: Instant, enVivo: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        color = if (isFocused) Color(0xFF2E3D30) else FestivalCardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(240.dp)
            .height(140.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) FestivalGold else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.mariachi_gala_stage),
                contentDescription = evento.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)))))
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = when {
                        enVivo -> FestivalBadgeLive
                        else -> FestivalGold
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (enVivo) "EN VIVO" else "${HorarioFestival.fechaCorta(evento)} · ${HorarioFestival.hora(evento)}",
                        color = if (enVivo) Color.White else Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Column {
                    Text(evento.nombre, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(evento.escenario ?: evento.ubicacion, color = FestivalTextSecondary, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

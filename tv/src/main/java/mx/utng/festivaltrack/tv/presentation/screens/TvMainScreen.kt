package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.tv.R
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.*

@Composable
fun TvMainScreen(
    eventos: List<EventoEntity>,
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit,
    onVerEnVivo: () -> Unit,
    onComprarBoletos: () -> Unit
) {
    var activeDialogText by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
    ) {
        // ------------------ LEFT SIDEBAR ------------------
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(FestivalSidebarBg)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Festival 2024",
                    color = FestivalGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Dolores Hidalgo, Cuna de la Independencia",
                    color = FestivalTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(32.dp))

                val navItems = listOf(
                    "Inicio" to Icons.Default.Home,
                    "Galería Histórica" to Icons.Default.Collections,
                    "Transmisión En Vivo" to Icons.Default.LiveTv,
                    "Programación" to Icons.Default.Event,
                    "Ajustes" to Icons.Default.Settings
                )

                navItems.forEachIndexed { index, (label, icon) ->
                    val isSelected = currentNavIndex == index
                    SidebarMenuItem(
                        label = label,
                        icon = icon,
                        isSelected = isSelected,
                        onClick = { onNavSelect(index) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Bottom CTA Button in Sidebar
            Button(
                onClick = onComprarBoletos,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FestivalGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("COMPRAR BOLETOS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ------------------ MAIN CONTENT AREA ------------------
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = FestivalTextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BUSCAR ARTISTA", color = FestivalTextSecondary, fontSize = 14.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("DOLORES HIDALGO 19:42", color = FestivalTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(FestivalCardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = FestivalGold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // HERO SECTION CARD with Local Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_dolores_hidalgo),
                    contentDescription = "Hero Festival",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.95f), Color.Black.copy(alpha = 0.5f))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "FESTIVAL REGIONAL MEXICANO",
                                color = FestivalGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "La Voz del Pueblo:\nHomenaje a José Alfredo Jiménez",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 28.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sintoniza en vivo desde Dolores Hidalgo, la Cuna de la Independencia. Vive una noche de gala con los mejores intérpretes.",
                                color = FestivalTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onVerEnVivo,
                                colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ver en Vivo", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { activeDialogText = "Sintonizando la transmisión en vivo del Festival José Alfredo Jiménez." },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color.White, Color.White))),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Detalles")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CAROUSEL SECTION: PRÓXIMOS EN EL ESCENARIO
            Text(
                text = "PRÓXIMOS EN EL ESCENARIO",
                color = FestivalGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            val displayList = if (eventos.isNotEmpty()) eventos else listOf(
                EventoEntity("1", "Voz del Mariachi", "2020-11-20T20:00:00Z", "Escenario Principal", "Escenario Principal", null, "ACTIVO", null, "Mariachi Sol"),
                EventoEntity("2", "Cuerdas de Dolores", "2020-11-20T21:30:00Z", "Plaza Principal", "Plaza Principal", null, "ACTIVO", null, "Orquesta"),
                EventoEntity("3", "Cena de Gala", "2020-11-20T23:00:00Z", "Jardín Histórico", "Jardín Histórico", null, "ACTIVO", null, "Tributo")
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(displayList) { evento ->
                    TvEventCard(evento = evento, onClick = { activeDialogText = "Evento: ${evento.nombre}\nLugar: ${evento.ubicacion}\nHora: ${evento.fechaHora.takeLast(8).take(5)}" })
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Navigation Hint at bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "→ USA EL CONTROL PARA NAVEGAR",
                    color = FestivalTextSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    if (activeDialogText != null) {
        AlertDialog(
            onDismissRequest = { activeDialogText = null },
            title = { Text("Smart TV Festival", color = FestivalGold, fontWeight = FontWeight.Bold) },
            text = { Text(activeDialogText!!, color = Color.White) },
            confirmButton = {
                Button(
                    onClick = { activeDialogText = null },
                    colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)
                ) {
                    Text("CERRAR", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = FestivalCardBg
        )
    }
}

@Composable
fun TvEventCard(evento: EventoEntity, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF2E3D30) else FestivalCardBg
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(220.dp)
            .height(130.dp)
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) FestivalGold else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.mariachi_gala_stage),
                contentDescription = evento.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = FestivalGold,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = evento.fechaHora.takeLast(8).take(5).ifEmpty { "20:00 HRS" },
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = evento.nombre,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = evento.ubicacion,
                        color = FestivalTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

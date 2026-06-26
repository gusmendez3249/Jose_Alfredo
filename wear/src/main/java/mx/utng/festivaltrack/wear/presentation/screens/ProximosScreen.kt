package mx.utng.festivaltrack.wear.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.material.*
import mx.utng.festivaltrack.wear.data.local.entity.EventoEntity
import mx.utng.festivaltrack.wear.R
import mx.utng.jose_alfredo.presentation.theme.*

@Composable
fun ProximosScreen(
    onEventoClick: (String) -> Unit,
    eventos: List<EventoEntity> = listOf(
        EventoEntity(id="1", nombre="Concierto Inaugural", fechaHora="2026-10-15T20:00:00Z", ubicacion="Parroquia de Nuestra Señora", escenario="Atrio Principal", bannerUrl=null, estado="PROGRAMADO", artistaId=null, artistaNombre="Sinfónica Nacional"),
        EventoEntity(id="2", nombre="Homenaje a José Alfredo", fechaHora="2026-10-15T21:45:00Z", ubicacion="Casa Museo José Alfredo", escenario="Patio Central", bannerUrl=null, estado="PROGRAMADO", artistaId=null, artistaNombre="Mariachi Vargas"),
        EventoEntity(id="3", nombre="Exhibición Histórica", fechaHora="2026-10-15T23:15:00Z", ubicacion="Museo de la Independencia", escenario="Sala 1", bannerUrl=null, estado="PROGRAMADO", artistaId=null, artistaNombre="Compañía de Teatro")
    )
) {
    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_festival),
                        contentDescription = "Festival Logo",
                        modifier = Modifier.size(48.dp).padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Próximos",
                        color = FestivalGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            itemsIndexed(eventos) { index, evento ->
                val label = if (index == 0) "AHORA" else if (index == 1) "SIGUIENTE" else null
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    if (label != null) {
                        Text(
                            text = label,
                            color = FestivalTextSecondary,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp, start = 8.dp)
                        )
                    }
                    Card(
                        onClick = { onEventoClick(evento.id) },
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = FestivalSurfaceCard,
                            endBackgroundColor = FestivalSurfaceCard
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = evento.artistaNombre ?: evento.nombre,
                                    color = FestivalTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = evento.fechaHora.substring(11, 16),
                                    color = FestivalGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "Location",
                                    tint = FestivalTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = (evento.escenario ?: evento.ubicacion).uppercase(),
                                    color = FestivalTextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            item {
                Chip(
                    onClick = { /* navegar a programa completo */ },
                    label = { Text("Programa completo", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ChipDefaults.chipColors(
                        backgroundColor = FestivalGold,
                        contentColor = FestivalTextOnGold
                    )
                )
            }
        }
    }
}

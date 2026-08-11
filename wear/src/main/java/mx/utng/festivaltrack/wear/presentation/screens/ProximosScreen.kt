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
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.wear.R
import mx.utng.jose_alfredo.presentation.theme.*

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import mx.utng.festivaltrack.wear.presentation.viewmodel.ProximosViewModel

@Composable
fun ProximosScreen(
    onEventoClick: (String) -> Unit,
    onProgramaCompletoClick: () -> Unit,
    viewModel: ProximosViewModel = viewModel()
) {
    val eventos by viewModel.eventos.collectAsState()
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
                val uiData = remember(evento.fechaHora) {
                    val currentInstant = java.time.Instant.now()
                    
                    val eventoInstant = try {
                        if (evento.fechaHora.endsWith("Z")) {
                            java.time.Instant.parse(evento.fechaHora)
                        } else {
                            java.time.LocalDateTime.parse(evento.fechaHora).atZone(java.time.ZoneId.systemDefault()).toInstant()
                        }
                    } catch (e: Exception) {
                        currentInstant
                    }
                    
                    val duration = java.time.Duration.between(currentInstant, eventoInstant)
                    val label = if (duration.isNegative || duration.isZero) {
                        "AHORA EN CURSO"
                    } else {
                        val dias = duration.toDays()
                        val horas = duration.toHours() % 24
                        val minutos = duration.toMinutes() % 60
                        
                        when {
                            dias > 0 -> "Faltan $dias días y $horas hr"
                            horas > 0 -> "Faltan $horas hr $minutos min"
                            else -> "Faltan $minutos min"
                        }
                    }
                    
                    val horaLocal = try {
                        val localDateTime = java.time.LocalDateTime.ofInstant(eventoInstant, java.time.ZoneId.systemDefault())
                        localDateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    } catch (e: Exception) {
                        evento.fechaHora.take(16).takeLast(5)
                    }
                    
                    Pair(label, horaLocal)
                }
                
                val label = uiData.first
                val horaLocal = uiData.second
                
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = label,
                        color = FestivalTextSecondary,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp, start = 8.dp)
                    )
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
                                    text = horaLocal,
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
                    onClick = onProgramaCompletoClick,
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

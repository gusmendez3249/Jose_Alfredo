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
import mx.utng.festivaltrack.wear.R
import mx.utng.jose_alfredo.presentation.theme.*

import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.wear.presentation.viewmodel.ProximosViewModel

/**
 * Pantalla que muestra el programa completo del festival.
 * Utiliza [ScalingLazyColumn] para renderizar todos los eventos ordenados por fecha y hora
 * en la interfaz circular de Wear OS.
 *
 * @param onEventoClick Acción que se invoca al presionar una tarjeta de evento, pasando el ID.
 * @param viewModel ViewModel de donde se extrae el [StateFlow] con todos los eventos.
 */
@Composable
fun ProgramaCompletoScreen(
    onEventoClick: (String) -> Unit,
    viewModel: ProximosViewModel = viewModel()
) {
    val eventos by viewModel.todosLosEventos.collectAsState()
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
                        text = "Programa",
                        color = FestivalGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            itemsIndexed(eventos) { index, evento ->
                val horaLocal = remember(evento.fechaHora) {
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
                    
                    try {
                        val localDateTime = java.time.LocalDateTime.ofInstant(eventoInstant, java.time.ZoneId.systemDefault())
                        localDateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd HH:mm"))
                    } catch (e: Exception) {
                        evento.fechaHora.take(16) // fallback
                    }
                }
                
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = horaLocal,
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
                            Text(
                                text = evento.artistaNombre ?: evento.nombre,
                                color = FestivalTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 2
                            )
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
        }
    }
}

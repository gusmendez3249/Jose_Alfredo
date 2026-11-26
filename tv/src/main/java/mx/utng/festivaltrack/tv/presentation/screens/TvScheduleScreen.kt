package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.ui.theme.*
import java.time.Instant

/**
 * Programación del festival: los próximos conciertos reales agrupados por día, con la hora
 * de México. Marca "EN CURSO" el que ya empezó y "EN VIVO" el que se está transmitiendo.
 * Al pulsar OK sobre uno se abre su compra con QR (o la transmisión, si está en vivo).
 */
@Composable
fun TvScheduleScreen(
    proximos: List<EventoEntity>,
    stream: StreamStatusDto?,
    ahora: Instant,
    onComprar: (String) -> Unit,
    onVerEnVivo: () -> Unit
) {
    val porDia = remember(proximos, ahora) { proximos.groupBy { HorarioFestival.dia(it, ahora) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
            .padding(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Programación del festival", color = FestivalGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Horarios del centro de México · Dolores Hidalgo", color = FestivalTextSecondary, fontSize = 12.sp)
            }
            Surface(color = FestivalCardBg, shape = RoundedCornerShape(8.dp)) {
                Text(
                    "${HorarioFestival.fechaActual(ahora).uppercase()} · ${HorarioFestival.horaActual(ahora)}",
                    color = FestivalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (proximos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.EventBusy, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("Aún no hay conciertos programados", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Cuando se publiquen aparecerán aquí con su horario.", color = FestivalTextSecondary, fontSize = 14.sp)
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            porDia.forEach { (dia, conciertos) ->
                item(key = "dia-$dia") {
                    Text(
                        dia.replaceFirstChar { it.uppercase() },
                        color = FestivalGold, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(conciertos, key = { it.id }) { evento ->
                    val transmitiendo = stream?.isLive == true && stream.eventoId == evento.id
                    ScheduleCard(
                        evento = evento,
                        enCurso = HorarioFestival.enCurso(evento, ahora),
                        enVivo = transmitiendo,
                        onClick = { if (transmitiendo) onVerEnVivo() else onComprar(evento.id) }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de un concierto en la programación; se resalta al enfocarla con el control.
 */
@Composable
fun ScheduleCard(evento: EventoEntity, enCurso: Boolean, enVivo: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val destacado = enCurso || enVivo

    Surface(
        onClick = onClick,
        color = when {
            isFocused -> Color(0xFF2E3D30)
            destacado -> Color(0xFF2C2214)
            else -> FestivalCardBg
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 3.dp else if (destacado) 1.dp else 0.dp,
                color = if (isFocused) FestivalGold else if (destacado) FestivalGold.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(color = if (destacado) FestivalGold else FestivalSidebarBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        "${HorarioFestival.hora(evento)} HRS",
                        color = if (destacado) Color.Black else FestivalGold,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(evento.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (enVivo || enCurso) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(color = FestivalBadgeLive, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    if (enVivo) "EN VIVO" else "EN CURSO",
                                    color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        listOfNotNull(evento.artistaNombre, evento.escenario ?: evento.ubicacion).joinToString(" • "),
                        color = FestivalTextSecondary, fontSize = 12.sp
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (enVivo) Icons.Default.LiveTv else Icons.Default.ConfirmationNumber,
                    contentDescription = null, tint = FestivalGold, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (enVivo) "Ver en vivo" else "Comprar", color = FestivalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

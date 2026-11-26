package mx.utng.festivaltrack.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import mx.utng.festivaltrack.shared.data.local.FestivalDatabase
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.wear.presentation.util.AgendaWear
import mx.utng.festivaltrack.wear.presentation.util.rememberUbicacionEnVivo
import mx.utng.jose_alfredo.presentation.theme.*

/** Carga un concierto de la base local. */
@Composable
internal fun rememberEvento(eventoId: String): EventoEntity? {
    val context = LocalContext.current
    var evento by remember { mutableStateOf<EventoEntity?>(null) }
    LaunchedEffect(eventoId) { evento = FestivalDatabase.getInstance(context).eventoDao().getEventoById(eventoId) }
    return evento
}

/**
 * Detalle de un concierto: cuándo, dónde y a qué distancia estás (con tu ubicación en vivo),
 * y el botón para ver la ruta a pie. También es la pantalla que abre el aviso de 15 minutos.
 */
@Composable
fun ConciertoScreen(eventoId: String, onComoLlegar: () -> Unit) {
    val evento = rememberEvento(eventoId)
    val ahora = rememberAhora()
    val ubicacion = rememberUbicacionEnVivo()
    val pos = ubicacion.posicion
    val destino = evento?.let { e -> if (e.latitud != null && e.longitud != null) e.latitud!! to e.longitud!! else null }
    val metros = if (pos != null && destino != null) AgendaWear.distanciaM(pos.latitud, pos.longitud, destino.first, destino.second) else null
    val estadoLista = rememberScalingLazyListState(initialCenterItemIndex = 0)

    Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = estadoLista) }) {
        ScalingLazyColumn(
            state = estadoLista,
            autoCentering = null,
            modifier = Modifier.fillMaxSize().background(FestivalBackground),
            contentPadding = PaddingValues(top = 36.dp, start = 14.dp, end = 14.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (evento == null) {
                item { CircularProgressIndicator(indicatorColor = FestivalGold, modifier = Modifier.size(28.dp)) }
                return@ScalingLazyColumn
            }
            item {
                Text(
                    "${AgendaWear.etiquetaDia(evento, ahora)} · ${AgendaWear.hora(evento)}".uppercase(),
                    color = FestivalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                )
            }
            item {
                Text(
                    evento.nombre, color = FestivalTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, lineHeight = 19.sp
                )
            }
            item {
                // El ícono queda pegado al texto aunque el lugar ocupe dos líneas.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = FestivalTextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        AgendaWear.lugar(evento), color = FestivalTextSecondary, fontSize = 12.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
            // Cuenta regresiva en una píldora: lo segundo que más importa de un vistazo.
            item {
                Text(
                    AgendaWear.faltan(evento, ahora),
                    color = FestivalTextOnGold, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(FestivalGold)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            // Distancia en vivo con una flecha que apunta hacia el escenario.
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    when {
                        destino == null -> Text("Sin ubicación del escenario", color = FestivalTextSecondary, fontSize = 11.sp)
                        !ubicacion.conPermiso -> Text("Activa tu ubicación para ver la distancia", color = FestivalTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                        metros == null -> Text("Buscando tu ubicación…", color = FestivalTextSecondary, fontSize = 11.sp)
                        metros < AgendaWear.LLEGADA_M -> {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ya estás en el lugar", color = FestivalTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Icon(
                                Icons.Filled.Navigation, contentDescription = null, tint = FestivalGold,
                                modifier = Modifier.size(14.dp).rotate(AgendaWear.rumbo(pos!!.latitud, pos.longitud, destino.first, destino.second).toFloat())
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${AgendaWear.textoDistancia(metros)} ${AgendaWear.direccion(pos.latitud, pos.longitud, destino.first, destino.second)}",
                                color = FestivalTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            if (metros != null && metros >= AgendaWear.LLEGADA_M) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = FestivalTextSecondary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("~${AgendaWear.minutosCaminando(metros)} min a pie", color = FestivalTextSecondary, fontSize = 11.sp)
                    }
                }
            }
            item {
                Chip(
                    onClick = onComoLlegar,
                    enabled = destino != null,
                    label = { Text("Cómo llegar", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Filled.Navigation, contentDescription = null, tint = FestivalTextOnGold, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ChipDefaults.chipColors(backgroundColor = FestivalGold, contentColor = FestivalTextOnGold)
                )
            }
        }
    }
}

package mx.utng.festivaltrack.wear.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.wear.R
import mx.utng.festivaltrack.wear.presentation.util.AgendaWear
import mx.utng.festivaltrack.wear.presentation.viewmodel.ProximosViewModel
import mx.utng.jose_alfredo.presentation.theme.*
import java.time.Instant

/**
 * Pantalla principal del reloj: la agenda del festival agrupada por día.
 * El concierto más próximo va destacado con su cuenta regresiva; un toque abre su detalle.
 */
@Composable
fun AgendaScreen(
    onConcierto: (String) -> Unit,
    viewModel: ProximosViewModel = viewModel()
) {
    PedirPermisoDeAvisos()
    val agenda by viewModel.agenda.collectAsState()
    val sincronizando by viewModel.sincronizando.collectAsState()
    val ahora = rememberAhora()
    val estadoLista = rememberScalingLazyListState(initialCenterItemIndex = 0)
    val primero = agenda.firstOrNull()?.second?.firstOrNull()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = estadoLista) }
    ) {
        ScalingLazyColumn(
            state = estadoLista,
            autoCentering = null,
            modifier = Modifier.fillMaxSize().background(FestivalBackground),
            contentPadding = PaddingValues(top = 34.dp, start = 10.dp, end = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_festival),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Agenda", color = FestivalGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            when {
                agenda.isEmpty() && sincronizando -> item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                        CircularProgressIndicator(indicatorColor = FestivalGold, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Cargando agenda…", color = FestivalTextSecondary, fontSize = 12.sp)
                    }
                }
                agenda.isEmpty() -> {
                    item {
                        Text(
                            "No hay conciertos próximos",
                            color = FestivalTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    item {
                        CompactChip(
                            onClick = { viewModel.sincronizar() },
                            label = { Text("Actualizar") },
                            icon = { Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = ChipDefaults.secondaryChipColors()
                        )
                    }
                }
            }

            agenda.forEach { (dia, conciertos) ->
                item {
                    // Encabezado del día compacto (ListHeader deja demasiado aire en pantallas pequeñas).
                    Text(
                        dia.uppercase(), color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp, modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(conciertos, key = { it.id }) { evento ->
                    TarjetaAgenda(
                        evento = evento,
                        destacado = evento == primero,
                        ahora = ahora,
                        onClick = { onConcierto(evento.id) }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de un concierto: la hora grande a la izquierda (lo que más se busca de un vistazo),
 * nombre y lugar a la derecha. La del concierto más próximo lleva borde dorado y cuenta regresiva.
 */
@Composable
private fun TarjetaAgenda(evento: EventoEntity, destacado: Boolean, ahora: Instant, onClick: () -> Unit) {
    val forma = RoundedCornerShape(18.dp)
    Card(
        onClick = onClick,
        backgroundPainter = CardDefaults.cardBackgroundPainter(FestivalSurfaceCard, FestivalSurfaceCard),
        shape = forma,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (destacado) Modifier.border(BorderStroke(1.5.dp, FestivalGold), forma) else Modifier)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                AgendaWear.hora(evento),
                color = FestivalGold, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(52.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    evento.nombre,
                    color = FestivalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = FestivalTextSecondary, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        AgendaWear.lugar(evento), color = FestivalTextSecondary, fontSize = 10.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (destacado) {
                    Text(
                        AgendaWear.faltan(evento, ahora),
                        color = FestivalGold, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/** Hora actual que se refresca cada minuto (para las cuentas regresivas). */
@Composable
internal fun rememberAhora(): Instant {
    var ahora by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            ahora = Instant.now()
        }
    }
    return ahora
}

/** Pide permiso de notificaciones (Wear OS 4+) para los avisos de 15 minutos antes. */
@Composable
private fun PedirPermisoDeAvisos() {
    val context = LocalContext.current
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

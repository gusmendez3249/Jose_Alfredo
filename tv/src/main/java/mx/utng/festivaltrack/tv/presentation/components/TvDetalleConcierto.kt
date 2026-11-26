package mx.utng.festivaltrack.tv.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.CancionDto
import mx.utng.festivaltrack.shared.util.EnlacesWeb
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.presentation.viewmodel.EstadoMusica
import mx.utng.festivaltrack.tv.ui.theme.*
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode
import java.time.Instant

/**
 * Detalle de un concierto, como en el móvil: fecha y lugar en hora de México, su repertorio
 * con demos de 30 s para escuchar y el QR para comprar boletos desde el celular.
 *
 * @param cargarRepertorio Pide al servidor las canciones del concierto (null = sin conexión).
 */
@Composable
fun TvDetalleConcierto(
    evento: EventoEntity,
    ahora: Instant,
    enVivo: Boolean,
    musica: EstadoMusica,
    cargarRepertorio: suspend (String) -> List<CancionDto>?,
    onReproducir: (List<CancionDto>, Int) -> Unit,
    onVerEnVivo: () -> Unit,
    onCerrar: () -> Unit
) {
    var repertorio by remember(evento.id) { mutableStateOf<List<CancionDto>?>(null) }
    var sinConexion by remember(evento.id) { mutableStateOf(false) }
    val primerFoco = remember { FocusRequester() }
    val url = EnlacesWeb.comprar(evento.id)

    LaunchedEffect(evento.id) {
        val lista = cargarRepertorio(evento.id)
        sinConexion = lista == null
        repertorio = lista.orEmpty()
    }
    LaunchedEffect(Unit) { runCatching { primerFoco.requestFocus() } }

    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = FestivalSidebarBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .border(1.dp, FestivalGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (enVivo) "EN VIVO AHORA" else if (HorarioFestival.enCurso(evento, ahora)) "EN CURSO" else "CONCIERTO DEL FESTIVAL",
                        color = if (enVivo) FestivalBadgeLive else FestivalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
                    )
                    Text(evento.nombre, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(HorarioFestival.cuando(evento, ahora), color = Color.White, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(listOfNotNull(evento.escenario, evento.ubicacion).distinct().joinToString(" · "), color = Color.White, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Repertorio", color = FestivalGold, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        val lista = repertorio.orEmpty()
                        if (lista.isNotEmpty()) {
                            TvBoton(
                                texto = if (musica.origen == evento.nombre && musica.sonando) "Pausar" else "Escuchar todo",
                                icono = if (musica.origen == evento.nombre && musica.sonando) Icons.Default.Pause else Icons.Default.PlayArrow,
                                onClick = { onReproducir(lista, 0) },
                                modifier = Modifier.focusRequester(primerFoco)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    when {
                        repertorio == null -> CircularProgressIndicator(color = FestivalGold, modifier = Modifier.size(28.dp))
                        sinConexion -> Text("No se pudo cargar el repertorio. Revisa la conexión.", color = FestivalTextSecondary, fontSize = 13.sp)
                        repertorio!!.isEmpty() -> Text("Este concierto todavía no tiene canciones para escuchar.", color = FestivalTextSecondary, fontSize = 13.sp)
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 230.dp)) {
                            itemsIndexed(repertorio!!, key = { _, c -> c.id }) { i, cancion ->
                                TvFilaCancion(
                                    numero = i + 1,
                                    cancion = cancion,
                                    sonando = musica.origen == evento.nombre && musica.sonando && musica.actual?.id == cancion.id,
                                    onClick = { onReproducir(repertorio!!, i) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (enVivo) TvBoton("Ver en vivo", Icons.Default.LiveTv, onVerEnVivo)
                        TvBoton("Cerrar", null, onCerrar, principal = false)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(240.dp)) {
                    Text("Compra tus boletos", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Escanea con tu celular", color = FestivalTextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    DynamicQrCode(content = url, modifier = Modifier.size(220.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(url, color = FestivalTextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

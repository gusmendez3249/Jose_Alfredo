package mx.utng.festivaltrack.tv.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.PreciosBoleto
import mx.utng.festivaltrack.shared.util.EnlacesWeb
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.ui.theme.*
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode
import java.text.NumberFormat
import java.time.Instant
import java.util.Locale

/**
 * Comprar boletos desde la TV: con el control no se puede pagar, así que se elige el concierto y
 * se escanea un QR real que abre su compra en la web desde el celular.
 *
 * @param conciertos Próximos conciertos (ya filtrados y ordenados).
 * @param preseleccionado Concierto que se elige al abrir (por ejemplo desde su tarjeta).
 */
@Composable
fun TvComprarBoletosDialog(
    conciertos: List<EventoEntity>,
    ahora: Instant,
    preseleccionado: String? = null,
    onCerrar: () -> Unit
) {
    var elegidoId by remember { mutableStateOf(preseleccionado ?: conciertos.firstOrNull()?.id) }
    val elegido = conciertos.firstOrNull { it.id == elegidoId } ?: conciertos.firstOrNull()
    val url = elegido?.let { EnlacesWeb.comprar(it.id) } ?: EnlacesWeb.cartelera()
    val primerFoco = remember { FocusRequester() }
    val pesos = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")).apply { maximumFractionDigits = 0 } }

    LaunchedEffect(Unit) { runCatching { primerFoco.requestFocus() } }

    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = FestivalSidebarBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .border(1.dp, FestivalGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                // Lista de conciertos
                Column(modifier = Modifier.weight(1f)) {
                    Text("Comprar boletos", color = FestivalGold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Elige el concierto con el control y escanea el código con tu celular para pagar ahí.",
                        color = FestivalTextSecondary, fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (conciertos.isEmpty()) {
                        Text("No hay conciertos próximos a la venta.", color = Color.White, fontSize = 15.sp)
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 300.dp)) {
                        items(conciertos, key = { it.id }) { concierto ->
                            var enfocado by remember { mutableStateOf(false) }
                            val esElegido = concierto.id == elegido?.id
                            Surface(
                                onClick = { elegidoId = concierto.id },
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    esElegido -> Color(0xFF2C2214)
                                    enfocado -> Color(0xFF2E3D30)
                                    else -> FestivalCardBg
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (concierto.id == conciertos.first().id) Modifier.focusRequester(primerFoco) else Modifier)
                                    .onFocusChanged {
                                        enfocado = it.isFocused
                                        // Al moverse con las flechas el QR sigue al concierto enfocado.
                                        if (it.isFocused) elegidoId = concierto.id
                                    }
                                    .border(
                                        if (enfocado || esElegido) 2.dp else 0.dp,
                                        if (enfocado || esElegido) FestivalGold else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(concierto.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        "${HorarioFestival.cuando(concierto, ahora)} • ${concierto.escenario ?: concierto.ubicacion}",
                                        color = FestivalTextSecondary, fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "VIP ${pesos.format(PreciosBoleto.VIP)} · General ${pesos.format(PreciosBoleto.GENERAL)}",
                            color = FestivalGold, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = onCerrar, shape = RoundedCornerShape(8.dp)) {
                            Text("Cerrar", color = Color.White)
                        }
                    }
                }

                // QR del concierto elegido
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(260.dp)
                ) {
                    DynamicQrCode(content = url, modifier = Modifier.size(240.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        elegido?.nombre ?: "Cartelera del festival",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center
                    )
                    Text(url, color = FestivalTextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/**
 * Diálogo con un QR y un texto, para acciones que se hacen en el celular (crear cuenta, etc.).
 */
@Composable
fun TvDialogoQr(titulo: String, texto: String, url: String, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = FestivalCardBg,
        title = { Text(titulo, color = FestivalGold, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(texto, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                DynamicQrCode(content = url, modifier = Modifier.size(200.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(url, color = FestivalTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(onClick = onCerrar, colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)) {
                Text("Listo", fontWeight = FontWeight.Bold)
            }
        }
    )
}

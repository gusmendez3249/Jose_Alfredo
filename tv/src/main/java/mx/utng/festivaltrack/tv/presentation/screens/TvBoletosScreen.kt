package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.data.remote.BoletoDto
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.presentation.components.TvAviso
import mx.utng.festivaltrack.tv.presentation.viewmodel.Cargable
import mx.utng.festivaltrack.tv.presentation.viewmodel.SesionTv
import mx.utng.festivaltrack.tv.ui.theme.*
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode
import java.time.Instant

/**
 * Mis boletos (el Perfil del móvil): con sesión iniciada lista los boletos de la cuenta y, al
 * elegir uno, muestra su QR real, el mismo que valida el escáner de la entrada.
 */
@Composable
fun TvBoletosScreen(
    sesion: SesionTv?,
    boletos: Cargable<List<BoletoDto>>,
    ahora: Instant,
    onCargar: () -> Unit,
    onIniciarSesion: () -> Unit,
    onComprar: () -> Unit
) {
    LaunchedEffect(sesion) { if (sesion != null) onCargar() }

    Column(Modifier.fillMaxSize().background(FestivalDarkBg).padding(28.dp)) {
        Text("Mis boletos", color = FestivalGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            sesion?.let { "Hola, ${it.nombre}. Muestra el QR en la entrada desde tu celular o desde aquí." } ?: "Inicia sesión para ver tus boletos.",
            color = FestivalTextSecondary, fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))

        if (sesion == null) {
            TvAviso(Icons.Default.ConfirmationNumber, "Tus boletos aparecen aquí", "Entra con la misma cuenta con la que compraste.", "Iniciar sesión", onIniciarSesion)
            return@Column
        }
        when (boletos) {
            Cargable.Cargando -> TvAviso(Icons.Default.ConfirmationNumber, "Cargando tus boletos…", "", cargando = true)
            Cargable.SinConexion -> TvAviso(Icons.Default.CloudOff, "Sin conexión con el servidor", "No se pudieron cargar tus boletos.", "Intentar de nuevo", onCargar)
            is Cargable.Listo -> {
                val lista = boletos.datos.sortedBy { it.evento?.fechaHora ?: "" }
                if (lista.isEmpty()) {
                    TvAviso(Icons.Default.ConfirmationNumber, "Todavía no tienes boletos", "Compra desde tu celular escaneando el QR.", "Comprar boletos", onComprar)
                    return@Column
                }
                var elegidoId by remember(lista) { mutableStateOf(lista.first().id) }
                val elegido = lista.firstOrNull { it.id == elegidoId } ?: lista.first()
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxSize()) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        items(lista, key = { it.id }) { boleto ->
                            FilaBoleto(boleto, ahora, elegido = boleto.id == elegido.id) { elegidoId = boleto.id }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(300.dp)) {
                        DynamicQrCode(content = elegido.codigoQR, modifier = Modifier.size(260.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(elegido.evento?.nombre ?: "Festival", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
                        Text("Boleto ${elegido.categoria} · ${estadoLegible(elegido.estado)}", color = FestivalGold, fontSize = 13.sp)
                        Text("ID: ${elegido.id}", color = FestivalTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun estadoLegible(estado: String) = when (estado.uppercase()) {
    "ACTIVO" -> "Activo"
    "USADO" -> "Ya usado"
    "CANCELADO" -> "Cancelado"
    else -> estado
}

@Composable
private fun FilaBoleto(boleto: BoletoDto, ahora: Instant, elegido: Boolean, onElegir: () -> Unit) {
    var enfocado by remember { mutableStateOf(false) }
    val evento = boleto.evento?.toEntity()
    Surface(
        onClick = onElegir,
        color = when {
            elegido -> Color(0xFF2C2214)
            enfocado -> Color(0xFF2E3D30)
            else -> FestivalCardBg
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { enfocado = it.isFocused; if (it.isFocused) onElegir() }
            .border(if (enfocado || elegido) 2.dp else 0.dp, if (enfocado || elegido) FestivalGold else Color.Transparent, RoundedCornerShape(12.dp))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.QrCode2, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(boleto.evento?.nombre ?: "Concierto del festival", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    listOfNotNull(evento?.let { HorarioFestival.cuando(it, ahora) }?.takeIf { it.isNotEmpty() }, evento?.escenario ?: evento?.ubicacion).joinToString(" · "),
                    color = FestivalTextSecondary, fontSize = 12.sp
                )
            }
            Surface(color = if (boleto.categoria.uppercase() == "VIP") FestivalGold else FestivalSidebarBg, shape = RoundedCornerShape(6.dp)) {
                Text(
                    boleto.categoria, color = if (boleto.categoria.uppercase() == "VIP") Color.Black else FestivalGold,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            if (boleto.estado.uppercase() != "ACTIVO") {
                Spacer(Modifier.width(8.dp))
                Text(estadoLegible(boleto.estado), color = FestivalTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

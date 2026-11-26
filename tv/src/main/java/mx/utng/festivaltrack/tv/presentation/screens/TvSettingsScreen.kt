package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import mx.utng.festivaltrack.shared.util.EnlacesWeb
import mx.utng.festivaltrack.tv.presentation.viewmodel.SesionTv
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * Ajustes de la TV con datos reales: a qué servidor se conecta y si responde, el estado de la
 * transmisión, la página donde se compran boletos y la cuenta (iniciar o cerrar sesión).
 */
@Composable
fun TvSettingsScreen(
    servidorConectado: Boolean?,
    stream: StreamStatusDto?,
    sesion: SesionTv?,
    onIniciarSesion: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }
    var confirmarSalida by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
            .padding(32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text("Ajustes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Versión $version", color = FestivalTextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(20.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            AjusteCard(
                icon = Icons.Default.Person,
                title = "Cuenta",
                subtitle = sesion?.let { "${it.nombre} · ${it.correo}" } ?: "Invitado: puedes ver todo; para escribir en el chat inicia sesión"
            ) {
                if (sesion != null) {
                    Button(
                        onClick = { confirmarSalida = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC51111), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cerrar sesión", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onIniciarSesion,
                        colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Iniciar sesión", fontWeight = FontWeight.Bold)
                    }
                }
            }
            AjusteCard(
                icon = Icons.Default.Dns,
                title = "Servidor del festival",
                subtitle = EnlacesWeb.servidor,
                estado = when (servidorConectado) {
                    true -> "Conectado" to Color(0xFF66BB6A)
                    false -> "Sin conexión" to FestivalBadgeLive
                    null -> "Comprobando…" to FestivalTextSecondary
                }
            )
            AjusteCard(
                icon = Icons.Default.LiveTv,
                title = "Transmisión en vivo",
                subtitle = when {
                    stream == null -> "No se sabe: el servidor no responde"
                    stream.isLive -> "En vivo: ${stream.eventoNombre ?: "concierto del festival"}"
                    else -> "Sin transmisión en este momento"
                }
            )
            AjusteCard(
                icon = Icons.Default.QrCode,
                title = "Compra de boletos con el celular",
                subtitle = "Los códigos QR abren ${EnlacesWeb.base}"
            )
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            containerColor = FestivalCardBg,
            title = { Text("¿Cerrar sesión?", color = FestivalGold, fontWeight = FontWeight.Bold) },
            text = { Text("La TV seguirá funcionando como invitado.", color = Color.White) },
            confirmButton = {
                Button(
                    onClick = { confirmarSalida = false; onCerrarSesion() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC51111), contentColor = Color.White)
                ) { Text("Cerrar sesión", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar", color = Color.Gray) } }
        )
    }
}

@Composable
private fun AjusteCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    estado: Pair<String, Color>? = null,
    accion: (@Composable () -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FestivalCardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color.Gray, fontSize = 13.sp)
            }
            estado?.let { (texto, color) ->
                Text(texto, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            accion?.invoke()
        }
    }
}

package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.utils.ProximosEventos
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.PublicacionDto
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import java.time.Duration
import java.time.Instant

/** Datos reales del festival para el panel del administrador (null = no se pudo leer). */
private data class ResumenAdmin(
    val canciones: Int?,
    val fotos: Int?,
    val usuarios: Int?,
    val proximosEventos: Int?,
    val stream: StreamStatusDto?,
    val publicaciones: List<PublicacionDto>
)

private suspend fun cargarResumen(api: FestivalApiService, bearer: String): ResumenAdmin = coroutineScope {
    val canciones = async { runCatching { api.getCanciones().size }.getOrNull() }
    val fotos = async { runCatching { api.getGalerias().sumOf { it.imagenes.size } }.getOrNull() }
    val usuarios = async { runCatching { api.getUsuarios(bearer).size }.getOrNull() }
    val eventos = async { runCatching { ProximosEventos.filtrar(api.getEventos().map { it.toEntity() }).size }.getOrNull() }
    val stream = async { runCatching { api.getStreamStatus() }.getOrNull() }
    val foro = async { runCatching { api.getForo(bearer, limit = 3) }.getOrDefault(emptyList()) }
    ResumenAdmin(canciones.await(), fotos.await(), usuarios.await(), eventos.await(), stream.await(), foro.await())
}

/** "hace 5 min", "hace 2 h", "hace 3 días". */
private fun haceCuanto(iso: String): String = runCatching {
    val d = Duration.between(Instant.parse(iso), Instant.now())
    when {
        d.toMinutes() < 1 -> "ahora"
        d.toHours() < 1 -> "hace ${d.toMinutes()} min"
        d.toDays() < 1 -> "hace ${d.toHours()} h"
        else -> "hace ${d.toDays()} días"
    }
}.getOrDefault("")

/**
 * Inicio del administrador: estado real de la transmisión, métricas del catálogo y del festival,
 * accesos a las secciones de gestión y la actividad reciente de la comunidad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToLivePanel: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onIrASubir: () -> Unit = {},
    onIrAGestionar: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val nombre = TokenManager(context).getUserName() ?: "Admin"

    // Se refresca cada 15 s: el estado de la transmisión cambia mientras el admin está aquí.
    val resumen by produceState<ResumenAdmin?>(initialValue = null) {
        val api = FestivalApiService.create()
        val bearer = "Bearer ${TokenManager(context).getToken()}"
        while (true) {
            value = cargarResumen(api, bearer)
            delay(15_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1410))
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("José Alfredo", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                Text("Jiménez", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
            }
            IconButton(
                onClick = onNavigateToUsers,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1E2720))
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Gestionar usuarios", tint = PrimaryGold, modifier = Modifier.size(20.dp))
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("¡Bienvenido,\n$nombre!", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Gestiona el contenido del festival y la transmisión en vivo de los conciertos.",
                color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Estado real de la transmisión (GET /stream/status)
        val stream = resumen?.stream
        val enVivo = stream?.isLive == true
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("TRANSMISIÓN EN VIVO", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (enVivo) Color(0xFFE53935) else Color.Gray))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when {
                            resumen == null -> "Consultando…"
                            stream == null -> "Sin conexión con el servidor"
                            enVivo -> "En vivo" + (stream.eventoNombre?.let { ": $it" } ?: "")
                            else -> "Sin transmisión"
                        },
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
                if (enVivo) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stream!!.streamUrl, color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToLivePanel,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (enVivo) "VER PANEL DE TRANSMISIÓN" else "INICIAR TRANSMISIÓN EN VIVO", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Resumen del\nfestival",
            color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        fun cifra(n: Int?) = if (resumen == null) "…" else n?.toString() ?: "—"
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricBox(icon = Icons.Default.MusicNote, count = cifra(resumen?.canciones), label = "Canciones\nen el catálogo", modifier = Modifier.weight(1f))
            MetricBox(icon = Icons.Default.PhotoLibrary, count = cifra(resumen?.fotos), label = "Fotos en\nla galería", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricBox(icon = Icons.Default.Event, count = cifra(resumen?.proximosEventos), label = "Próximos\nconciertos", modifier = Modifier.weight(1f))
            MetricBox(icon = Icons.Default.People, count = cifra(resumen?.usuarios), label = "Usuarios\nregistrados", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Gestión\nrápida",
            color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        ManagementCard(
            title = "Catálogo de canciones",
            desc = "Sube canciones con su portada y audio; después asígnalas al repertorio de cada concierto.",
            btn = "SUBIR CANCIÓN",
            onClick = onIrASubir
        )
        Spacer(modifier = Modifier.height(16.dp))
        ManagementCard(
            title = "Conciertos y repertorio",
            desc = "Crea, edita o elimina conciertos y elige qué canciones se escuchan en cada uno.",
            btn = "GESTIONAR CONCIERTOS",
            onClick = onIrAGestionar
        )
        Spacer(modifier = Modifier.height(16.dp))
        ManagementCard(
            title = "Transmisión en vivo",
            desc = "Transmite un concierto con la cámara del teléfono; los asistentes lo ven en la app y en la TV.",
            btn = "IR AL PANEL LIVE",
            onClick = onNavigateToLivePanel
        )

        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Actividad reciente de la comunidad", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                val publicaciones = resumen?.publicaciones.orEmpty()
                if (publicaciones.isEmpty()) {
                    Text(
                        if (resumen == null) "Cargando…" else "Todavía no hay publicaciones en la galería.",
                        color = Color.Gray, fontSize = 13.sp
                    )
                }
                publicaciones.forEachIndexed { i, pub ->
                    ActivityItem(
                        icon = Icons.Default.Forum,
                        iconColor = Color(0xFF4CAF50),
                        title = "${pub.autorNombre}: ${pub.titulo ?: pub.contenido}",
                        time = "${haceCuanto(pub.createdAt)} · ${pub.likes} me gusta",
                        hideLine = i == publicaciones.lastIndex
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MetricBox(icon: ImageVector, count: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171A18)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF1E2720), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF8AA694), modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(count, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun ManagementCard(title: String, desc: String, btn: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171A18)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(desc, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGold),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(btn)
            }
        }
    }
}

@Composable
fun ActivityItem(icon: ImageVector, iconColor: Color, title: String, time: String, hideLine: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            if (!hideLine) {
                Box(modifier = Modifier.width(2.dp).height(40.dp).background(iconColor.copy(alpha = 0.3f)))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(time, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

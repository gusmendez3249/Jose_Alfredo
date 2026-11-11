package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.EventosViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Lista de conciertos del festival. Al elegir uno se abre su detalle, donde se
 * escuchan las demos de sus canciones y se compran los boletos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcertsScreen(
    eventosViewModel: EventosViewModel? = null,
    onOpenEvent: (String) -> Unit = {}
) {
    val eventos by eventosViewModel?.eventosLocales?.collectAsState()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Conciertos", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Icon(
                        Icons.Default.Audiotrack, null, tint = PrimaryGold,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { inner ->
        if (eventos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(inner).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Aún no hay conciertos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Cuando haya conciertos programados aparecerán aquí para que escuches sus demos.",
                    color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Escucha un fragmento de las canciones de cada concierto y decide a cuál ir.",
                        color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(eventos, key = { it.id }) { evento ->
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E2720), shape)
                            .border(1.dp, Color(0xFF2E3D31), shape)
                            .clickable { onOpenEvent(evento.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(PrimaryGold.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Audiotrack, null, tint = PrimaryGold) }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                evento.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            val detalle = listOfNotNull(
                                formatConcertDate(evento.fechaHora),
                                evento.escenario?.takeIf { it.isNotBlank() }
                            ).joinToString(" • ")
                            if (detalle.isNotBlank()) {
                                Text(detalle, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Text("Escuchar demos", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = PrimaryGold)
                    }
                }
            }
        }
    }
}

private fun formatConcertDate(iso: String): String = try {
    val fmt = DateTimeFormatter.ofPattern("d 'de' MMMM, HH:mm 'hrs'", Locale("es", "MX"))
    fmt.format(Instant.parse(iso).atZone(ZoneId.systemDefault()))
} catch (e: Exception) {
    ""
}

package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import mx.utng.festivaltrack.app.ui.theme.DarkSurface
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.theme.TextSecondary
import mx.utng.festivaltrack.app.ui.viewmodels.ArViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.HistorialArState
import mx.utng.festivaltrack.shared.data.remote.VisitaSitioDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoFecha = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy, HH:mm", Locale("es", "MX"))

/** Pantalla 3 del módulo AR: historial de sitios visitados. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArHistoryScreen(
    viewModel: ArViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.historial.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargarHistorial() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sitios visitados", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                HistorialArState.Loading -> CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.align(Alignment.Center))
                is HistorialArState.Error -> Text(s.mensaje, color = Color.White, modifier = Modifier.align(Alignment.Center))
                is HistorialArState.Success -> if (s.visitas.isEmpty()) {
                    Text(
                        "Aún no has escaneado ningún sitio.\nApunta la cámara a un monumento para empezar.",
                        color = TextSecondary, textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(s.visitas, key = { it.id }) { VisitaItem(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitaItem(visita: VisitaSitioDto) {
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (visita.sitio.imagenUrl != null) {
                    AsyncImage(
                        model = visita.sitio.imagenUrl, contentDescription = visita.sitio.nombre,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.AccountBalance, null, tint = PrimaryGold, modifier = Modifier.size(32.dp))
                }
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(visita.sitio.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Text(visita.sitio.ubicacion, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                    Text(formatear(visita.fechaVisita), color = PrimaryGold, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

private fun formatear(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(formatoFecha)
} catch (e: Exception) {
    iso
}

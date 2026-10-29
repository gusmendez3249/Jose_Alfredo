package mx.utng.festivaltrack.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import mx.utng.festivaltrack.app.auto.Destino
import mx.utng.festivaltrack.app.auto.DestinosCalculator
import mx.utng.festivaltrack.app.ui.theme.DarkSurface
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.theme.TextSecondary
import mx.utng.festivaltrack.app.ui.viewmodels.DestinosState
import mx.utng.festivaltrack.app.ui.viewmodels.DestinosViewModel

private val VerdeFestival = Color(0xFF243B2E)

/**
 * Pantalla 7 del modo automóvil: el usuario elige a qué escenario o zona del festival llegar.
 * Cada destino muestra distancia y tiempo estimado; "Navegar" guarda la ruta activa para
 * Android Auto y abre la navegación paso a paso.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ElegirDestinoScreen(
    onNavigateBack: () -> Unit,
    viewModel: DestinosViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val permisos = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    ) { viewModel.cargar() }

    LaunchedEffect(Unit) {
        if (permisos.permissions.any { it.status.isGranted }) {
            viewModel.cargar()
        } else {
            permisos.launchMultiplePermissionRequest()
        }
    }

    fun navegar(destino: Destino) {
        viewModel.elegir(destino)
        val uri = Uri.parse("google.navigation:q=${destino.latitud},${destino.longitud}&mode=d")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        val respaldo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${destino.latitud},${destino.longitud}?q=${destino.latitud},${destino.longitud}(${Uri.encode(destino.nombre)})"))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            runCatching { context.startActivity(respaldo) }
                .onFailure { Toast.makeText(context, "Ruta guardada para el auto: ${destino.nombre}", Toast.LENGTH_LONG).show() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS del festival", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Text("GPS DEL FESTIVAL", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("¿A dónde te dirigimos?", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Elige el escenario o zona a la que quieres llegar", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is DestinosState.Loading -> Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGold)
                }
                is DestinosState.Error -> Column(
                    Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.mensaje, color = Color.White, textAlign = TextAlign.Center)
                    TextButton(onClick = { viewModel.cargar() }) { Text("Intentar de nuevo", color = PrimaryGold) }
                }
                is DestinosState.Success -> LazyColumn(
                    Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (s.ubicacionAproximada) {
                        item {
                            Text(
                                "Sin ubicación del teléfono: distancias calculadas desde el Jardín Principal.",
                                color = TextSecondary, fontSize = 12.sp
                            )
                        }
                    }
                    items(s.destinos) { destino -> DestinoCard(destino, onNavegar = { navegar(destino) }) }
                }
            }

            Text(
                "Al seleccionar, la ruta se envía a la pantalla del auto.",
                color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun DestinoCard(destino: Destino, onNavegar: () -> Unit) {
    val destacado = destino.esDestacado
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (destacado) VerdeFestival else DarkSurface),
        border = if (destacado) null else BorderStroke(1.dp, Color(0xFF2C2C2C))
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(destino.nombre, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(destino.subtitulo, color = TextSecondary, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val color = if (destacado) PrimaryGold else TextSecondary
                    Text(DestinosCalculator.textoDistancia(destino.distanciaKm), color = color, fontWeight = FontWeight.Bold)
                    Text("${destino.trasladoMin} min", color = color, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onNavegar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destacado) PrimaryGold else Color(0xFF2C2C2C),
                    contentColor = if (destacado) Color.Black else Color.White
                )
            ) { Text("Navegar →", fontWeight = FontWeight.Bold) }
        }
    }
}

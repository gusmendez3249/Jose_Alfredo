package mx.utng.festivaltrack.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonPinCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var selectedDestinationIndex by remember { mutableStateOf(0) }

    // User start location in Dolores Hidalgo
    val userStartPoint = remember { GeoPoint(21.1530, -100.9340) }

    // Destinations
    val destinations = remember {
        listOf(
            Triple("Mausoleo José Alfredo", GeoPoint(21.1561, -100.9317), "450 m • 6 min a pie"),
            Triple("Teatro del Pueblo", GeoPoint(21.1565, -100.9308), "600 m • 8 min a pie"),
            Triple("Zona Gastronómica", GeoPoint(21.1550, -100.9325), "300 m • 4 min a pie")
        )
    }

    fun updateMapRoute(mapView: MapView, selectedIdx: Int) {
        mapView.overlays.clear()

        val dest = destinations[selectedIdx]
        val destPoint = dest.second

        // Add Stage Markers (Red/Gold markers)
        destinations.forEachIndexed { idx, (name, pt, snippet) ->
            val isSelected = idx == selectedIdx
            val marker = Marker(mapView).apply {
                position = pt
                title = if (isSelected) "🎯 $name (DESTINO)" else "🎪 $name"
                this.snippet = snippet
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                if (isSelected) {
                    showInfoWindow()
                }
            }
            mapView.overlays.add(marker)
        }

        // Add User Location Marker (Distinct Blue Icon with User Label)
        val userMarker = Marker(mapView).apply {
            position = userStartPoint
            title = "📍 ¡AQUÍ ESTÁS TÚ! (TU UBICACIÓN)"
            snippet = "Punto de partida GPS"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            showInfoWindow()
        }
        mapView.overlays.add(userMarker)

        // Draw Interactive Polyline Route to selected destination
        val routeLine = Polyline(mapView).apply {
            val waypoints = arrayListOf(
                userStartPoint,
                GeoPoint((userStartPoint.latitude + destPoint.latitude) / 2, userStartPoint.longitude),
                destPoint
            )
            setPoints(waypoints)
            outlinePaint.color = android.graphics.Color.parseColor("#E6C27A")
            outlinePaint.strokeWidth = 16f
        }
        mapView.overlays.add(routeLine)

        mapView.controller.animateTo(destPoint)
        mapView.controller.setZoom(16.8)
        mapView.invalidate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navegación GPS y Rutas", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive OSMDroid Map View
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        mapViewInstance = this
                        updateMapRoute(this, selectedDestinationIndex)
                    }
                },
                update = { mapView ->
                    updateMapRoute(mapView, selectedDestinationIndex)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Container with Legend & Stage Selector Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Legend Bar
                Surface(
                    color = Color(0xEE1E2720),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF2196F3), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tú (Azul)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("➔", color = PrimaryGold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.size(10.dp).background(PrimaryGold, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Escenario Destino (Dorado)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stage Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    destinations.forEachIndexed { idx, (name, _, _) ->
                        FilterChip(
                            selected = selectedDestinationIndex == idx,
                            onClick = {
                                selectedDestinationIndex = idx
                            },
                            label = { Text(name.take(12) + "...", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGold,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xDD1E2720),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Bottom Navigation Route Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEE1E2720)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(PrimaryGold, RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(destinations[selectedDestinationIndex].first, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Ruta: " + destinations[selectedDestinationIndex].third, color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            mapViewInstance?.let { updateMapRoute(it, selectedDestinationIndex) }
                        },
                        containerColor = PrimaryGold,
                        contentColor = Color.Black,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Recentar")
                    }
                }
            }
        }
    }
}

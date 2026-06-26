package mx.utng.festivaltrack.wear.presentation.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.jose_alfredo.presentation.theme.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import mx.utng.festivaltrack.wear.R

@Composable
fun AlertaScreen(eventoId: String, onVerMapa: () -> Unit) {
    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FestivalBackground)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.DateRange,
                contentDescription = "Calendario",
                tint = FestivalGold,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "¡El evento está por comenzar!",
                color = FestivalTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Serenata al Maestro",
                color = FestivalTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Chip(
                onClick = onVerMapa,
                label = { Text("VER MAPA", fontWeight = FontWeight.Bold) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Pin",
                        tint = FestivalTextOnGold
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ChipDefaults.chipColors(
                    backgroundColor = FestivalGold,
                    contentColor = FestivalTextOnGold
                )
            )
        }
    }
}

@Composable
fun NavEscenarioScreen(eventoId: String, onLlegarAhora: () -> Unit) {
    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FestivalBackground)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Arriba
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = "Flecha",
                    tint = FestivalTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "450m",
                    color = FestivalTextSecondary,
                    fontSize = 12.sp
                )
            }
            // Centro
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = "Lugar",
                    tint = FestivalGold,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Mausoleo",
                    color = FestivalTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp
                )
                Text(
                    text = "JOSÉ ALFREDO JIMÉNEZ",
                    color = FestivalGold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
            // Abajo
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = "Caminando",
                    tint = FestivalTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.height(4.dp))
                Chip(
                    onClick = onLlegarAhora,
                    label = { Text("LLEGAR AHORA", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.chipColors(
                        backgroundColor = FestivalGold,
                        contentColor = FestivalTextOnGold
                    )
                )
            }
        }
    }
}

@Composable
fun MapaAccesoScreen(eventoId: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var routeInfo by remember { mutableStateOf<String?>(null) }

    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        userAgentValue = context.packageName
        osmdroidBasePath = context.cacheDir
        osmdroidTileCache = java.io.File(context.cacheDir, "osmdroid")
    }
    val startPoint = GeoPoint(21.152222, -100.937500)
    val destinationPoint = when (eventoId) {
        "1" -> GeoPoint(21.156111, -100.932500)
        "2" -> GeoPoint(21.157833, -100.934722)
        "3" -> GeoPoint(21.156828, -100.934444)
        else -> GeoPoint(21.156111, -100.932500)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val frameLayout = android.widget.FrameLayout(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                
                val mapView = MapView(ctx).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(16.0)
                    controller.setCenter(startPoint)
                }
                frameLayout.addView(mapView)

                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val urlStr = "https://router.project-osrm.org/route/v1/foot/${startPoint.longitude},${startPoint.latitude};${destinationPoint.longitude},${destinationPoint.latitude}?geometries=geojson"
                        val connection = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
                        connection.setRequestProperty("User-Agent", "FestivalTrack WearOS App/1.0")
                        if (connection.responseCode == 200) {
                            val json = JSONObject(connection.inputStream.bufferedReader().readText())
                            val routes = json.getJSONArray("routes")
                            if (routes.length() > 0) {
                                val route = routes.getJSONObject(0)
                                val distance = route.getDouble("distance")
                                val duration = route.getDouble("duration")
                                
                                val distStr = if (distance > 1000) String.format("%.1f km", distance / 1000) else "${distance.toInt()} m"
                                val durStr = "${(duration / 60).toInt()} min"
                                
                                val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
                                val geoPoints = ArrayList<GeoPoint>()
                                for (i in 0 until coords.length()) {
                                    val c = coords.getJSONArray(i)
                                    geoPoints.add(GeoPoint(c.getDouble(1), c.getDouble(0)))
                                }
                                
                                withContext(Dispatchers.Main) {
                                    routeInfo = "$durStr • $distStr a pie"
                                    
                                    val polyline = Polyline(mapView)
                                    polyline.setPoints(geoPoints)
                                    polyline.outlinePaint.color = android.graphics.Color.parseColor("#C9A030")
                                    polyline.outlinePaint.strokeWidth = 15f
                                    mapView.overlays.add(polyline)

                                    val destMarker = Marker(mapView)
                                    destMarker.position = destinationPoint
                                    destMarker.icon = ctx.getDrawable(R.drawable.ic_logo_festival)
                                    destMarker.title = "Destino"
                                    mapView.overlays.add(destMarker)

                                    val startMarker = Marker(mapView)
                                    startMarker.position = startPoint
                                    val walkIcon = ctx.getDrawable(android.R.drawable.ic_menu_directions)
                                    if(walkIcon != null) {
                                        walkIcon.setTint(android.graphics.Color.BLUE)
                                        startMarker.icon = walkIcon
                                    }
                                    startMarker.title = "Tú"
                                    mapView.overlays.add(startMarker)

                                    val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                                    mapView.post {
                                        mapView.zoomToBoundingBox(boundingBox, true, 60)
                                    }

                                    mapView.invalidate()
                                }
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                
                frameLayout
            },
            modifier = Modifier.fillMaxSize()
        )

        // Botón Volver
        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(32.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0x991C1C1E))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White, modifier = Modifier.size(16.dp))
        }
        
        // Minimalist Distance Overlay
        if (routeInfo != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC1C1C1E))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = routeInfo!!,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

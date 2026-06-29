package mx.utng.festivaltrack.wear.presentation.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Composable
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import androidx.compose.runtime.LaunchedEffect
import mx.utng.festivaltrack.wear.data.local.FestivalDatabase
import mx.utng.festivaltrack.wear.data.local.entity.EventoEntity
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
                    imageVector = Icons.Filled.DirectionsWalk,
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
    var evento by remember { mutableStateOf<EventoEntity?>(null) }
    
    // Jardín Principal de Dolores Hidalgo como punto de inicio por defecto si falla el GPS
    var startPoint by remember { mutableStateOf(GeoPoint(21.156828, -100.934444)) }
    var locationFetched by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        startPoint = GeoPoint(location.latitude, location.longitude)
                    }
                    locationFetched = true
                }
            } catch (e: SecurityException) {
                locationFetched = true
            }
        } else {
            locationFetched = true // Continue with default location
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    startPoint = GeoPoint(location.latitude, location.longitude)
                }
                locationFetched = true
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(eventoId) {
        val dao = FestivalDatabase.getInstance(context).eventoDao()
        evento = dao.getEventoById(eventoId)
    }

    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        userAgentValue = "FestivalTrack/1.0"
        osmdroidBasePath = context.cacheDir
        osmdroidTileCache = java.io.File(context.cacheDir, "osmdroid")
    }
    
    val destinationPoint = if (evento?.latitud != null && evento?.longitud != null) {
        GeoPoint(evento!!.latitud!!, evento!!.longitud!!)
    } else {
        GeoPoint(21.156111, -100.932500)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE5E3DF))) {
        if (!locationFetched) {
            Text(
                text = "Buscando GPS...",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Black,
                fontSize = 12.sp
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    val mapView = MapView(ctx).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        maxZoomLevel = 19.0 // IMPORTANT: Fix for grey tiles on zoom > 19
                        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(16.0)
                        controller.setCenter(startPoint)
                    }

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

                                        // Apply expanded bounding box so it doesn't zoom too tightly
                                        val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                                        val expandedBox = BoundingBox(
                                            boundingBox.latNorth + 0.002,
                                            boundingBox.lonEast + 0.002,
                                            boundingBox.latSouth - 0.002,
                                            boundingBox.lonWest - 0.002
                                        )
                                        mapView.post {
                                            mapView.zoomToBoundingBox(expandedBox, true, 80)
                                        }

                                        mapView.invalidate()
                                    }
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    
                    mapView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Botón Volver
        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(32.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0x991C1C1E))
        ) {
            Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White, modifier = Modifier.size(16.dp))
        }
        
        // Minimalist Distance Overlay
        if (routeInfo != null && locationFetched) {
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

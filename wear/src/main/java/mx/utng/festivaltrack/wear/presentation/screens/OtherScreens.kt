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
    val context = androidx.compose.ui.platform.LocalContext.current
    var evento by remember { mutableStateOf<EventoEntity?>(null) }
    var tiempoFaltante by remember { mutableStateOf("Calculando...") }

    // Cargar el evento desde la base de datos
    LaunchedEffect(eventoId) {
        val dao = FestivalDatabase.getInstance(context).eventoDao()
        evento = dao.getEventoById(eventoId)
    }

    // Cronómetro en vivo
    LaunchedEffect(evento) {
        if (evento == null) return@LaunchedEffect
        while (true) {
            val currentInstant = java.time.Instant.now()
            val eventoInstant = try {
                java.time.Instant.parse(evento!!.fechaHora)
            } catch (e: Exception) {
                try {
                    java.time.LocalDateTime.parse(evento!!.fechaHora.replace("Z", "")).atZone(java.time.ZoneId.systemDefault()).toInstant()
                } catch (e2: Exception) {
                    currentInstant
                }
            }
            
            val duration = java.time.Duration.between(currentInstant, eventoInstant)
            tiempoFaltante = if (duration.isNegative || duration.isZero) {
                "¡AHORA EN CURSO!"
            } else {
                val dias = duration.toDays()
                val horas = duration.toHours() % 24
                val minutos = duration.toMinutes() % 60
                
                when {
                    dias > 0 -> "Faltan $dias días y $horas hr"
                    horas > 0 -> "Faltan $horas hr $minutos min"
                    else -> "Faltan $minutos min"
                }
            }
            kotlinx.coroutines.delay(10000) // Actualizar cada 10 segundos para mayor precisión
        }
    }

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
                text = if (tiempoFaltante.contains("AHORA") || tiempoFaltante.contains("min") && !tiempoFaltante.contains("hr")) "¡El evento está por comenzar!" else "Próximo Evento",
                color = FestivalTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = evento?.nombre ?: "Cargando evento...",
                color = FestivalGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = tiempoFaltante,
                color = FestivalTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
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

    // ESTADOS PARA DATOS
    // Inicializamos con el Jardín Principal para evitar que el mapa se quede esperando si el GPS del emulador falla
    var userLocation by remember { mutableStateOf(GeoPoint(21.156828, -100.934444)) }
    var routeGeoPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var routeInfo by remember { mutableStateOf<String?>(null) }
    var destinationPoint by remember { mutableStateOf(GeoPoint(21.156111, -100.932500)) }
    
    // Referencia al mapa para los botones de zoom de Compose
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // 1. Configuración obligatoria de OSMDroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
        }
    }

    // 2. Obtener Destino desde la BD
    LaunchedEffect(eventoId) {
        val dao = FestivalDatabase.getInstance(context).eventoDao()
        val evento = dao.getEventoById(eventoId)
        if (evento?.latitud != null && evento?.longitud != null) {
            destinationPoint = GeoPoint(evento.latitud!!, evento.longitud!!)
        }
    }

    // 3. Obtener GPS (Actualiza userLocation si tiene éxito)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            try {
                val token = com.google.android.gms.tasks.CancellationTokenSource().token
                fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) userLocation = GeoPoint(loc.latitude, loc.longitude)
                    }
            } catch (e: SecurityException) { /* Ignorar, usa el default */ }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val token = com.google.android.gms.tasks.CancellationTokenSource().token
            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, token)
                .addOnSuccessListener { loc ->
                    if (loc != null) userLocation = GeoPoint(loc.latitude, loc.longitude)
                }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // 4. Calcular Ruta observando tanto el origen como el destino
    LaunchedEffect(userLocation, destinationPoint) {
        kotlinx.coroutines.delay(1000) // 1 seg de gracia para que carguen los tiles del mapa
        withContext(Dispatchers.IO) {
            try {
                // overview=full obliga a OSRM a mandar todos los puntos exactos de las calles (mejor calibración visual)
                val urlStr = "https://router.project-osrm.org/route/v1/foot/${userLocation.longitude},${userLocation.latitude};${destinationPoint.longitude},${destinationPoint.latitude}?geometries=geojson&overview=full"
                val connection = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("User-Agent", "FestivalTrack WearOS App/1.0")
                if (connection.responseCode == 200) {
                    val json = JSONObject(connection.inputStream.bufferedReader().readText())
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val distance = route.getDouble("distance")
                        
                        // NOTA: El servidor gratuito de OSRM (demo) siempre devuelve tiempos de "vehículo"
                        // incluso si le pides "foot". Así que calculamos el tiempo real a pie usando
                        // la distancia exacta y una velocidad promedio humana de 5 km/h (83.3 metros por minuto).
                        val durMins = Math.ceil(distance / 83.3).toInt()
                        
                        val distStr = if (distance > 1000) String.format("%.1f km", distance / 1000) else "${distance.toInt()} m"
                        val durStr = if (durMins < 1) "< 1 min" else "$durMins min"
                        
                        val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
                        val points = mutableListOf<GeoPoint>()
                        for (i in 0 until coords.length()) {
                            val c = coords.getJSONArray(i)
                            points.add(GeoPoint(c.getDouble(1), c.getDouble(0)))
                        }
                        
                        withContext(Dispatchers.Main) {
                            routeInfo = "$durStr • $distStr a pie"
                            routeGeoPoints = points
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // INTERFAZ
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // Custom FrameLayout para evitar que el reloj intercepte los gestos (Swipe to Dismiss)
                val frameLayout = object : android.widget.FrameLayout(ctx) {
                    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
                        // Le dice a la pantalla (SwipeDismissableNavHost) que NO robe los toques
                        // Así puedes arrastrar el mapa libremente
                        parent.requestDisallowInterceptTouchEvent(true)
                        return super.dispatchTouchEvent(ev)
                    }
                }.apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                
                val mapView = MapView(ctx).apply {
                    // ESTO ES VITAL EN WEAR OS PARA QUE NO SE CORTE A LA MITAD LA PANTALLA
                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                    
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    
                    // Nivel de zoom seguro
                    maxZoomLevel = 17.5
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(21.156828, -100.934444)) // Dolores Hidalgo
                }
                
                // Agregamos el mapa al contenedor
                frameLayout.addView(mapView, android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                ))
                
                mapViewRef = mapView // Guardamos referencia para los botones de zoom
                frameLayout
            },
            update = { frameLayout ->
                val mapView = frameLayout.getChildAt(0) as MapView
                
                // Si tenemos GPS pero la ruta no llega, centrar al usuario mientras espera
                if (userLocation != null && routeGeoPoints.isEmpty()) {
                    mapView.controller.animateTo(userLocation)
                }

                // Si la ruta ya llegó, pintamos TODO (solo una vez)
                if (routeGeoPoints.isNotEmpty()) {
                    mapView.overlays.clear()

                    val polyline = Polyline(mapView)
                    polyline.setPoints(routeGeoPoints)
                    polyline.outlinePaint.color = android.graphics.Color.parseColor("#C9A030")
                    polyline.outlinePaint.strokeWidth = 15f
                    mapView.overlays.add(polyline)

                    val destMarker = Marker(mapView)
                    destMarker.position = destinationPoint
                    
                    // Usamos un icono por defecto de Android para que no cubra el mapa con una imagen gigante
                    val destIcon = context.getDrawable(android.R.drawable.ic_menu_myplaces)
                    if (destIcon != null) {
                        destIcon.setTint(android.graphics.Color.RED)
                        destMarker.icon = destIcon
                    }
                    
                    destMarker.title = "Destino"
                    mapView.overlays.add(destMarker)

                    val startMarker = Marker(mapView)
                    startMarker.position = userLocation
                    val walkIcon = context.getDrawable(android.R.drawable.ic_menu_directions)
                    if (walkIcon != null) {
                        walkIcon.setTint(android.graphics.Color.BLUE)
                        startMarker.icon = walkIcon
                    }
                    startMarker.title = "Tú"
                    mapView.overlays.add(startMarker)

                    // Zoom fijo seguro y centro a la mitad del camino
                    val midLat = (userLocation!!.latitude + destinationPoint.latitude) / 2
                    val midLon = (userLocation!!.longitude + destinationPoint.longitude) / 2
                    mapView.controller.setZoom(17.0)
                    mapView.controller.animateTo(GeoPoint(midLat, midLon))

                    mapView.invalidate()
                }
            }
        )

        // Botón Volver
        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(32.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0x99000000))
        ) {
            Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White, modifier = Modifier.size(16.dp))
        }

        // Botones de Zoom (Pequeños a la derecha)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { mapViewRef?.controller?.zoomIn() },
                modifier = Modifier.size(28.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xCC000000))
            ) {
                Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { mapViewRef?.controller?.zoomOut() },
                modifier = Modifier.size(28.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xCC000000))
            ) {
                Text("-", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        // Cartel de Distancia
        if (routeInfo != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
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

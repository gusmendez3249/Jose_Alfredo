package mx.utng.festivaltrack.app.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import mx.utng.festivaltrack.app.auto.Destino
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.DestinosState
import mx.utng.festivaltrack.app.ui.viewmodels.DestinosViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.RutaState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

private val Superficie = Color(0xEE1E2720)
private const val AZUL_USUARIO = 0xFF2196F3.toInt()

/** Acercamiento máximo al encuadrar una ruta corta (más cerca los mosaicos se pixelan). */
private const val ZOOM_MAXIMO_RUTA = 17.5

/**
 * Mapa del festival: tu ubicación real (GPS), los escenarios y sitios del festival con sus
 * coordenadas reales, y la ruta a pie por calles hasta el destino elegido, con distancia y tiempo.
 *
 * El destino elegido en "GPS del festival" llega preseleccionado.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit = {},
    onElegirDestino: () -> Unit = {},
    viewModel: DestinosViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val ruta by viewModel.ruta.collectAsState()
    var mapView by remember { mutableStateOf<MapView?>(null) }

    val permisos = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    ) { viewModel.cargar() }

    // Cada vez que se entra al mapa (también al volver de "GPS del festival").
    LaunchedEffect(Unit) {
        when {
            state is DestinosState.Success -> viewModel.seleccionarInicial()
            permisos.permissions.any { it.status.isGranted } -> viewModel.cargar()
            else -> permisos.launchMultiplePermissionRequest()
        }
    }

    val exito = state as? DestinosState.Success
    val destinoActual = when (val r = ruta) {
        is RutaState.Lista -> r.destino
        is RutaState.Calculando -> r.destino
        RutaState.SinDestino -> null
    }

    // Sigue tu ubicación mientras el mapa está abierto (y deja de hacerlo al salir, para ahorrar batería).
    val conPermiso = permisos.permissions.any { it.status.isGranted }
    DisposableEffect(conPermiso) {
        if (conPermiso) viewModel.seguirUbicacion()
        onDispose { viewModel.dejarDeSeguir() }
    }

    // Solo se encuadra la ruta cuando cambia el destino o llega su trazo, no cada vez que te mueves:
    // así el mapa no salta mientras caminas.
    var encuadreHecho by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(mapView, exito, ruta) {
        val mv = mapView ?: return@LaunchedEffect
        val clave = when (val r = ruta) {
            is RutaState.Lista -> "${r.destino.nombre}|${r.ruta.porCalles}"
            is RutaState.Calculando -> r.destino.nombre
            RutaState.SinDestino -> null
        }
        val encuadrar = clave != null && clave != encuadreHecho
        if (encuadrar) encuadreHecho = clave
        dibujar(mv, context, exito, ruta, encuadrar)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Mapa del festival", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onElegirDestino) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lista de destinos", tint = PrimaryGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                    Configuration.getInstance().userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)
                        controller.setCenter(GeoPoint(DestinosViewModel.CENTRO_DOLORES.first, DestinosViewModel.CENTRO_DOLORES.second))
                        mapView = this
                    }
                },
                onRelease = { it.onDetach() },
                modifier = Modifier.fillMaxSize()
            )

            // Selector de destinos y aviso de origen
            Column(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 10.dp)) {
                if (exito != null) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(exito.destinos, key = { it.nombre }) { destino ->
                            FilterChip(
                                selected = destino.nombre == destinoActual?.nombre,
                                onClick = { viewModel.trazarRuta(destino) },
                                label = { Text(destino.nombre, fontSize = 12.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGold,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xDD1E2720),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                    avisoOrigen(exito)?.let { aviso ->
                        Surface(
                            color = Superficie, shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(aviso, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }

            when (val s = state) {
                is DestinosState.Loading -> Surface(
                    color = Superficie, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Buscando tu ubicación…", color = Color.White)
                    }
                }
                is DestinosState.Error -> Surface(
                    color = Superficie, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.mensaje, color = Color.White, textAlign = TextAlign.Center)
                        TextButton(onClick = { viewModel.cargar() }) { Text("Intentar de nuevo", color = PrimaryGold) }
                    }
                }
                is DestinosState.Success -> TarjetaRuta(
                    ruta = ruta,
                    onCentrar = { mapView?.let { dibujar(it, context, exito, ruta, encuadrar = true) } },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun TarjetaRuta(ruta: RutaState, onCentrar: () -> Unit, modifier: Modifier = Modifier) {
    val destino: Destino = when (ruta) {
        is RutaState.Lista -> ruta.destino
        is RutaState.Calculando -> ruta.destino
        RutaState.SinDestino -> return
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Superficie),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().padding(16.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(PrimaryGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = Color.Black)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(destino.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(destino.subtitulo, color = Color.LightGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                when (ruta) {
                    is RutaState.Lista -> {
                        Text(ruta.ruta.resumen, color = PrimaryGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (!ruta.ruta.porCalles) {
                            Text("Sin servicio de rutas: distancia en línea recta", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                    else -> Text("Calculando ruta…", color = PrimaryGold, fontSize = 13.sp)
                }
            }
            FloatingActionButton(
                onClick = onCentrar,
                containerColor = PrimaryGold,
                contentColor = Color.Black,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Centrar ruta")
            }
        }
    }
}

/** Pinta tu ubicación (con su margen de error), los destinos y la ruta; si [encuadrar], encuadra la ruta. */
private fun dibujar(mapView: MapView, context: Context, state: DestinosState.Success?, ruta: RutaState, encuadrar: Boolean) {
    mapView.overlays.clear()
    if (state == null) {
        mapView.invalidate()
        return
    }
    val seleccionado = (ruta as? RutaState.Lista)?.destino ?: (ruta as? RutaState.Calculando)?.destino

    state.destinos.forEach { destino ->
        val elegido = destino.nombre == seleccionado?.nombre
        mapView.overlays.add(Marker(mapView).apply {
            position = GeoPoint(destino.latitud, destino.longitud)
            title = destino.nombre
            snippet = destino.subtitulo
            icon = pin(context, if (elegido) 0xFFE6C27A.toInt() else 0xFF7A8F7E.toInt())
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        })
    }

    val origen = GeoPoint(state.origenLat, state.origenLon)
    if (ruta is RutaState.Lista) {
        mapView.overlays.add(0, Polyline(mapView).apply {
            setPoints(ruta.ruta.puntos.map { (lat, lon) -> GeoPoint(lat, lon) })
            outlinePaint.color = 0xFFE6C27A.toInt()
            outlinePaint.strokeWidth = 14f
        })
    }
    // Círculo con el margen de error del GPS: muestra qué tan exacta es tu posición.
    state.precisionM?.takeIf { it > 0 }?.let { radio ->
        mapView.overlays.add(Polygon(mapView).apply {
            points = Polygon.pointsAsCircle(origen, radio.toDouble())
            fillPaint.color = 0x332196F3
            outlinePaint.color = 0x882196F3.toInt()
            outlinePaint.strokeWidth = 2f
            setOnClickListener { _, _, _ -> false }
        })
    }
    mapView.overlays.add(Marker(mapView).apply {
        position = origen
        title = if (state.ubicacionAproximada) "Jardín Principal (punto de partida)" else "Tú estás aquí"
        icon = puntoUsuario()
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    })

    val puntos = (ruta as? RutaState.Lista)?.ruta?.puntos?.map { (lat, lon) -> GeoPoint(lat, lon) }
        ?: listOfNotNull(origen, seleccionado?.let { GeoPoint(it.latitud, it.longitud) })
    if (!encuadrar) {
        mapView.invalidate()
        return
    }
    // Encuadrar necesita el tamaño del mapa: si aún no se mide (al volver a la pestaña), espera.
    if (mapView.width == 0 || mapView.height == 0) {
        mapView.addOnFirstLayoutListener { _, _, _, _, _ -> encuadrar(mapView, puntos, origen) }
    } else {
        mapView.post { encuadrar(mapView, puntos, origen) }
    }
    mapView.invalidate()
}

/** Encuadra la ruta completa (o centra en el origen si aún no hay ruta). */
private fun encuadrar(mapView: MapView, puntos: List<GeoPoint>, origen: GeoPoint) {
    run {
        if (puntos.size >= 2) {
            // El margen se aplica a los cuatro lados: se calcula sobre el lado corto de la pantalla.
            val margen = (minOf(mapView.width, mapView.height) * 0.15).toInt().coerceIn(40, 200)
            mapView.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(puntos), false, margen)
            if (mapView.zoomLevelDouble > ZOOM_MAXIMO_RUTA) mapView.controller.setZoom(ZOOM_MAXIMO_RUTA)
            // Baja un poco el centro para que la ruta no quede bajo la tarjeta inferior.
            val centro = mapView.mapCenter
            val alto = mapView.boundingBox.latitudeSpan
            mapView.controller.animateTo(GeoPoint(centro.latitude - alto * 0.12, centro.longitude))
        } else {
            mapView.controller.animateTo(origen)
        }
    }
}

private fun pin(context: Context, color: Int): Drawable? =
    ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)?.mutate()?.apply { setTint(color) }

private fun puntoUsuario(): Drawable = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(AZUL_USUARIO)
    setStroke(6, android.graphics.Color.WHITE)
    setSize(48, 48)
}

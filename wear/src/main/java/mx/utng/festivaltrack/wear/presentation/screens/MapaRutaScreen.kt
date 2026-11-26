package mx.utng.festivaltrack.wear.presentation.screens

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.utng.festivaltrack.wear.presentation.util.AgendaWear
import mx.utng.festivaltrack.wear.presentation.util.Posicion
import mx.utng.festivaltrack.wear.presentation.util.rememberUbicacionEnVivo
import mx.utng.jose_alfredo.presentation.theme.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

private const val ORO = 0xFFE6C27A.toInt()
private const val ORO_OSCURO = 0xFF6B5218.toInt()
private const val AZUL = 0xFF4FA3FF.toInt()

/** Ruta a pie por calles. */
private data class RutaWear(val puntos: List<GeoPoint>, val metros: Double)

/** Ruta a pie del servicio peatonal de OpenStreetMap; null sin conexión. */
private suspend fun pedirRutaAPie(desde: Posicion, hasta: Pair<Double, Double>): RutaWear? = withContext(Dispatchers.IO) {
    runCatching {
        val url = "https://routing.openstreetmap.de/routed-foot/route/v1/foot/" +
            "${desde.longitud},${desde.latitud};${hasta.second},${hasta.first}?overview=full&geometries=geojson"
        val conexion = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conexion.connectTimeout = 6000
        conexion.readTimeout = 8000
        conexion.setRequestProperty("User-Agent", "FestivalTrack-Wear")
        try {
            if (conexion.responseCode != 200) return@runCatching null
            val json = JSONObject(conexion.inputStream.bufferedReader().use { it.readText() })
            val ruta = json.getJSONArray("routes").takeIf { it.length() > 0 }?.getJSONObject(0) ?: return@runCatching null
            val coords = ruta.getJSONObject("geometry").getJSONArray("coordinates")
            val puntos = (0 until coords.length()).map { i -> coords.getJSONArray(i).let { GeoPoint(it.getDouble(1), it.getDouble(0)) } }
            RutaWear(puntos, ruta.getDouble("distance"))
        } finally {
            conexion.disconnect()
        }
    }.getOrNull()
}

/**
 * Filtro para los mosaicos de OpenStreetMap: escala de grises invertida y oscurecida.
 * El mapa queda oscuro (como el resto del reloj), gasta menos batería en pantallas OLED y
 * la ruta dorada resalta.
 */
private val FILTRO_MAPA_OSCURO = ColorMatrixColorFilter(
    ColorMatrix().apply {
        setSaturation(0f)
        postConcat(ColorMatrix(floatArrayOf(
            -0.78f, 0f, 0f, 0f, 225f,
            0f, -0.78f, 0f, 0f, 225f,
            0f, 0f, -0.82f, 0f, 235f,
            0f, 0f, 0f, 1f, 0f
        )))
    }
)

/** Línea suave con extremos y uniones redondeadas. */
private fun Polyline.estilo(color: Int, ancho: Float) = apply {
    outlinePaint.apply {
        this.color = color
        strokeWidth = ancho
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
}

/** Punto con aro blanco y sombra oscura, para que se distinga sobre cualquier fondo. */
private fun punto(color: Int, tam: Int): LayerDrawable {
    val sombra = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0x66000000); setSize(tam + 8, tam + 8) }
    val relleno = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color); setStroke(5, android.graphics.Color.WHITE); setSize(tam, tam) }
    return LayerDrawable(arrayOf(sombra, relleno)).apply { setLayerInset(1, 4, 4, 4, 4) }
}

/**
 * Mapa con la ruta a pie hasta el concierto, pensado para la pantalla del reloj:
 * tu posición en vivo (punto azul con su margen de error), el escenario (punto dorado) y la
 * ruta por calles. Se acerca o aleja con la corona (o pellizcando) y el botón de la derecha
 * vuelve a encuadrar el recorrido. La ruta se recalcula al avanzar ~25 m.
 */
@Composable
fun MapaRutaScreen(eventoId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val evento = rememberEvento(eventoId)
    val ubicacion = rememberUbicacionEnVivo()
    val pos = ubicacion.posicion
    val destino = evento?.let { e -> if (e.latitud != null && e.longitud != null) e.latitud!! to e.longitud!! else null }

    var ruta by remember { mutableStateOf<RutaWear?>(null) }
    var origenRuta by remember { mutableStateOf<Posicion?>(null) }
    var encuadrar by remember { mutableStateOf(true) }
    var mapa by remember { mutableStateOf<MapView?>(null) }
    val foco = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
        }
        foco.requestFocus()
    }

    LaunchedEffect(pos, destino) {
        if (pos == null || destino == null) return@LaunchedEffect
        val previo = origenRuta
        if (previo != null && AgendaWear.distanciaM(previo.latitud, previo.longitud, pos.latitud, pos.longitud) < 25) return@LaunchedEffect
        origenRuta = pos
        pedirRutaAPie(pos, destino)?.let {
            if (ruta == null) encuadrar = true // al llegar la primera ruta se muestra completa
            ruta = it
        }
    }

    val restantes = if (pos != null && destino != null) AgendaWear.distanciaM(pos.latitud, pos.longitud, destino.first, destino.second) else null
    val llegaste = restantes != null && restantes < AgendaWear.LLEGADA_M
    val detalle = when {
        destino == null && evento != null -> "Sin ubicación del escenario"
        !ubicacion.conPermiso -> "Activa tu ubicación"
        pos == null -> "Buscando tu ubicación…"
        llegaste -> "¡Ya llegaste!"
        ruta != null -> "${AgendaWear.textoDistancia(ruta!!.metros)} · ${AgendaWear.minutosCaminando(ruta!!.metros)} min a pie"
        restantes != null -> "${AgendaWear.textoDistancia(restantes)} · calculando ruta…"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101012))
            // Corona del reloj: girar acerca o aleja el mapa.
            .onRotaryScrollEvent { e ->
                mapa?.controller?.let { if (e.verticalScrollPixels > 0) it.zoomIn() else it.zoomOut() }
                true
            }
            .focusRequester(foco)
            .focusable()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // Contenedor que evita que "deslizar para atrás" robe el arrastre del mapa.
                val contenedor = object : android.widget.FrameLayout(ctx) {
                    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
                        parent.requestDisallowInterceptTouchEvent(true)
                        return super.dispatchTouchEvent(ev)
                    }
                }
                val mapView = MapView(ctx).apply {
                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                    setTileSource(TileSourceFactory.MAPNIK)
                    overlayManager.tilesOverlay.setColorFilter(FILTRO_MAPA_OSCURO)
                    overlayManager.tilesOverlay.loadingBackgroundColor = android.graphics.Color.rgb(28, 28, 30)
                    overlayManager.tilesOverlay.loadingLineColor = android.graphics.Color.rgb(40, 40, 44)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    minZoomLevel = 13.0
                    maxZoomLevel = 18.5
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(AgendaWear.JARDIN.first, AgendaWear.JARDIN.second))
                }
                contenedor.addView(mapView, android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT
                ))
                mapa = mapView
                contenedor
            },
            update = { _ ->
                val mv = mapa ?: return@AndroidView
                mv.overlays.clear()
                ruta?.let { r ->
                    // El servicio de rutas "pega" el origen y el destino a la calle caminable más
                    // cercana (dentro de una plaza o un panteón no hay calles). Una línea punteada une
                    // tu posición con el inicio de la ruta y su final con el escenario, para que la
                    // ruta salga de tu punto y llegue al destino.
                    pos?.let { p -> conectorPunteado(mv, GeoPoint(p.latitud, p.longitud), r.puntos.first()) }
                    destino?.let { (lat, lon) -> conectorPunteado(mv, r.puntos.last(), GeoPoint(lat, lon)) }
                    // Borde oscuro debajo y línea dorada encima: la ruta se lee limpia sobre el mapa.
                    mv.overlays.add(Polyline(mv).apply { setPoints(r.puntos) }.estilo(ORO_OSCURO, 16f))
                    mv.overlays.add(Polyline(mv).apply { setPoints(r.puntos) }.estilo(ORO, 9f))
                }
                pos?.precisionM?.takeIf { it in 5f..300f }?.let { radio ->
                    mv.overlays.add(Polygon(mv).apply {
                        points = Polygon.pointsAsCircle(GeoPoint(pos.latitud, pos.longitud), radio.toDouble())
                        fillPaint.color = 0x334FA3FF
                        outlinePaint.color = 0x664FA3FF
                        outlinePaint.strokeWidth = 2f
                        outlinePaint.isAntiAlias = true
                        setOnClickListener { _, _, _ -> false }
                    })
                }
                destino?.let { (lat, lon) ->
                    mv.overlays.add(Marker(mv).apply {
                        position = GeoPoint(lat, lon)
                        icon = punto(ORO, 34)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setInfoWindow(null)
                    })
                }
                pos?.let { p ->
                    mv.overlays.add(Marker(mv).apply {
                        position = GeoPoint(p.latitud, p.longitud)
                        icon = punto(AZUL, 28)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setInfoWindow(null)
                    })
                }
                if (encuadrar && mv.width > 0) {
                    // Se encuadran tu posición, la ruta y el escenario (todo lo que se dibuja).
                    val puntos = (ruta?.puntos.orEmpty() + listOfNotNull(
                        pos?.let { GeoPoint(it.latitud, it.longitud) },
                        destino?.let { GeoPoint(it.first, it.second) }
                    ))
                    when {
                        puntos.size >= 2 -> {
                            // Margen amplio: deja libres la orilla redonda, los botones de la
                            // derecha y el cartel de abajo, para que nada tape la ruta.
                            mv.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(puntos).increaseByScale(1.8f), false, 24)
                            // Recorre el encuadre un poco a la izquierda y hacia arriba: a la derecha
                            // están los botones y abajo el cartel.
                            val caja = mv.boundingBox
                            val centro = mv.mapCenter
                            mv.controller.setCenter(GeoPoint(
                                centro.latitude - caja.latitudeSpan * 0.08,
                                centro.longitude + caja.longitudeSpanWithDateLine * 0.08
                            ))
                            if (ruta != null) encuadrar = false
                        }
                        puntos.size == 1 -> mv.controller.setCenter(puntos.first())
                    }
                }
                mv.invalidate()
            }
        )

        // Volver (arriba a la izquierda, dentro del área segura de una pantalla redonda).
        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 30.dp, top = 30.dp).size(34.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xCC1C1C1E))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White, modifier = Modifier.size(18.dp))
        }
        // Centrar la ruta: arriba al centro (libre en pantallas redondas y cuadradas).
        Button(
            onClick = { encuadrar = true; mapa?.invalidate() },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp).size(34.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xCC1C1C1E))
        ) {
            Icon(Icons.Filled.MyLocation, "Centrar ruta", tint = FestivalGold, modifier = Modifier.size(18.dp))
        }
        // Acercar y alejar: a la derecha, un poco arriba del centro para no chocar con el cartel de abajo.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = (-14).dp)
                .padding(end = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xCC1C1C1E))
        ) {
            // Acercar centrado en ti: así ves tu calle y hacia dónde ir, en lugar de un punto cualquiera.
            BotonMapa(onClick = {
                val mv = mapa
                if (mv != null && pos != null) {
                    mv.controller.animateTo(GeoPoint(pos.latitud, pos.longitud), (mv.zoomLevelDouble + 1).coerceAtMost(mv.maxZoomLevel), 300L)
                } else {
                    mv?.controller?.zoomIn()
                }
            }) {
                Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.width(18.dp).height(1.dp).background(Color(0x55FFFFFF)))
            BotonMapa(onClick = { mapa?.controller?.zoomOut() }) {
                Text("−", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Cartel: a dónde vas y cuánto falta.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp)
                .widthIn(max = 220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xE61C1C1E))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                evento?.let { AgendaWear.lugar(it) } ?: "",
                color = FestivalTextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (llegaste) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                }
                Text(detalle, color = if (llegaste) FestivalGold else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Botón circular pequeño de los controles del mapa (área táctil de 36 dp). */
@Composable
private fun BotonMapa(onClick: () -> Unit, contenido: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) { contenido() }
}

/** Tramo punteado entre dos puntos (si están a más de 5 m), para unir tu posición o el escenario con la ruta. */
private fun conectorPunteado(mv: MapView, desde: GeoPoint, hasta: GeoPoint) {
    if (desde.distanceToAsDouble(hasta) < 5) return
    mv.overlays.add(Polyline(mv).apply {
        setPoints(listOf(desde, hasta))
        outlinePaint.apply {
            color = ORO
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(2f, 14f), 0f)
        }
    })
}

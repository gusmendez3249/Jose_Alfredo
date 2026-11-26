package mx.utng.festivaltrack.tv.presentation.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.SitioArDto
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.presentation.components.TvAviso
import mx.utng.festivaltrack.tv.ui.theme.*
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Una sede del festival en el mapa, con su próximo concierto si tiene. */
data class SedeTv(val nombre: String, val detalle: String, val lat: Double, val lon: Double, val proximo: EventoEntity?)

private fun distanciaKm(a: Double, b: Double, c: Double, d: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(c - a)
    val dLon = Math.toRadians(d - b)
    val h = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(a)) * cos(Math.toRadians(c)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * atan2(sqrt(h), sqrt(1 - h))
}

/**
 * Sedes con sus coordenadas reales: un lugar por escenario de los próximos conciertos (con el
 * más cercano en el tiempo) y los sitios del catálogo que no coinciden con uno ya listado
 * (a menos de 50 m se toma como el mismo lugar). Mismas reglas que el móvil.
 */
fun construirSedes(proximos: List<EventoEntity>, sitios: List<SitioArDto>): List<SedeTv> {
    val sedes = mutableListOf<SedeTv>()
    proximos.filter { it.latitud != null && it.longitud != null }
        .groupBy { it.escenario ?: it.ubicacion }
        .forEach { (lugar, eventos) ->
            val e = eventos.first()
            sedes += SedeTv(lugar, e.ubicacion, e.latitud!!, e.longitud!!, e)
        }
    sitios.forEach { s ->
        val lat = s.latitud ?: return@forEach
        val lon = s.longitud ?: return@forEach
        if (sedes.none { distanciaKm(it.lat, it.lon, lat, lon) < 0.05 }) sedes += SedeTv(s.nombre, s.ubicacion, lat, lon, null)
    }
    return sedes
}

/** Ruta a pie en el mapa del celular (Google Maps abre la app de mapas del teléfono). */
fun urlRuta(sede: SedeTv) = "https://www.google.com/maps/dir/?api=1&destination=${sede.lat},${sede.lon}&travelmode=walking"

/**
 * Mapa del festival en la TV: las sedes reales con su próximo concierto. La TV no se mueve, así
 * que en lugar de la ruta con GPS se muestra un QR que abre la ruta a pie en el celular.
 */
@Composable
fun TvMapaScreen(proximos: List<EventoEntity>, sitios: List<SitioArDto>, ahora: Instant) {
    val sedes = remember(proximos, sitios) { construirSedes(proximos, sitios) }
    var elegida by remember(sedes) { mutableStateOf(sedes.firstOrNull()) }

    if (sedes.isEmpty()) {
        TvAviso(Icons.Default.Place, "Cargando las sedes del festival…", "Si no aparecen, revisa la conexión con el servidor.", cargando = true)
        return
    }

    Row(Modifier.fillMaxSize().background(FestivalDarkBg).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        // Lista de sedes (se maneja con el control)
        Column(Modifier.width(250.dp)) {
            Text("Mapa del festival", color = FestivalGold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Elige una sede con el control", color = FestivalTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(sedes, key = { it.nombre }) { sede ->
                    var enfocada by remember { mutableStateOf(false) }
                    val esElegida = sede == elegida
                    Surface(
                        onClick = { elegida = sede },
                        color = when { esElegida -> Color(0xFF2C2214); enfocada -> Color(0xFF2E3D30); else -> FestivalCardBg },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { enfocada = it.isFocused; if (it.isFocused) elegida = sede }
                            .border(if (enfocada || esElegida) 2.dp else 0.dp, if (enfocada || esElegida) FestivalGold else Color.Transparent, RoundedCornerShape(10.dp))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(sede.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                sede.proximo?.let { "${it.nombre} · ${HorarioFestival.cuando(it, ahora)}" } ?: sede.detalle,
                                color = FestivalTextSecondary, fontSize = 11.sp, maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // Mapa grande y, debajo, la sede elegida con el QR para la ruta a pie
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
                MapaSedes(sedes = sedes, elegida = elegida)
            }
            elegida?.let { sede ->
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(sede.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(sede.detalle, color = FestivalTextSecondary, fontSize = 11.sp)
                        sede.proximo?.let {
                            Text("Próximo: ${it.nombre} · ${HorarioFestival.cuando(it, ahora)}", color = FestivalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Escanea el código para ver la ruta a pie en tu celular", color = FestivalTextSecondary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    DynamicQrCode(content = urlRuta(sede), modifier = Modifier.size(120.dp))
                }
            }
        }
    }
}

@Composable
private fun MapaSedes(sedes: List<SedeTv>, elegida: SedeTv?) {
    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(false)
                // El mapa no toma el foco: se maneja desde la lista de sedes con el control.
                isFocusable = false
                controller.setZoom(16.0)
            }
        },
        update = { mapa ->
            mapa.overlays.clear()
            sedes.forEach { sede ->
                mapa.overlays.add(Marker(mapa).apply {
                    position = GeoPoint(sede.lat, sede.lon)
                    title = sede.nombre
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    if (sede == elegida) showInfoWindow()
                })
            }
            elegida?.let { mapa.controller.animateTo(GeoPoint(it.lat, it.lon)) }
            mapa.invalidate()
        },
        modifier = Modifier.fillMaxSize()
    )
}

package mx.utng.festivaltrack.tv.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.tv.R
import java.text.Normalizer

/** Fotos del festival incluidas en la app para los conciertos que no tienen imagen propia. */
enum class FondoConcierto(@DrawableRes val imagen: Int) {
    MARIACHI(R.drawable.mariachi_gala_stage),
    EL_REY(R.drawable.gal_el_rey_escenario),
    BOHEMIA(R.drawable.gal_servilleta_cantina),
    PLAZA(R.drawable.gal_plaza_dolores),
    MAUSOLEO(R.drawable.gal_mausoleo_sarape),
    CASA_MUSEO(R.drawable.gal_casa_museo),
    GUITARRA(R.drawable.ranchera_guitar),
    FESTIVAL(R.drawable.gal_banner_festival)
}

private fun normalizar(texto: String?): String =
    Normalizer.normalize(texto.orEmpty(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()

private fun contiene(texto: String, vararg palabras: String) =
    palabras.any { Regex("\\b$it\\b").containsMatchIn(texto) }

/**
 * Elige la foto local de un concierto: primero por el tipo de concierto que dice su nombre
 * (homenaje a "El Rey", mariachi, bohemia) y luego por el lugar (Mausoleo, Casa Museo, Plaza,
 * Escenario Principal). Si nada coincide, una de las fotos generales, siempre la misma para el
 * mismo concierto (según su id), para que las tarjetas no se vean todas iguales.
 */
fun fondoDe(id: String, nombre: String, escenario: String?, ubicacion: String?): FondoConcierto {
    val n = normalizar(nombre)
    when {
        contiene(n, "rey") -> return FondoConcierto.EL_REY
        contiene(n, "mariachi", "mariachis", "gala") -> return FondoConcierto.MARIACHI
        contiene(n, "bohemia", "cantina") -> return FondoConcierto.BOHEMIA
    }
    val lugar = normalizar(listOfNotNull(escenario, ubicacion, nombre).joinToString(" "))
    return when {
        contiene(lugar, "mausoleo", "panteon") -> FondoConcierto.MAUSOLEO
        lugar.contains("casa museo") -> FondoConcierto.CASA_MUSEO
        contiene(lugar, "plaza", "jardin") -> FondoConcierto.PLAZA
        contiene(lugar, "escenario") -> FondoConcierto.MARIACHI
        else -> GENERALES[Math.floorMod(id.hashCode(), GENERALES.size)]
    }
}

private val GENERALES = listOf(FondoConcierto.FESTIVAL, FondoConcierto.GUITARRA, FondoConcierto.PLAZA)

/** URL completa de la imagen del concierto, o null si no tiene (las rutas `/uploads/...` son del backend). */
fun urlBanner(bannerUrl: String?, servidor: String): String? {
    val ruta = bannerUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (ruta.startsWith("/")) servidor.removeSuffix("/") + ruta else ruta
}

/**
 * Imagen de un concierto: su `bannerUrl` si el administrador le puso una; mientras carga, si
 * falla o si no tiene, la foto local de [fondoDe].
 */
@Composable
fun PortadaConcierto(evento: EventoEntity, modifier: Modifier = Modifier) {
    val local = painterResource(fondoDe(evento.id, evento.nombre, evento.escenario, evento.ubicacion).imagen)
    AsyncImage(
        model = urlBanner(evento.bannerUrl, FestivalApiService.SERVER_URL),
        contentDescription = evento.nombre,
        placeholder = local,
        error = local,
        fallback = local,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

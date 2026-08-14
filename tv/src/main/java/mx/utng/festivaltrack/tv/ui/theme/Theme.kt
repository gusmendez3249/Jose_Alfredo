package mx.utng.festivaltrack.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema de colores oscuros personalizado para el módulo de Android TV.
 */
private val DarkColorScheme = darkColorScheme(
    primary = FestivalGold,
    secondary = FestivalGoldDark,
    background = FestivalDarkBg,
    surface = FestivalCardBg,
    onPrimary = FestivalDarkBg,
    onBackground = FestivalTextPrimary,
    onSurface = FestivalTextPrimary
)

/**
 * Tema principal para la aplicación de Android TV de FestivalTrack.
 *
 * @param content El contenido [Composable] que será renderizado usando este tema.
 */
@Composable
fun FestivalTrackTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

package mx.utng.festivaltrack.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = FestivalGold,
    secondary = FestivalGoldDark,
    background = FestivalDarkBg,
    surface = FestivalCardBg,
    onPrimary = FestivalDarkBg,
    onBackground = FestivalTextPrimary,
    onSurface = FestivalTextPrimary
)

@Composable
fun FestivalTrackTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

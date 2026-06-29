package mx.utng.jose_alfredo.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

val FestivalColorScheme = ColorScheme(
    primary = FestivalGold,
    primaryDim = FestivalGold,
    onPrimary = FestivalTextOnGold,
    secondary = FestivalGold,
    onSecondary = FestivalTextOnGold,
    background = FestivalBackground,
    onBackground = FestivalTextPrimary
)

@Composable
fun Jose_AlfredoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FestivalColorScheme,
        content = content
    )
}
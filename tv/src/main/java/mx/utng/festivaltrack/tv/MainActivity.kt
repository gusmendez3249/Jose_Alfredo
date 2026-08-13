package mx.utng.festivaltrack.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import mx.utng.festivaltrack.tv.presentation.screens.TvGalleryScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvLiveStreamScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvLoginScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvMainScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvScheduleScreen
import mx.utng.festivaltrack.tv.presentation.viewmodel.TvViewModel
import mx.utng.festivaltrack.tv.ui.theme.FestivalDarkBg
import mx.utng.festivaltrack.tv.ui.theme.FestivalTrackTvTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FestivalTrackTvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FestivalDarkBg
                ) {
                    val eventos by viewModel.eventos.collectAsState()
                    var currentScreenIndex by remember { mutableStateOf(0) }
                    var isLoggedIn by remember { mutableStateOf(false) }

                    if (!isLoggedIn) {
                        TvLoginScreen(
                            onLoginSuccess = { isLoggedIn = true }
                        )
                    } else {
                        when (currentScreenIndex) {
                            0 -> TvMainScreen(
                                eventos = eventos,
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it },
                                onVerEnVivo = { currentScreenIndex = 2 },
                                onComprarBoletos = { /* Open ticket dialog */ }
                            )
                            1 -> TvGalleryScreen(
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it }
                            )
                            2 -> TvLiveStreamScreen(
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it }
                            )
                            3 -> TvScheduleScreen(
                                eventos = eventos,
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it }
                            )
                            4 -> mx.utng.festivaltrack.tv.presentation.screens.TvSettingsScreen(
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it },
                                onLogout = { isLoggedIn = false }
                            )
                            else -> TvMainScreen(
                                eventos = eventos,
                                currentNavIndex = currentScreenIndex,
                                onNavSelect = { currentScreenIndex = it },
                                onVerEnVivo = { currentScreenIndex = 2 },
                                onComprarBoletos = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

package mx.utng.jose_alfredo.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material3.MaterialTheme
import mx.utng.festivaltrack.wear.presentation.navigation.WearNavGraph
import mx.utng.jose_alfredo.presentation.theme.Jose_AlfredoTheme

/**
 * Actividad única del reloj. El logo del festival se muestra con el splash del sistema
 * (sin pantallas de espera extra) y se entra directo a la agenda.
 * Si se abre desde un aviso de concierto, va directo a ese concierto.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val conciertoInicial = intent?.getStringExtra(EXTRA_EVENTO_ID)
        setContent {
            Jose_AlfredoTheme {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    WearNavGraph(conciertoInicial = conciertoInicial)
                }
            }
        }
    }

    companion object {
        /** Extra con el id del concierto a abrir (lo pone el aviso de 15 minutos). */
        const val EXTRA_EVENTO_ID = "eventoId"
    }
}

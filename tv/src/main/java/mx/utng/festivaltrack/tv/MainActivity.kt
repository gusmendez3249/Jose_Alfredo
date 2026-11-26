package mx.utng.festivaltrack.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import mx.utng.festivaltrack.tv.presentation.components.SeccionTv
import mx.utng.festivaltrack.tv.presentation.components.TvComprarBoletosDialog
import mx.utng.festivaltrack.tv.presentation.components.TvSidebar
import mx.utng.festivaltrack.tv.presentation.screens.TvGalleryScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvLiveStreamScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvLoginScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvMainScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvScheduleScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvSettingsScreen
import mx.utng.festivaltrack.tv.presentation.viewmodel.TvViewModel
import mx.utng.festivaltrack.tv.ui.theme.FestivalDarkBg
import mx.utng.festivaltrack.tv.ui.theme.FestivalTrackTvTheme

/**
 * Punto de entrada de la app de Android TV.
 *
 * Al abrir se muestra el inicio de sesión (con opción de seguir como invitado). Después, un
 * menú lateral único y la sección elegida. "Comprar boletos" abre, desde cualquier sección, el
 * QR para pagar en el celular. El botón Atrás del control regresa al Inicio antes de salir.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FestivalTrackTvTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = FestivalDarkBg) {
                    val proximos by viewModel.proximos.collectAsState()
                    val stream by viewModel.stream.collectAsState()
                    val ahora by viewModel.ahora.collectAsState()
                    val sesion by viewModel.sesion.collectAsState()
                    val galeria by viewModel.galeria.collectAsState()
                    val servidorConectado by viewModel.servidorConectado.collectAsState()

                    var mostrarLogin by rememberSaveable { mutableStateOf(true) }
                    var yaEntro by rememberSaveable { mutableStateOf(false) }
                    var seccion by rememberSaveable { mutableStateOf(SeccionTv.INICIO) }
                    // Compra con QR: null = cerrada; "" = sin concierto preseleccionado.
                    var comprar by remember { mutableStateOf<String?>(null) }

                    if (mostrarLogin) {
                        // Si se abrió desde dentro (chat o Ajustes), Atrás regresa a donde estaba.
                        BackHandler(enabled = yaEntro) { mostrarLogin = false }
                        TvLoginScreen(
                            iniciarSesion = viewModel::iniciarSesion,
                            onEntrar = { mostrarLogin = false; yaEntro = true },
                            onContinuarSinCuenta = { mostrarLogin = false; yaEntro = true }
                        )
                        return@Surface
                    }

                    BackHandler(enabled = seccion != SeccionTv.INICIO) { seccion = SeccionTv.INICIO }

                    Row(Modifier.fillMaxSize()) {
                        TvSidebar(
                            seccion = seccion,
                            onSeccion = { seccion = it },
                            enVivo = stream?.isLive == true,
                            usuario = sesion?.nombre,
                            onComprarBoletos = { comprar = "" }
                        )
                        Box(Modifier.weight(1f)) {
                            when (seccion) {
                                SeccionTv.INICIO -> TvMainScreen(
                                    proximos = proximos,
                                    stream = stream,
                                    ahora = ahora,
                                    usuario = sesion?.nombre,
                                    onVerEnVivo = { seccion = SeccionTv.EN_VIVO },
                                    onVerProgramacion = { seccion = SeccionTv.PROGRAMACION },
                                    onComprar = { comprar = it ?: "" }
                                )
                                SeccionTv.GALERIA -> TvGalleryScreen(estado = galeria, onReintentar = viewModel::cargarGaleria)
                                SeccionTv.EN_VIVO -> TvLiveStreamScreen(
                                    stream = stream,
                                    proximos = proximos,
                                    ahora = ahora,
                                    sesion = sesion,
                                    onIniciarSesion = { mostrarLogin = true },
                                    onVerProgramacion = { seccion = SeccionTv.PROGRAMACION }
                                )
                                SeccionTv.PROGRAMACION -> TvScheduleScreen(
                                    proximos = proximos,
                                    stream = stream,
                                    ahora = ahora,
                                    onComprar = { comprar = it },
                                    onVerEnVivo = { seccion = SeccionTv.EN_VIVO }
                                )
                                SeccionTv.AJUSTES -> TvSettingsScreen(
                                    servidorConectado = servidorConectado,
                                    stream = stream,
                                    sesion = sesion,
                                    onIniciarSesion = { mostrarLogin = true },
                                    onCerrarSesion = viewModel::cerrarSesion
                                )
                            }
                        }
                    }

                    comprar?.let { preseleccionado ->
                        TvComprarBoletosDialog(
                            conciertos = proximos,
                            ahora = ahora,
                            preseleccionado = preseleccionado.ifEmpty { null },
                            onCerrar = { comprar = null }
                        )
                    }
                }
            }
        }
    }
}

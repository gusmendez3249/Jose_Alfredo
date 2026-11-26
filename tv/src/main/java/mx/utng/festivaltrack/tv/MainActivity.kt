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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import mx.utng.festivaltrack.tv.presentation.components.SeccionTv
import mx.utng.festivaltrack.tv.presentation.components.TvBarraMusica
import mx.utng.festivaltrack.tv.presentation.components.TvDetalleConcierto
import mx.utng.festivaltrack.tv.presentation.screens.ORIGEN_DISCOGRAFIA
import mx.utng.festivaltrack.tv.presentation.screens.TvBiografiaScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvBoletosScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvComunidadScreen
import mx.utng.festivaltrack.tv.presentation.screens.TvMapaScreen
import mx.utng.festivaltrack.tv.presentation.viewmodel.Cargable
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
 * Al abrir se muestra el inicio de sesión (con opción de seguir como invitado), salvo que haya
 * una sesión guardada y vigente: entonces se entra directo con esa cuenta. Después, un
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
                    val musica by viewModel.musica.collectAsState()
                    val biografia by viewModel.biografia.collectAsState()
                    val canciones by viewModel.canciones.collectAsState()
                    val sitios by viewModel.sitios.collectAsState()
                    val comunidad by viewModel.comunidad.collectAsState()
                    val boletos by viewModel.boletos.collectAsState()

                    // Con una sesión guardada y vigente se entra directo; si no, se pide iniciar sesión.
                    var mostrarLogin by rememberSaveable { mutableStateOf(viewModel.sesion.value == null) }
                    var yaEntro by rememberSaveable { mutableStateOf(viewModel.sesion.value != null) }
                    var seccion by rememberSaveable { mutableStateOf(SeccionTv.INICIO) }
                    // Compra con QR: null = cerrada; "" = sin concierto preseleccionado.
                    var comprar by remember { mutableStateOf<String?>(null) }
                    // Detalle de concierto abierto (id) o null.
                    var detalle by remember { mutableStateOf<String?>(null) }

                    // En la transmisión se pausa la música para que se escuche el concierto (como en el móvil).
                    LaunchedEffect(seccion) { if (seccion == SeccionTv.EN_VIVO) viewModel.reproductor.pausar() }

                    val reproducirCatalogo: (Int) -> Unit = { i ->
                        (canciones as? Cargable.Listo)?.datos?.takeIf { it.isNotEmpty() }
                            ?.let { viewModel.reproductor.reproducir(it, i, ORIGEN_DISCOGRAFIA) }
                    }

                    if (mostrarLogin) {
                        // Si se abrió desde dentro (chat o Ajustes), Atrás regresa a donde estaba.
                        BackHandler(enabled = yaEntro) { mostrarLogin = false }
                        TvLoginScreen(
                            iniciarSesion = viewModel::iniciarSesion,
                            pedirCodigo = viewModel::pedirCodigoTv,
                            consultarCodigo = viewModel::consultarVinculoTv,
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
                        Column(Modifier.weight(1f)) {
                          Box(Modifier.weight(1f)) {
                            when (seccion) {
                                SeccionTv.INICIO -> TvMainScreen(
                                    proximos = proximos,
                                    stream = stream,
                                    ahora = ahora,
                                    usuario = sesion?.nombre,
                                    onVerEnVivo = { seccion = SeccionTv.EN_VIVO },
                                    onVerProgramacion = { seccion = SeccionTv.PROGRAMACION },
                                    onComprar = { comprar = it ?: "" },
                                    onAbrirConcierto = { detalle = it },
                                    onEscucharMusica = { reproducirCatalogo(0) }
                                )
                                SeccionTv.BIOGRAFIA -> TvBiografiaScreen(
                                    biografia = biografia,
                                    canciones = canciones,
                                    musica = musica,
                                    onReproducir = { lista, i -> viewModel.reproductor.reproducir(lista, i, ORIGEN_DISCOGRAFIA) },
                                    onReintentar = viewModel::cargarBiografiaYMusica
                                )
                                SeccionTv.MAPA -> TvMapaScreen(proximos = proximos, sitios = sitios, ahora = ahora)
                                SeccionTv.COMUNIDAD -> TvComunidadScreen(comunidad = comunidad, onRecargar = viewModel::cargarComunidad)
                                SeccionTv.BOLETOS -> TvBoletosScreen(
                                    sesion = sesion,
                                    boletos = boletos,
                                    ahora = ahora,
                                    onCargar = viewModel::cargarBoletos,
                                    onIniciarSesion = { mostrarLogin = true },
                                    onComprar = { comprar = "" }
                                )
                                SeccionTv.GALERIA -> TvGalleryScreen(estado = galeria, onReintentar = viewModel::cargarGaleria)
                                SeccionTv.EN_VIVO -> TvLiveStreamScreen(
                                    stream = stream,
                                    proximos = proximos,
                                    ahora = ahora,
                                    sesion = sesion,
                                    enviarMensaje = viewModel::enviarMensajeChat,
                                    onIniciarSesion = { mostrarLogin = true },
                                    onVerProgramacion = { seccion = SeccionTv.PROGRAMACION }
                                )
                                SeccionTv.PROGRAMACION -> TvScheduleScreen(
                                    proximos = proximos,
                                    stream = stream,
                                    ahora = ahora,
                                    onAbrir = { detalle = it },
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
                          // Barra del reproductor: visible en cualquier sección mientras hay música.
                          if (musica.actual != null && seccion != SeccionTv.EN_VIVO) {
                              Box(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)) {
                                  TvBarraMusica(
                                      estado = musica,
                                      onAnterior = viewModel.reproductor::anterior,
                                      onAlternar = viewModel.reproductor::alternar,
                                      onSiguiente = { viewModel.reproductor.siguiente() },
                                      onCerrar = viewModel.reproductor::detener
                                  )
                              }
                          }
                        }
                    }

                    detalle?.let { id ->
                        val evento = proximos.firstOrNull { it.id == id }
                        if (evento == null) detalle = null
                        else TvDetalleConcierto(
                            evento = evento,
                            ahora = ahora,
                            enVivo = stream?.isLive == true && stream?.eventoId == evento.id,
                            musica = musica,
                            cargarRepertorio = viewModel::repertorio,
                            onReproducir = { lista, i -> viewModel.reproductor.reproducir(lista, i, evento.nombre) },
                            onVerEnVivo = { detalle = null; seccion = SeccionTv.EN_VIVO },
                            onCerrar = { detalle = null }
                        )
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

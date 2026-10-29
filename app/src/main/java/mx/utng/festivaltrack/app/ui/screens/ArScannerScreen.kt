package mx.utng.festivaltrack.app.ui.screens

import android.Manifest
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import mx.utng.festivaltrack.app.ui.theme.DarkSurface
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.theme.TextSecondary
import mx.utng.festivaltrack.app.ui.viewmodels.ArState
import mx.utng.festivaltrack.app.ui.viewmodels.ArViewModel
import mx.utng.festivaltrack.shared.data.remote.SitioArDto
import java.util.Locale

/**
 * Pantallas 1 y 2 del módulo AR.
 *
 * 1. Vista de cámara con retícula y los pasos del flujo:
 *    apuntar → capturar foto → reconocimiento IA → resultado AR.
 * 2. Al reconocer un sitio, se superpone sobre la cámara una tarjeta AR con
 *    nombre, ubicación y descripción, y los botones "Ver en galería" y "Escuchar audio".
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ArScannerScreen(
    viewModel: ArViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onVerHistorial: () -> Unit = {},
    onVerGaleria: () -> Unit = {}
) {
    val permisoCamara = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        if (!permisoCamara.status.isGranted) permisoCamara.launchPermissionRequest()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (permisoCamara.status.isGranted) {
            ArCamara(viewModel, onVerGaleria)
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = PrimaryGold, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    "Necesitamos la cámara para reconocer los monumentos del festival.",
                    color = Color.White, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { permisoCamara.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                ) { Text("Permitir cámara") }
            }
        }

        // Barra superior
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Regresar", tint = Color.White)
            }
            Text(
                "Explorar con AR", color = PrimaryGold, fontWeight = FontWeight.Bold,
                fontSize = 20.sp, modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onVerHistorial) {
                Icon(Icons.Default.History, "Historial de sitios", tint = Color.White)
            }
        }
    }
}

@Composable
private fun BoxScope.ArCamara(viewModel: ArViewModel, onVerGaleria: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )

    // Retícula de enfoque (estilo Google Lens)
    val colorReticula = if (state is ArState.Reconocido) PrimaryGold else Color.White
    Reticula(colorReticula, Modifier.align(Alignment.Center).size(240.dp))
    if (state is ArState.Reconocido) {
        Icon(
            Icons.Default.Place, null, tint = PrimaryGold,
            modifier = Modifier.align(Alignment.Center).size(48.dp)
        )
    }

    // Pasos del flujo
    PasosFlujo(state, Modifier.align(Alignment.TopCenter).padding(top = 72.dp))

    // Parte inferior: botón de captura / mensajes / tarjeta de resultado
    Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val s = state) {
            ArState.Apuntando -> {
                Text(
                    "Apunta la cámara a un monumento del festival",
                    color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Spacer(Modifier.height(16.dp))
                BotonCaptura { viewModel.reconocer(imageCapture) }
            }
            ArState.Capturando, ArState.Reconociendo -> {
                CircularProgressIndicator(color = PrimaryGold)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (s is ArState.Capturando) "Capturando foto…" else "Reconociendo con IA…",
                    color = Color.White
                )
            }
            is ArState.NoReconocido -> MensajeReintentar(s.mensaje, Icons.Default.SearchOff) { viewModel.reiniciar() }
            is ArState.Error -> MensajeReintentar(s.mensaje, Icons.Default.ErrorOutline) { viewModel.reiniciar() }
            is ArState.Reconocido -> Unit
        }
    }

    AnimatedVisibility(
        visible = state is ArState.Reconocido,
        enter = slideInVertically { it } + fadeIn(),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        (state as? ArState.Reconocido)?.let { r ->
            TarjetaSitioAr(
                sitio = r.sitio,
                confianza = r.confianza,
                onVerGaleria = onVerGaleria,
                onEscanearOtro = { viewModel.reiniciar() }
            )
        }
    }
}

@Composable
private fun Reticula(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val largo = size.minDimension * 0.18f
        val grosor = 4.dp.toPx()
        val w = size.width
        val h = size.height
        listOf(
            Offset(0f, 0f) to Pair(Offset(largo, 0f), Offset(0f, largo)),
            Offset(w, 0f) to Pair(Offset(w - largo, 0f), Offset(w, largo)),
            Offset(0f, h) to Pair(Offset(largo, h), Offset(0f, h - largo)),
            Offset(w, h) to Pair(Offset(w - largo, h), Offset(w, h - largo)),
        ).forEach { (esquina, extremos) ->
            drawLine(color, esquina, extremos.first, grosor, StrokeCap.Round)
            drawLine(color, esquina, extremos.second, grosor, StrokeCap.Round)
        }
    }
}

@Composable
private fun PasosFlujo(state: ArState, modifier: Modifier) {
    val pasoActual = when (state) {
        ArState.Apuntando, is ArState.NoReconocido, is ArState.Error -> 0
        ArState.Capturando -> 1
        ArState.Reconociendo -> 2
        is ArState.Reconocido -> 3
    }
    val pasos = listOf("Apuntar", "Capturar", "IA", "Resultado")
    Row(
        modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pasos.forEachIndexed { i, paso ->
            val activo = i <= pasoActual
            Box(
                Modifier.size(20.dp).background(if (activo) PrimaryGold else Color.DarkGray, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("${i + 1}", fontSize = 11.sp, color = if (activo) Color.Black else Color.White) }
            Text(
                paso, fontSize = 12.sp, color = if (activo) PrimaryGold else TextSecondary,
                modifier = Modifier.padding(start = 4.dp, end = if (i < pasos.lastIndex) 8.dp else 0.dp)
            )
        }
    }
}

@Composable
private fun BotonCaptura(onClick: () -> Unit) {
    Box(
        Modifier.size(76.dp).border(4.dp, Color.White, CircleShape).padding(6.dp)
            .background(PrimaryGold, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Default.CameraAlt, "Capturar foto", tint = Color.Black, modifier = Modifier.size(32.dp)) }
}

@Composable
private fun MensajeReintentar(mensaje: String, icono: androidx.compose.ui.graphics.vector.ImageVector, onReintentar: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.95f))) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icono, null, tint = PrimaryGold)
            Spacer(Modifier.height(8.dp))
            Text(mensaje, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onReintentar,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
            ) { Text("Intentar de nuevo") }
        }
    }
}

/** Pantalla 2: tarjeta AR del sitio identificado. */
@Composable
private fun TarjetaSitioAr(
    sitio: SitioArDto,
    confianza: Double,
    onVerGaleria: () -> Unit,
    onEscanearOtro: () -> Unit
) {
    val narrador = rememberNarrador()
    var reproduciendo by remember { mutableStateOf(false) }
    DisposableEffect(sitio.id) { onDispose { narrador.detener() } }

    Card(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.96f))
    ) {
        Column(Modifier.padding(20.dp).heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = PrimaryGold, modifier = Modifier.size(18.dp))
                Text(
                    "Sitio identificado · ${(confianza * 100).toInt()}%",
                    color = PrimaryGold, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(sitio.nombre, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Text(sitio.ubicacion, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(sitio.descripcion, color = Color.White, fontSize = 15.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onVerGaleria, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = PrimaryGold)
                    Spacer(Modifier.width(6.dp))
                    Text("Ver en galería", color = PrimaryGold)
                }
                Button(
                    onClick = {
                        if (reproduciendo) narrador.detener() else narrador.reproducir(sitio) { reproduciendo = false }
                        reproduciendo = !reproduciendo
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                ) {
                    Icon(if (reproduciendo) Icons.Default.Stop else Icons.Default.VolumeUp, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (reproduciendo) "Detener" else "Escuchar audio")
                }
            }
            TextButton(onClick = onEscanearOtro, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Escanear otro sitio", color = TextSecondary)
            }
        }
    }
}

/**
 * "Escuchar audio": reproduce la audio guía del sitio si el catálogo la tiene;
 * si no, narra la descripción con Text-to-Speech en español.
 */
private class Narrador(context: android.content.Context) {
    private var player: MediaPlayer? = null
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) tts.language = Locale("es", "MX")
        }
    }

    fun reproducir(sitio: SitioArDto, alTerminar: () -> Unit) {
        detener()
        val url = sitio.audioUrl
        if (url != null) {
            player = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { start() }
                setOnCompletionListener { alTerminar() }
                prepareAsync()
            }
        } else {
            tts.speak("${sitio.nombre}. ${sitio.descripcion}", TextToSpeech.QUEUE_FLUSH, null, sitio.id)
        }
    }

    fun detener() {
        player?.release()
        player = null
        tts.stop()
    }

    fun liberar() {
        detener()
        tts.shutdown()
    }
}

@Composable
private fun rememberNarrador(): Narrador {
    val context = LocalContext.current
    val narrador = remember { Narrador(context) }
    DisposableEffect(Unit) { onDispose { narrador.liberar() } }
    return narrador
}

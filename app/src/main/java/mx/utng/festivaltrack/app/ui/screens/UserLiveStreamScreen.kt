package mx.utng.festivaltrack.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import mx.utng.festivaltrack.app.playback.PlaybackManager
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.EstadoTransmision
import mx.utng.festivaltrack.app.ui.viewmodels.LiveViewModel

private val RojoVivo = Color(0xFFC51111)

/**
 * Transmisión en vivo de un concierto con su chat.
 *
 * El video aparece solo cuando el administrador inicia la transmisión desde su panel; mientras
 * tanto se explica cuándo empieza el concierto. Si el video se corta, se reconecta solo.
 */
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserLiveStreamScreen(
    eventoId: String,
    onNavigateBack: () -> Unit,
    onAbrirOtroConcierto: (String) -> Unit = {},
    viewModel: LiveViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val estado by viewModel.estado.collectAsState()
    val errorChat by viewModel.errorChat.collectAsState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(eventoId) { viewModel.startLiveStream(eventoId) }
    DisposableEffect(Unit) { onDispose { viewModel.stopLiveStream() } }

    // MainScreen ya deja el espacio de la barra de estado: sin WindowInsets(0) se sumaba otra
    // vez y quedaba una franja vacía arriba (se nota en Android 15+, de borde a borde).
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Transmisión en vivo", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    if (estado is EstadoTransmision.EnVivo) {
                        Surface(color = RojoVivo, shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(end = 12.dp)) {
                            Text("● EN VIVO", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .imePadding()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                when (val e = estado) {
                    EstadoTransmision.Consultando -> CircularProgressIndicator(color = PrimaryGold)
                    is EstadoTransmision.EnVivo -> ReproductorEnVivo(e.urls)
                    is EstadoTransmision.SinTransmision -> AvisoVideo(
                        icono = Icons.Default.LiveTv,
                        titulo = "Este concierto aún no está en vivo",
                        detalle = buildString {
                            val fecha = formatConcertDateTime(e.fechaHora)
                            if (fecha.isNotEmpty()) append("Empieza el $fecha. ")
                            append("El video aparecerá aquí en cuanto inicie la transmisión.")
                        }
                    )
                    is EstadoTransmision.OtroConcierto -> AvisoVideo(
                        icono = Icons.Default.LiveTv,
                        titulo = "Ahora se transmite «${e.nombre}»",
                        detalle = "Este concierto todavía no está en vivo.",
                        accion = "Ver esa transmisión" to { onAbrirOtroConcierto(e.eventoId) }
                    )
                    EstadoTransmision.SinConexion -> AvisoVideo(
                        icono = Icons.Default.WifiOff,
                        titulo = "Sin conexión con el servidor",
                        detalle = "Revisa tu internet. Seguimos intentando conectar."
                    )
                }
            }

            Text(
                "Chat en vivo",
                color = PrimaryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(16.dp)
            )

            val listState = rememberLazyListState()
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                if (messages.isEmpty()) {
                    item { Text("Aún no hay mensajes. ¡Saluda a los demás asistentes!", color = Color.Gray, fontSize = 13.sp) }
                }
                items(messages) { msg ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "${msg.usuarioNombre}: ",
                            fontWeight = FontWeight.Bold,
                            color = if (msg.esAdmin) PrimaryGold else Color.LightGray
                        )
                        Text(text = msg.mensaje, color = Color.White)
                    }
                }
            }

            errorChat?.let {
                Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { if (it.length <= 300) inputText = it },
                    placeholder = { Text("Escribe un mensaje...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.sendMessage(eventoId, inputText.trim())
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier.background(PrimaryGold, RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.Black)
                }
            }
        }
    }
}

@Composable
private fun AvisoVideo(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    accion: Pair<String, () -> Unit>? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Box(Modifier.size(52.dp).background(Color(0xFF1E2720), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = PrimaryGold)
        }
        Spacer(Modifier.height(10.dp))
        Text(titulo, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(detalle, color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
        accion?.let { (texto, alTocar) ->
            TextButton(onClick = alTocar) { Text(texto, color = PrimaryGold, fontWeight = FontWeight.Bold) }
        }
    }
}

/**
 * Reproduce el RTSP del admin. Si una dirección falla prueba la siguiente; si fallan todas,
 * reintenta cada pocos segundos mientras la transmisión siga activa.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ReproductorEnVivo(urls: List<String>) {
    val context = LocalContext.current
    var intento by remember(urls) { mutableIntStateOf(0) }
    var reproduciendo by remember(urls) { mutableStateOf(false) }
    val url = urls[intento % urls.size]

    // Pausa la música de las demos para que se escuche el concierto.
    LaunchedEffect(Unit) { if (PlaybackManager.isPlaying.value) PlaybackManager.playPause() }

    val player = remember(url, intento) {
        // En vivo no hace falta acumular video: empieza con 0.3 s en lugar de los 2.5 s por defecto
        // y mantiene poco búfer para ir casi al tiempo real.
        val cargaEnVivo = DefaultLoadControl.Builder()
            .setBufferDurationsMs(500, 2_000, 300, 500)
            .build()
        ExoPlayer.Builder(context).setLoadControl(cargaEnVivo).build().apply {
            setMediaSource(
                RtspMediaSource.Factory().setForceUseRtpTcp(true).setTimeoutMs(8000)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(url)))
            )
            playWhenReady = true
            prepare()
        }
    }
    var fallo by remember(player) { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                reproduciendo = state == Player.STATE_READY
            }
            override fun onPlayerError(error: PlaybackException) {
                reproduciendo = false
                fallo = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    // Si la conexión se queda colgada sin error ni imagen, se reintenta en lugar de esperar para siempre.
    LaunchedEffect(player) {
        delay(12_000)
        if (!reproduciendo) fallo = true
    }
    LaunchedEffect(fallo) {
        if (fallo) {
            // La siguiente dirección de inmediato; tras recorrerlas todas, espera antes de reintentar.
            if ((intento + 1) % urls.size == 0) delay(5000)
            intento++
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { PlayerView(it).apply { useController = false } },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )
        if (!reproduciendo) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PrimaryGold)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (intento == 0) "Conectando con la transmisión…" else "Reconectando con la transmisión…",
                    color = Color.White, fontSize = 13.sp
                )
                // Tras probar todas las direcciones sin éxito: casi siempre es la red.
                if (intento >= urls.size) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "No llega el video de ${urls.first().removePrefix("rtsp://")}. Conéctate al mismo Wi-Fi " +
                            "que el teléfono que transmite (las redes escolares suelen bloquear la conexión entre teléfonos; " +
                            "prueben con el hotspot de uno de ellos).",
                        color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }
}

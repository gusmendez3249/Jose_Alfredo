package mx.utng.festivaltrack.tv.presentation.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.shared.data.remote.ChatMessageDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto
import mx.utng.festivaltrack.shared.util.HorarioFestival
import mx.utng.festivaltrack.tv.presentation.viewmodel.SesionTv
import mx.utng.festivaltrack.tv.ui.theme.*
import java.time.Instant

/**
 * Transmisión en vivo en la TV, con el mismo comportamiento que el móvil:
 *
 * - Prueba primero la IP real del teléfono del administrador y luego la del emulador; si una
 *   falla pasa a la siguiente y reintenta solo mientras la transmisión siga activa. Ya no se
 *   cambia a un video de demostración: si no llega la señal se dice.
 * - Sin transmisión, explica cuál es el próximo concierto y cuándo empieza.
 * - El chat es el del concierto que se transmite. Para escribir hace falta iniciar sesión
 *   (el servidor lo exige); sin sesión se ofrece entrar.
 *
 * @param enviarMensaje Envía (eventoId, texto) con la sesión actual; regresa null si se envió o
 *   el texto del error. Lo hace el ViewModel para cerrar la sesión si el token ya no sirve.
 */
@Composable
fun TvLiveStreamScreen(
    stream: StreamStatusDto?,
    proximos: List<EventoEntity>,
    ahora: Instant,
    sesion: SesionTv?,
    enviarMensaje: suspend (String, String) -> String?,
    onIniciarSesion: () -> Unit,
    onVerProgramacion: () -> Unit
) {
    val enVivo = stream?.isLive == true
    val urls = remember(stream?.streamUrl, stream?.emulatorUrl, enVivo) {
        if (!enVivo) emptyList()
        else listOfNotNull(stream?.streamUrl, stream?.emulatorUrl).filter { it.isNotBlank() }.distinct()
    }
    val eventoChat = if (enVivo) stream?.eventoId else null

    Row(modifier = Modifier.fillMaxSize().background(FestivalDarkBg)) {
        // Video (68 %)
        Column(modifier = Modifier.weight(0.68f).fillMaxHeight().padding(20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                when {
                    stream == null -> AvisoSinVideo(
                        icono = Icons.Default.CloudOff,
                        titulo = "Sin conexión con el servidor",
                        texto = "Revisa que la TV esté conectada a la red. Se vuelve a intentar sola."
                    )
                    !enVivo || urls.isEmpty() -> {
                        val siguiente = proximos.firstOrNull()
                        AvisoSinVideo(
                            icono = Icons.Default.LiveTv,
                            titulo = "No hay transmisión en este momento",
                            texto = if (siguiente != null)
                                "Próximo concierto: ${siguiente.nombre}\n${HorarioFestival.cuando(siguiente, ahora)} · ${siguiente.escenario ?: siguiente.ubicacion}\n\nEl video aparecerá aquí solo en cuanto empiece."
                            else "El video aparecerá aquí solo en cuanto el festival transmita un concierto."
                        )
                    }
                    else -> ReproductorTv(urls = urls)
                }

                if (enVivo) {
                    // Fondo oscuro para que el nombre se lea aunque el video sea claro.
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = FestivalBadgeLive, shape = RoundedCornerShape(4.dp)) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                                Spacer(Modifier.width(6.dp))
                                Text("EN VIVO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stream?.eventoNombre ?: "Festival José Alfredo Jiménez",
                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onVerProgramacion, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ver programación", color = Color.White)
                }
            }
        }

        // Chat (32 %)
        ChatTv(
            eventoId = eventoChat,
            sesion = sesion,
            enviarMensaje = enviarMensaje,
            onIniciarSesion = onIniciarSesion,
            modifier = Modifier.weight(0.32f).fillMaxHeight().background(FestivalSidebarBg).padding(20.dp)
        )
    }
}

@Composable
private fun AvisoSinVideo(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, texto: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icono, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(titulo, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(texto, color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
    }
}

/**
 * Reproduce el RTSP del administrador. Si una dirección falla (o se queda sin imagen 12 s)
 * prueba la siguiente; tras recorrerlas todas espera 5 s y vuelve a empezar.
 */
@OptIn(UnstableApi::class)
@Composable
private fun ReproductorTv(urls: List<String>) {
    val context = LocalContext.current
    var intento by remember(urls) { mutableIntStateOf(0) }
    var reproduciendo by remember(urls) { mutableStateOf(false) }
    val url = urls[intento % urls.size]

    val player = remember(url, intento) {
        // En vivo no hace falta acumular video: empieza con poco búfer para ir casi al tiempo real.
        val cargaEnVivo = DefaultLoadControl.Builder().setBufferDurationsMs(500, 2_000, 300, 500).build()
        ExoPlayer.Builder(context).setLoadControl(cargaEnVivo).build().apply {
            setMediaSource(
                RtspMediaSource.Factory().setForceUseRtpTcp(true).setTimeoutMs(8_000)
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
    // Conexión colgada sin error ni imagen: se reintenta en lugar de esperar para siempre.
    LaunchedEffect(player) {
        delay(12_000)
        if (!reproduciendo) fallo = true
    }
    LaunchedEffect(fallo) {
        if (fallo) {
            if ((intento + 1) % urls.size == 0) delay(5_000)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                CircularProgressIndicator(color = FestivalGold)
                Spacer(Modifier.height(10.dp))
                Text(
                    if (intento == 0) "Conectando con la transmisión…" else "Reconectando con la transmisión…",
                    color = Color.White, fontSize = 14.sp
                )
                if (intento >= urls.size) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "No llega el video. La TV debe estar en la misma red Wi-Fi que el teléfono que transmite.",
                        color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Chat del concierto que se transmite: mensajes reales del servidor cada 3 s.
 * Escribir exige sesión; sin ella se ofrece iniciarla.
 */
@Composable
private fun ChatTv(
    eventoId: String?,
    sesion: SesionTv?,
    enviarMensaje: suspend (String, String) -> String?,
    onIniciarSesion: () -> Unit,
    modifier: Modifier
) {
    val api = remember { FestivalApiService.create() }
    val scope = rememberCoroutineScope()
    var mensajes by remember(eventoId) { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var escribiendo by remember { mutableStateOf(false) }
    var pedirSesion by remember { mutableStateOf(false) }
    val lista = rememberLazyListState()

    LaunchedEffect(eventoId) {
        if (eventoId == null) return@LaunchedEffect
        while (true) {
            runCatching { api.getChatMessages(eventoId) }.onSuccess { mensajes = it }
            delay(3_000)
        }
    }
    LaunchedEffect(mensajes.size) { if (mensajes.isNotEmpty()) lista.animateScrollToItem(mensajes.lastIndex) }

    Column(modifier = modifier) {
        Text("Chat en vivo", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            when {
                eventoId == null -> "El chat se abre cuando empieza la transmisión."
                sesion == null -> "Inicia sesión para escribir."
                else -> "Escribes como ${sesion.nombre}."
            },
            color = FestivalTextSecondary, fontSize = 11.sp
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(state = lista, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            if (eventoId != null && mensajes.isEmpty()) {
                item { Text("Aún no hay mensajes. ¡Saluda a los demás!", color = FestivalTextSecondary, fontSize = 12.sp) }
            }
            items(mensajes) { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (msg.esAdmin) Color(0xFF2E2415) else FestivalCardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            if (msg.esAdmin) "${msg.usuarioNombre} · Festival" else msg.usuarioNombre,
                            color = FestivalGold, fontWeight = FontWeight.Bold, fontSize = 11.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(msg.mensaje, color = Color.White, fontSize = 12.sp, lineHeight = 15.sp)
                    }
                }
            }
        }

        error?.let {
            Text(it, color = FestivalBadgeLive, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(10.dp))

        var enfocado by remember { mutableStateOf(false) }
        Surface(
            onClick = { if (sesion == null) pedirSesion = true else escribiendo = true },
            enabled = eventoId != null,
            color = FestivalCardBg,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { enfocado = it.isFocused }
                .border(if (enfocado) 2.dp else 1.dp, FestivalGold.copy(alpha = if (enfocado) 1f else 0.5f), RoundedCornerShape(8.dp))
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        eventoId == null -> "Sin transmisión"
                        sesion == null -> "Pulsa OK para iniciar sesión y escribir"
                        else -> "Pulsa OK para escribir un mensaje…"
                    },
                    color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.Send, contentDescription = "Escribir", tint = FestivalGold, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (escribiendo && sesion != null && eventoId != null) {
        TvChatInputDialog(
            onDismiss = { escribiendo = false },
            onSend = { texto ->
                val limpio = texto.trim()
                if (limpio.isNotEmpty()) scope.launch {
                    error = enviarMensaje(eventoId, limpio)
                    if (error == null) {
                        mensajes = mensajes + ChatMessageDto(eventoId = eventoId, usuarioNombre = sesion.nombre, mensaje = limpio)
                    }
                }
            }
        )
    }
    if (pedirSesion) {
        AlertDialog(
            onDismissRequest = { pedirSesion = false },
            containerColor = FestivalCardBg,
            title = { Text("Inicia sesión para escribir", color = FestivalGold, fontWeight = FontWeight.Bold) },
            text = { Text("El chat necesita tu cuenta del festival para saber quién escribe.", color = Color.White) },
            confirmButton = {
                Button(
                    onClick = { pedirSesion = false; onIniciarSesion() },
                    colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)
                ) { Text("Iniciar sesión", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pedirSesion = false }) { Text("Ahora no", color = Color.Gray) } }
        )
    }
}

/**
 * Diálogo para escribir con el teclado en pantalla de la TV.
 */
@Composable
fun TvChatInputDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A2118),
        title = { Text("Escribe tu mensaje", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(300) },
                placeholder = { Text("Tu mensaje aquí...", color = Color.Gray, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0D1210),
                    unfocusedContainerColor = Color(0xFF0D1210),
                    focusedBorderColor = FestivalGold,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = FestivalGold
                ),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { onSend(text); onDismiss() },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)
            ) { Text("Enviar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } }
    )
}

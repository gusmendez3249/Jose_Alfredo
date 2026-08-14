package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.*

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.launch

/**
 * Representa un mensaje dentro del chat comunitario de la Smart TV.
 *
 * @property user Nombre del remitente.
 * @property text Texto del mensaje.
 * @property time Etiqueta de tiempo (ej. "Hace 2m" o "En vivo").
 * @property isAdmin Indica si el mensaje proviene del administrador (para resaltado).
 */
data class ChatMessage(val user: String, val text: String, val time: String, val isAdmin: Boolean = false)

/**
 * Pantalla de Transmisión en Vivo para Smart TV ([TvLiveStreamScreen]).
 *
 * Esta pantalla integra el flujo en vivo del festival adaptado a pantallas grandes.
 * Layout estructurado en:
 * - **Reproductor RTSP (68%)**: Reproductor principal implementado con [ExoPlayer] (Media3) para
 *   consumir una señal de video en tiempo real (RTSP) directamente desde el emulador o red local.
 * - **Panel de Chat (32%)**: Barra lateral que muestra mensajes en vivo. Utiliza una técnica de
 *   polling cada 3 segundos para sincronizar la lista de mensajes con el backend mediante HTTP.
 *
 * Para interactuar en el chat usando un control remoto, cuenta con un área especial navegable
 * con el D-Pad. Al presionarse ("OK"), invoca el [TvChatInputDialog] que levanta el teclado en pantalla.
 *
 * @param currentNavIndex Índice de la opción de navegación actualmente seleccionada.
 * @param onNavSelect Callback para cambiar de pantalla al usar el menú lateral.
 */
@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun TvLiveStreamScreen(
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit
) {
    var showChatDialog by remember { mutableStateOf(false) }
    var isPlayingStream by remember { mutableStateOf(true) }
    val chatFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    // Cliente HTTP para enviar y consultar mensajes al backend
    val api = remember { mx.utng.festivaltrack.shared.data.remote.FestivalApiService.create() }
    val coroutineScope = rememberCoroutineScope()

    // Lista reactiva de mensajes de chat mostrados en pantalla
    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage("Miguel Angel", "¡Qué bonita es mi tierra! Saludos desde Chicago.", "Hace 2m"),
            ChatMessage("Elena Jimenez", "La interpretación de Camino de Guanajuato espectacular.", "Hace 1m"),
            ChatMessage("Admin Festival", "¡Bienvenidos a la transmisión oficial en Smart TV!", "Hace 4m", isAdmin = true)
        )
    }

    // Diálogo de chat (D-Pad accesible)
    if (showChatDialog) {
        TvChatInputDialog(
            onDismiss = { showChatDialog = false },
            onSend = { msgText ->
                if (msgText.isNotBlank()) {
                    chatMessages.add(ChatMessage("Espectador TV", msgText, "Ahora"))
                    coroutineScope.launch {
                        try {
                            api.sendChatMessage(
                                mx.utng.festivaltrack.shared.data.remote.ChatMessageDto(
                                    eventoId = "EVT-001",
                                    usuarioNombre = "Espectador TV",
                                    mensaje = msgText
                                )
                            )
                        } catch (e: Exception) { /* red */ }
                    }
                }
            }
        )
    }
    var currentStreamUrl by remember { mutableStateOf("") }
    var isLive by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }
    val fallbackDemoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"

    // ExoPlayer — se recrea cuando cambia la URL del stream
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    coroutineScope.launch {
                        if (isLive && retryCount < 2) {
                            retryCount++
                            kotlinx.coroutines.delay(1500)
                            prepare()
                            playWhenReady = true
                        } else if (isLive && retryCount >= 2 && currentStreamUrl != fallbackDemoUrl) {
                            // Si RTSP falla por red de emuladores, cambiar a stream demo en vivo
                            currentStreamUrl = fallbackDemoUrl
                            setMediaItem(androidx.media3.common.MediaItem.fromUri(fallbackDemoUrl))
                            prepare()
                            playWhenReady = true
                        }
                    }
                }
            })
        }
    }

    // Función para cargar/cambiar la URL en el reproductor
    fun loadStream(url: String) {
        if (url.isNotBlank()) {
            retryCount = 0
            try {
                if (url.startsWith("rtsp://")) {
                    val mediaSource = androidx.media3.exoplayer.rtsp.RtspMediaSource.Factory()
                        .setForceUseRtpTcp(true)
                        .createMediaSource(androidx.media3.common.MediaItem.fromUri(url))
                    exoPlayer.setMediaSource(mediaSource)
                } else {
                    val mediaItem = androidx.media3.common.MediaItem.fromUri(url)
                    exoPlayer.setMediaItem(mediaItem)
                }
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } catch (e: Exception) {
                val mediaItem = androidx.media3.common.MediaItem.fromUri(url)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        } else {
            exoPlayer.stop()
        }
    }

    // Liberar recursos del reproductor al abandonar la pantalla
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Polling del stream: cada 3s consulta si el admin inició el live y mantiene reproduciendo
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val status = api.getStreamStatus()
                val urlParaTV = if (status.emulatorUrl.isNotBlank()) status.emulatorUrl else status.streamUrl
                
                isLive = status.isLive
                
                if (status.isLive && urlParaTV.isNotBlank()) {
                    val isNewStream = urlParaTV != currentStreamUrl
                    val isIdleOrError = exoPlayer.playbackState == androidx.media3.common.Player.STATE_IDLE || exoPlayer.playerError != null
                    
                    if (isNewStream || isIdleOrError) {
                        currentStreamUrl = urlParaTV
                        loadStream(currentStreamUrl)
                    }
                } else if (!status.isLive) {
                    currentStreamUrl = ""
                    if (exoPlayer.isPlaying) {
                        exoPlayer.stop()
                    }
                }
            } catch (e: Exception) {
                // Falla silenciosa
            }
            kotlinx.coroutines.delay(3000)
        }
    }

    // Polling del chat: cada 3s carga mensajes reales del backend
    LaunchedEffect(Unit) {
        while (true) {
            try {
                // Intentar obtener el eventoId del primer evento activo, o usar EVT-001 por defecto
                val msgs = api.getChatMessages("EVT-001")
                if (msgs.isNotEmpty()) {
                    chatMessages.clear()
                    msgs.forEach { m ->
                        chatMessages.add(
                            ChatMessage(
                                user = m.usuarioNombre,
                                text = m.mensaje,
                                time = "En vivo",
                                isAdmin = m.esAdmin
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Falla silenciosa en polling de chat
            }
            kotlinx.coroutines.delay(3000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
    ) {
        // SIDEBAR MENÚ LATERAL (Navegación con D-Pad)
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .background(FestivalSidebarBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Festival 2024",
                    color = FestivalGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                val navItems = listOf(
                    "Inicio" to Icons.Default.Home,
                    "Galería Histórica" to Icons.Default.Collections,
                    "Transmisión En Vivo" to Icons.Default.Tv,
                    "Programación" to Icons.Default.Event,
                    "Ajustes" to Icons.Default.Settings
                )

                navItems.forEachIndexed { index, (label, icon) ->
                    SidebarMenuItem(
                        label = label,
                        icon = icon,
                        isSelected = currentNavIndex == index,
                        onClick = { onNavSelect(index) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        // ÁREA DEL REPRODUCTOR EN VIVO (68% del ancho)
        Column(
            modifier = Modifier
                .weight(0.68f)
                .fillMaxHeight()
                .padding(20.dp)
        ) {
            // Reproductor ExoPlayer de Android
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false // Controles personalizados en Compose
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay: "Sin señal" cuando no hay stream activo
                if (!isLive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A0A0A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(72.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Sin transmisión activa", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("El administrador aún no ha iniciado el live", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                            Surface(
                                color = Color(0xFF1A2118),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "⏳ Actualizando cada 5 segundos...",
                                    color = FestivalGold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Festival José Alfredo Jiménez",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = FestivalBadgeLive,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("LIVE NOW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("1251 PERSONAS VIENDO", color = Color.White, fontSize = 10.sp)
                        }
                    }

                    // Título y botones de acción
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Serenata de Gala: Mausoleo Dolores Hidalgo",
                            color = FestivalGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "En vivo desde la Cuna de la Independencia Nacional. Un tributo al Rey de la Canción Ranchera.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    isPlayingStream = !isPlayingStream
                                    if (isPlayingStream) exoPlayer.play() else exoPlayer.pause()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(if (isPlayingStream) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPlayingStream) "PAUSAR EN VIVO" else "REANUDAR", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("VER PROGRAMA", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // PANEL DE CHAT LATERAL (32% del ancho)
        Column(
            modifier = Modifier
                .weight(0.32f)
                .fillMaxHeight()
                .background(FestivalSidebarBg)
                .padding(20.dp)
        ) {
            Text(
                text = "Comunidad en Vivo",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Escribe un mensaje para participar en la pantalla", color = FestivalTextSecondary, fontSize = 10.sp)

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de mensajes enviada por usuarios
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(chatMessages) { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isAdmin) Color(0xFF2E2415) else FestivalCardBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(msg.user, color = FestivalGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(msg.time, color = FestivalTextSecondary, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(msg.text, color = Color.White, fontSize = 11.sp, lineHeight = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Entrada de Chat adaptada a Smart TV (abrir diálogo con D-Pad)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(chatFocusRequester)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showChatDialog = true }
                        .background(FestivalCardBg, RoundedCornerShape(8.dp))
                        .border(1.dp, FestivalGold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Pulsa OK para escribir un mensaje...",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showChatDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FestivalGold)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.Black)
                }
            }
        }
    }
}

/**
 * Diálogo modal para escribir texto en Smart TV ([TvChatInputDialog]).
 * Despliega un [OutlinedTextField] que solicita el foco del teclado inmediatamente al abrirse.
 *
 * @param onDismiss Callback para cerrar el diálogo.
 * @param onSend Callback ejecutado al presionar Enviar con el mensaje escrito.
 */
@Composable
fun TvChatInputDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A2118),
        title = {
            Text("Escribe tu mensaje", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Tu mensaje aquí...", color = Color.Gray, fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
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
                onClick = {
                    onSend(text)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FestivalGold,
                    contentColor = Color.Black
                )
            ) {
                Text("Enviar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

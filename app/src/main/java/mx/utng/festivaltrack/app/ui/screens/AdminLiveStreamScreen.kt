package mx.utng.festivaltrack.app.ui.screens

import android.Manifest
import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.pedro.common.ConnectChecker
import com.pedro.rtspserver.RtspServerCamera1
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.TokenManager
import mx.utng.festivaltrack.app.ui.utils.ProximosEventos
import mx.utng.festivaltrack.shared.data.local.entity.EventoEntity
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto

/**
 * Pantalla de Emisión de Transmisión en Vivo para Administradores ([AdminLiveStreamScreen]).
 *
 * Implementa un servidor RTSP nativo utilizando la librería `Root-less RTSP Server` de Pedro.
 * Permite capturar video desde la cámara física del smartphone y emitir un stream en vivo
 * por el puerto TCP `1935`.
 *
 * Características clave:
 * 1. **Gestión de Permisos**: Solicita dinámicamente permisos de `CAMERA` y `RECORD_AUDIO`.
 * 2. **Previsualización de Cámara**: Usa [AndroidView] envolviendo un [SurfaceView] de Android.
 * 3. **Servidor RTSP Integrado**: Escucha conexiones entrantes de clientes (Smart TV o Web)
 *    en `rtsp://<IP>:1935`.
 * 4. **Limpieza Automática**: [DisposableEffect] detiene la vista previa y el stream al salir.
 *
 * @param onNavigateBack Callback de navegación para regresar al panel anterior.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminLiveStreamScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val api = remember { FestivalApiService.create() }

    // Estado para gestionar permisos en runtime (Cámara y Micrófono)
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    // Instancia del servidor RTSP de cámara
    var rtspServer by remember { mutableStateOf<RtspServerCamera1?>(null) }
    var isStreaming by remember { mutableStateOf(false) }
    var cambiando by remember { mutableStateOf(false) }
    var streamUrl by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Listo para transmitir") }

    // Concierto al que pertenece la transmisión: los usuarios la ven en la pantalla de ese concierto.
    var eventos by remember { mutableStateOf<List<EventoEntity>>(emptyList()) }
    var eventoElegido by remember { mutableStateOf<EventoEntity?>(null) }
    var menuEventos by remember { mutableStateOf(false) }
    val bearer = remember { "Bearer ${TokenManager(context).getToken()}" }

    fun estado(enVivo: Boolean) = StreamStatusDto(
        streamUrl = if (enVivo) streamUrl else "",
        isLive = enVivo,
        eventoId = if (enVivo) eventoElegido?.id else null,
        eventoNombre = if (enVivo) eventoElegido?.nombre else null
    )

    LaunchedEffect(Unit) {
        eventos = runCatching { ProximosEventos.filtrar(api.getEventos().map { it.toEntity() }) }.getOrDefault(emptyList())
        if (eventoElegido == null) eventoElegido = eventos.firstOrNull()
    }

    // Mientras transmite, reporta cada 15 s: si la app se cierra, el servidor da la transmisión por terminada.
    LaunchedEffect(isStreaming, eventoElegido) {
        while (isStreaming) {
            runCatching { api.setStreamStatus(bearer, estado(true)) }
                .onFailure { statusText = "En vivo, pero sin conexión con el servidor" }
            delay(15_000)
        }
    }

    // Solicita permisos al cargar la pantalla y obtiene la IP local
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // IP real del teléfono en la red (Wi-Fi o hotspot). Se revisa cada pocos segundos por si el
    // admin se conecta a la red con la pantalla abierta.
    LaunchedEffect(Unit) {
        while (true) {
            if (!isStreaming) {
                val ip = mx.utng.festivaltrack.app.ui.utils.IpLocal.paraTransmitir()
                streamUrl = ip?.let { "rtsp://$it:1935" } ?: ""
                if (ip == null) {
                    statusText = "Sin red Wi-Fi: conecta este teléfono al mismo Wi-Fi que los asistentes o comparte tu hotspot"
                } else if (statusText.startsWith("Sin red Wi-Fi")) {
                    statusText = "Listo para transmitir"
                }
            }
            delay(5_000)
        }
    }

    // Efecto de limpieza: se ejecuta cuando la pantalla se destruye o el usuario navega fuera
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (isStreaming) {
                    val servidor = rtspServer
                    // La pantalla ya no existe: detiene y avisa al servidor fuera de su ciclo de vida
                    // y fuera del hilo principal.
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching { servidor?.stopStream() }
                        runCatching { api.setStreamStatus(bearer, StreamStatusDto(streamUrl = "", isLive = false)) }
                    }
                }
                rtspServer?.stopPreview()
            } catch (e: Exception) {
                // Previene cierres inesperados durante la liberación de hardware
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transmisión en vivo", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (permissionsState.allPermissionsGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Tarjeta contenedora de la vista previa de la cámara
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceView(ctx).apply {
                                holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {
                                        try {
                                            // Callback de estado para el servidor de RTSP
                                            val checker = object : ConnectChecker {
                                                override fun onAuthError() { statusText = "Error de Auth" }
                                                override fun onAuthSuccess() { }
                                                override fun onConnectionFailed(reason: String) {
                                                    statusText = "Error: $reason"
                                                    isStreaming = false
                                                    try { rtspServer?.stopStream() } catch (e: Exception) {}
                                                }
                                                override fun onConnectionStarted(url: String) {
                                                    statusText = "Transmisión Activa en $url"
                                                }
                                                override fun onConnectionSuccess() { }
                                                override fun onDisconnect() {
                                                    statusText = "Transmisión detenida"
                                                }
                                                override fun onNewBitrate(bitrate: Long) { }
                                            }
                                            
                                            // Inicializa el servidor RTSP en el puerto 1935
                                            val server = RtspServerCamera1(this@apply, checker, 1935)
                                            rtspServer = server
                                            server.startPreview()
                                        } catch (e: Exception) {
                                            statusText = "Error al iniciar cámara: ${e.localizedMessage}"
                                        }
                                    }

                                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                                        // El stream lo detiene onDispose en segundo plano; aquí solo la vista previa.
                                        runCatching { rtspServer?.stopPreview() }
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Panel inferior de control y estado del stream
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2720))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(statusText, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (streamUrl.isBlank()) "Sin dirección de video" else "Dirección del video (TV y app):\n$streamUrl", color = PrimaryGold, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box {
                            OutlinedButton(
                                onClick = { if (!isStreaming) menuEventos = true },
                                enabled = !isStreaming && eventos.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    eventoElegido?.let { "Concierto: ${it.nombre} · ${ProximosEventos.dia(it)} ${ProximosEventos.mesCorto(it)} ${ProximosEventos.hora(it)}" }
                                        ?: "No hay conciertos próximos",
                                    color = Color.White, maxLines = 2
                                )
                            }
                            DropdownMenu(expanded = menuEventos, onDismissRequest = { menuEventos = false }) {
                                eventos.forEach { ev ->
                                    DropdownMenuItem(
                                        text = { Text("${ev.nombre} · ${ProximosEventos.dia(ev)} ${ProximosEventos.mesCorto(ev)} ${ProximosEventos.hora(ev)}") },
                                        onClick = { eventoElegido = ev; menuEventos = false }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón de Inicio / Detención de Transmisión
                        Button(
                            onClick = {
                                // Preparar los códecs y arrancar el servidor tarda varios segundos: se hace
                                // fuera del hilo principal para que la pantalla no se congele (ANR).
                                val servidor = rtspServer
                                if (!isStreaming) {
                                    cambiando = true
                                    statusText = "Iniciando transmisión…"
                                    coroutineScope.launch {
                                        val listo = kotlinx.coroutines.withContext(Dispatchers.Default) {
                                            runCatching {
                                                // Códecs de audio y video con resolución compatible 640x480
                                                val audio = servidor?.prepareAudio() ?: false
                                                val video = runCatching { servidor?.prepareVideo(640, 480, 30, 1200 * 1024, 0) ?: false }
                                                    .getOrElse { servidor?.prepareVideo() ?: false }
                                                if (audio && video) servidor?.startStream()
                                                audio && video
                                            }.getOrDefault(false)
                                        }
                                        cambiando = false
                                        if (listo) {
                                            isStreaming = true
                                            statusText = "Transmisión en vivo ACTIVA"
                                            // El reporte periódico (LaunchedEffect de arriba) publica la URL y el concierto.
                                        } else {
                                            statusText = "Error al inicializar cámara/audio"
                                        }
                                    }
                                } else {
                                    cambiando = true
                                    isStreaming = false
                                    statusText = "Deteniendo transmisión…"
                                    coroutineScope.launch {
                                        kotlinx.coroutines.withContext(Dispatchers.Default) { runCatching { servidor?.stopStream() } }
                                        cambiando = false
                                        statusText = "Listo para transmitir"
                                        runCatching { api.setStreamStatus(bearer, estado(false)) }
                                    }
                                }
                            },
                            // Sin IP en la red nadie podría ver el video: no se deja iniciar.
                            enabled = !cambiando && (isStreaming || streamUrl.isNotBlank()),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStreaming) Color.Red else PrimaryGold,
                                contentColor = if (isStreaming) Color.White else Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(if (isStreaming) Icons.Default.Stop else Icons.Default.Cast, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isStreaming) "DETENER TRANSMISIÓN" else "INICIAR EN VIVO", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Pantalla de advertencia si no se otorgaron permisos de hardware
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Se requieren permisos de cámara y micrófono para transmitir.", color = Color.White)
                    TextButton(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                        Text("Dar permisos", color = PrimaryGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

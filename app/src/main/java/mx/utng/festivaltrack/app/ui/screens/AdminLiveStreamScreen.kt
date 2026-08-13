package mx.utng.festivaltrack.app.ui.screens

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
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
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.StreamStatusDto

/**
 * Obtiene la dirección IP local del dispositivo dentro de la red Wi-Fi.
 *
 * Esta función consulta el [WifiManager] de Android para calcular la IP en formato `X.X.X.X`.
 * Si el dispositivo se encuentra dentro del emulador Android o la IP es nula, retorna
 * por defecto `"10.0.2.2"` (alias del host en emuladores).
 *
 * @param context Contexto de la aplicación.
 * @return Dirección IP en formato String para la URL del servidor RTSP.
 */
fun getLocalIpAddress(context: Context): String {
    return try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ipAddress = wifiManager?.connectionInfo?.ipAddress ?: 0
        if (ipAddress != 0) {
            String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        } else {
            "10.0.2.2"
        }
    } catch (e: Exception) {
        "10.0.2.2"
    }
}

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
    var streamUrl by remember { mutableStateOf("rtsp://10.0.2.2:1935") }
    var statusText by remember { mutableStateOf("Listo para transmitir") }

    // Solicita permisos al cargar la pantalla y obtiene la IP local
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
        val wifiIp = getLocalIpAddress(context)
        streamUrl = "rtsp://$wifiIp:1935"
    }

    // Efecto de limpieza: se ejecuta cuando la pantalla se destruye o el usuario navega fuera
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (isStreaming) rtspServer?.stopStream()
                rtspServer?.stopPreview()
            } catch (e: Exception) {
                // Previene cierres inesperados durante la liberación de hardware
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Live - Administrador", color = Color.White) },
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
                                        if (isStreaming) rtspServer?.stopStream()
                                        rtspServer?.stopPreview()
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
                        Text("Ingresa esto en la TV:\n$streamUrl", color = PrimaryGold, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón de Inicio / Detención de Transmisión
                        Button(
                            onClick = {
                                if (!isStreaming) {
                                    // Prepara códecs de audio y video con resolución compatible 640x480
                                    val audioPrepared = rtspServer?.prepareAudio() ?: false
                                    val videoPrepared = try {
                                        rtspServer?.prepareVideo(640, 480, 30, 1200 * 1024, 0) ?: false
                                    } catch (e: Exception) {
                                        rtspServer?.prepareVideo() ?: false
                                    }

                                    if (audioPrepared && videoPrepared) {
                                        rtspServer?.startStream()
                                        isStreaming = true
                                        statusText = "Transmisión en Vivo ACTIVA"
                                        // Publicar URL del stream al backend para que la TV se conecte
                                        coroutineScope.launch {
                                            try {
                                                api.setStreamStatus(StreamStatusDto(streamUrl = streamUrl, isLive = true))
                                            } catch (e: Exception) { /* Fallo silencioso */ }
                                        }
                                    } else {
                                        statusText = "Error al inicializar cámara/audio"
                                    }
                                } else {
                                    rtspServer?.stopStream()
                                    isStreaming = false
                                    statusText = "Listo para transmitir"
                                    // Notificar al backend que el stream terminó
                                    coroutineScope.launch {
                                        try {
                                            api.setStreamStatus(StreamStatusDto(streamUrl = "", isLive = false))
                                        } catch (e: Exception) { /* Fallo silencioso */ }
                                    }
                                }
                            },
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
                Text("Se requieren permisos de cámara y micrófono.", color = Color.White)
            }
        }
    }
}

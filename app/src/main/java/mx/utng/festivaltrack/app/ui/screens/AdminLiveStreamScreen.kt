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
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

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

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminLiveStreamScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    var rtspServer by remember { mutableStateOf<RtspServerCamera1?>(null) }
    var isStreaming by remember { mutableStateOf(false) }
    var streamUrl by remember { mutableStateOf("rtsp://10.0.2.2:1935") }
    var statusText by remember { mutableStateOf("Listo para transmitir") }

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
        val wifiIp = getLocalIpAddress(context)
        streamUrl = "rtsp://$wifiIp:1935"
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (isStreaming) rtspServer?.stopStream()
                rtspServer?.stopPreview()
            } catch (e: Exception) {
                // Ignore cleanup errors
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

                        Button(
                            onClick = {
                                if (!isStreaming) {
                                    if (rtspServer?.prepareAudio() == true && rtspServer?.prepareVideo() == true) {
                                        rtspServer?.startStream()
                                        isStreaming = true
                                    } else {
                                        statusText = "Error al inicializar cámara/audio"
                                    }
                                } else {
                                    rtspServer?.stopStream()
                                    isStreaming = false
                                    statusText = "Listo para transmitir"
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Se requieren permisos de cámara y micrófono.", color = Color.White)
            }
        }
    }
}

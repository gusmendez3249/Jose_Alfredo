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
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import com.pedro.rtspserver.RtspServerCamera1
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

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
    var streamUrl by remember { mutableStateOf("Desconocida") }
    var statusText by remember { mutableStateOf("Listo para transmitir") }

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
        
        // Obtener IP
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = wifiManager.connectionInfo.ipAddress
        if (ipAddress != 0) {
            val ip = String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
            streamUrl = "rtsp://$ip:1935"
        } else {
            // Emuladores a veces no reportan Wifi. Para pruebas en emulador usando el adb forward:
            streamUrl = "rtsp://10.0.2.2:1935"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isStreaming) {
                rtspServer?.stopStream()
            }
            rtspServer?.stopPreview()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transmisión en Vivo", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (permissionsState.allPermissionsGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cámara Preview
                Box(
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
                                        val checker = object : ConnectCheckerRtsp {
                                            override fun onAuthErrorRtsp() { statusText = "Error de Auth" }
                                            override fun onAuthSuccessRtsp() { }
                                            override fun onConnectionFailedRtsp(reason: String) {
                                                statusText = "Error: $reason"
                                                isStreaming = false
                                                rtspServer?.stopStream()
                                            }
                                            override fun onConnectionStartedRtsp(rtspUrl: String) {
                                                statusText = "Transmisión Activa en $rtspUrl"
                                            }
                                            override fun onConnectionSuccessRtsp() { }
                                            override fun onDisconnectRtsp() {
                                                statusText = "Transmisión detenida"
                                            }
                                            override fun onNewBitrateRtsp(bitrate: Long) { }
                                        }
                                        
                                        val server = RtspServerCamera1(this@apply, checker, 1935)
                                        rtspServer = server
                                        server.startPreview()
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
                                        rtspServer?.startStream("")
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

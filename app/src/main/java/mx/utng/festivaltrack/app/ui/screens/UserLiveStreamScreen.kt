package mx.utng.festivaltrack.app.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.LiveViewModel

@kotlin.OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun UserLiveStreamScreen(
    eventoId: String,
    onNavigateBack: () -> Unit,
    viewModel: LiveViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val streamUrl by viewModel.streamUrl.collectAsState()
    var inputText by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(eventoId) {
        viewModel.startLiveStream(eventoId)
    }

    DisposableEffect(streamUrl) {
        if (streamUrl != null) {
            val player = ExoPlayer.Builder(context).build()
            val mediaSource = RtspMediaSource.Factory()
                .createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl)))
            player.setMediaSource(mediaSource)
            player.prepare()
            player.playWhenReady = true
            exoPlayer = player
        }

        onDispose {
            exoPlayer?.release()
            exoPlayer = null
            viewModel.stopLiveStream()
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Video Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (streamUrl != null && exoPlayer != null) {
                    AndroidView(
                        factory = {
                            PlayerView(context).apply {
                                player = exoPlayer
                                useController = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryGold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Esperando transmisión...", color = Color.Gray)
                    }
                }
            }

            // Chat Title
            Text(
                "Chat en vivo",
                color = PrimaryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(16.dp)
            )

            // Chat Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "${msg.usuarioNombre}: ",
                            fontWeight = FontWeight.Bold,
                            color = if (msg.esAdmin) PrimaryGold else Color.LightGray
                        )
                        Text(
                            text = msg.mensaje,
                            color = Color.White
                        )
                    }
                }
            }

            // Chat Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Escribe un mensaje...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = Color.Transparent,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.sendMessage(eventoId, inputText)
                        inputText = ""
                    },
                    modifier = Modifier
                        .background(PrimaryGold, RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.Black)
                }
            }
        }
    }
}

package mx.utng.festivaltrack.app.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.viewmodels.AudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScreen(viewModel: AudioViewModel = viewModel()) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf(0.35f) }
    var currentTrackTitle by remember { mutableStateOf("El Rey - Mariachi Festival") }
    val scrollState = rememberScrollState()

    val canciones by viewModel.canciones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var currentTrack by remember { mutableStateOf<mx.utng.festivaltrack.shared.data.remote.CancionDto?>(null) }
    
    // Auto-select first track when loaded
    LaunchedEffect(canciones) {
        if (canciones.isNotEmpty() && currentTrack == null) {
            currentTrack = canciones.first()
        }
    }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Streaming & Podcasts", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Icon(
                        Icons.Default.Headphones,
                        contentDescription = "Podcast",
                        tint = PrimaryGold,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Vinyl Record / Album Art
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                // Inner groove
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(PrimaryGold)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = currentTrack?.titulo ?: "Selecciona una pista",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = currentTrack?.artista ?: "",
                color = if (isPlaying) PrimaryGold else Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Bar
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryGold,
                    activeTrackColor = PrimaryGold,
                    inactiveTrackColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("02:15", color = Color.Gray, fontSize = 12.sp)
                Text("04:30", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    sliderPosition = (sliderPosition - 0.1f).coerceAtLeast(0f)
                }) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                
                FloatingActionButton(
                    onClick = {
                        try {
                            if (isPlaying) {
                                mediaPlayer?.pause()
                                isPlaying = false
                            } else {
                                if (mediaPlayer == null && currentTrack != null) {
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(currentTrack!!.archivoUrl)
                                        prepareAsync()
                                        setOnPreparedListener {
                                            start()
                                            isPlaying = true
                                        }
                                    }
                                } else if (mediaPlayer != null) {
                                    mediaPlayer?.start()
                                    isPlaying = true
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            isPlaying = !isPlaying
                        }
                    },
                    containerColor = PrimaryGold,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = {
                    sliderPosition = (sliderPosition + 0.1f).coerceAtMost(1f)
                }) {
                    Icon(Icons.Default.FastForward, contentDescription = "Forward", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Episodes List
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Catálogo de Canciones", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    canciones.forEachIndexed { index, cancion ->
                        val minutes = cancion.duracion / 60
                        val seconds = cancion.duracion % 60
                        val durationStr = String.format("%02d:%02d", minutes, seconds)
                        
                        EpisodeItem(
                            number = (index + 1).toString(),
                            title = cancion.titulo,
                            duration = durationStr,
                            isPlaying = currentTrack?.id == cancion.id,
                            onClick = { 
                                if (currentTrack?.id != cancion.id) {
                                    currentTrack = cancion
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    isPlaying = false
                                }
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun EpisodeItem(number: String, title: String, duration: String, isPlaying: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if(isPlaying) Color(0xFF2A3A2C) else Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = number,
                color = if (isPlaying) PrimaryGold else Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.width(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(duration, color = Color.Gray, fontSize = 12.sp)
            }
            if (isPlaying) {
                Icon(Icons.Default.Headphones, contentDescription = "Playing", tint = PrimaryGold)
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
            }
        }
    }
}

package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
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

data class ChatMessage(val user: String, val text: String, val time: String, val isAdmin: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun TvLiveStreamScreen(
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit
) {
    var isPlayingStream by remember { mutableStateOf(true) }
    var userMessageText by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val streamUrl = "rtsp://10.0.2.2:1935"
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(streamUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage("Miguel Angel", "¡Qué bonita es mi tierra! Saludos desde Chicago, extrañando a mi José Alfredo.", "Hace 2m"),
            ChatMessage("Elena Jimenez", "La interpretación de Camino de Guanajuato me puso la piel de gallina. ¡Viva México!", "Hace 1m"),
            ChatMessage("Admin Festival", "¡Bienvenidos todos! No olviden que pueden comprar sus boletos para la gala física.", "Hace 4m", isAdmin = true),
            ChatMessage("Roberto C.", "¿A qué hora empieza la transmisión desde la plaza principal?", "Ahora")
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
    ) {
        // SIDEBAR
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .background(FestivalSidebarBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Festival 2024", color = FestivalGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                val navItems = listOf(
                    "Inicio" to Icons.Default.Home,
                    "Galería Histórica" to Icons.Default.Collections,
                    "Transmisión En Vivo" to Icons.Default.LiveTv,
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

        // 68% VIDEO PLAYER STREAM AREA
        Column(
            modifier = Modifier
                .weight(0.68f)
                .fillMaxHeight()
                .padding(20.dp)
        ) {
            // Video Player
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
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay Container (to keep UI on top of video)
                Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Top Overlay Badges
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
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isPlayingStream) "LIVE NOW" else "PAUSED",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${1248 + chatMessages.size} PERSONAS VIENDO", color = Color.White, fontSize = 11.sp)
                    }
                }

                // Video Title Overlay at bottom of video
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Serenata de Gala: Mausoleo Dolores Hidalgo",
                        color = FestivalGold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "En vivo desde la Cuna de la Independencia Nacional. Un tributo al Rey de la Canción Ranchera.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { 
                                isPlayingStream = !isPlayingStream 
                                if (isPlayingStream) {
                                    exoPlayer.play()
                                } else {
                                    exoPlayer.pause()
                                }
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
                } // End Overlay Container
            } // End Video Player Container
        }

        // 32% RIGHT CHAT & SETLIST PANEL
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

            // Chat Messages List
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

            // Chat Input Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userMessageText,
                    onValueChange = { userMessageText = it },
                    placeholder = { Text("Escribe un mensaje...", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FestivalCardBg,
                        unfocusedContainerColor = FestivalCardBg,
                        focusedBorderColor = FestivalGold,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (userMessageText.isNotBlank()) {
                            chatMessages.add(ChatMessage("Espectador TV", userMessageText, "Ahora"))
                            userMessageText = ""
                        }
                    },
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
}

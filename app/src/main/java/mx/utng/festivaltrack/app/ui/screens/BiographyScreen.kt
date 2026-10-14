package mx.utng.festivaltrack.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mx.utng.festivaltrack.app.R
import mx.utng.festivaltrack.app.data.factory.BiographyEntryType
import mx.utng.festivaltrack.app.data.factory.InteractiveBiographyItem
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.ArtistViewModel

@Composable
fun BiographyScreen(viewModel: ArtistViewModel = viewModel()) {
    val scrollState = rememberScrollState()
    val biografia by viewModel.biografia.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var isAudioPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D120E))
                .verticalScroll(scrollState)
        ) {
            // ─── 1. HERO SECTION DINÁMICO ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                // Retrato oficial con sombrero charro
                Image(
                    painter = painterResource(id = R.drawable.jose_alfredo_portrait),
                    contentDescription = "José Alfredo Jiménez",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradiente oscuro profundo de gran elegancia
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x990D120E),
                                    Color(0xFF0D120E)
                                )
                            )
                        )
                )

                // Insignia superior
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 20.dp, start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                            .border(1.dp, PrimaryGold.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = PrimaryGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "PATRIMONIO MUSICAL DE MÉXICO",
                                color = PrimaryGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Textos del Hero
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "DOLORES HIDALGO, GUANAJUATO",
                        color = PrimaryGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vida y Obra",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "del Rey",
                        color = PrimaryGold,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // ─── 3. REPRODUCTOR DE CITA DE VOZ INTERACTIVO ──────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF172019)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isAudioPlaying) PrimaryGold else Color(0xFF26362A))
                                .clickable {
                                    isAudioPlaying = !isAudioPlaying
                                    Toast.makeText(
                                        context,
                                        if (isAudioPlaying) "▶ Reproduciendo cita histórica de voz" else "⏸ Audio pausado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Reproducir",
                                tint = if (isAudioPlaying) Color.Black else PrimaryGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CITA CÉLEBRE DEL MAESTRO",
                                color = PrimaryGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isAudioPlaying) "Voz original • Audio remasterizado" else "Toca para escuchar",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }

                        // Visualizador de ondas de sonido animadas
                        if (isAudioPlaying) {
                            AnimatedSoundWaves()
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = biografia?.citaCelebre ?: "\"No tengo trono ni reina, ni nadie que me comprenda, pero sigo siendo el Rey.\"",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ─── 4. RESUMEN BIOGRÁFICO DE PRESENTACIÓN ──────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    text = biografia?.descripcion
                        ?: "José Alfredo Jiménez Sandoval (Dolores Hidalgo, 1926 – Ciudad de México, 1973) es el compositor más trascendental de la música vernácula mexicana. Sus canciones capturaron el corazón, el despecho, el amor y la bravura de todo un pueblo sin haber cursado estudios formales de música.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── 5. DISCOGRAFÍA ─────────────────────────────────────────────────────
            DiscographySection()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Sección de discografía legendaria con reproductores interactivos.
 */
@Composable
fun DiscographySection() {
    val context = LocalContext.current
    var playingAlbum by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "DISCOGRAFÍA Y GRABACIONES MAESTRAS",
            color = PrimaryGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InteractiveAlbumCard(
                title = "La Enorme Distancia",
                subtitle = "1950 • 12 Canciones",
                drawableId = R.drawable.ranchera_guitar,
                isPlaying = playingAlbum == "La Enorme Distancia",
                onClick = {
                    playingAlbum = if (playingAlbum == "La Enorme Distancia") null else "La Enorme Distancia"
                    Toast.makeText(context, "Reproduciendo muestra: La Enorme Distancia", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            )

            InteractiveAlbumCard(
                title = "El Camino de la Noche",
                subtitle = "1954 • 10 Canciones",
                drawableId = R.drawable.mariachi_gala_stage,
                isPlaying = playingAlbum == "El Camino de la Noche",
                onClick = {
                    playingAlbum = if (playingAlbum == "El Camino de la Noche") null else "El Camino de la Noche"
                    Toast.makeText(context, "Reproduciendo muestra: El Camino de la Noche", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InteractiveAlbumCard(
                title = "El Rey (Edición Oro)",
                subtitle = "1968 • 14 Canciones",
                drawableId = R.drawable.jose_alfredo_portrait,
                isPlaying = playingAlbum == "El Rey (Edición Oro)",
                onClick = {
                    playingAlbum = if (playingAlbum == "El Rey (Edición Oro)") null else "El Rey (Edición Oro)"
                    Toast.makeText(context, "Reproduciendo muestra: El Rey", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            )

            InteractiveAlbumCard(
                title = "Caminos de Guanajuato",
                subtitle = "1953 • 12 Canciones",
                drawableId = R.drawable.hero_dolores_hidalgo,
                isPlaying = playingAlbum == "Caminos de Guanajuato",
                onClick = {
                    playingAlbum = if (playingAlbum == "Caminos de Guanajuato") null else "Caminos de Guanajuato"
                    Toast.makeText(context, "Reproduciendo muestra: Caminos de Guanajuato", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InteractiveAlbumCard(
    title: String,
    subtitle: String,
    drawableId: Int,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141C16)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPlaying) PrimaryGold else Color(0xFF28382C)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Image(
                    painter = painterResource(id = drawableId),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(34.dp)
                        .background(if (isPlaying) PrimaryGold else Color.Black.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isPlaying) Color.Black else PrimaryGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = Color.LightGray, fontSize = 11.sp)
        }
    }
}

/**
 * Visualizador de ondas de sonido animadas para el reproductor de voz.
 */
@Composable
fun AnimatedSoundWaves() {
    val infiniteTransition = rememberInfiniteTransition(label = "waves")
    val height1 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val height2 by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val height3 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(height1.dp).background(PrimaryGold, RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.width(3.dp).height(height2.dp).background(PrimaryGold, RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.width(3.dp).height(height3.dp).background(PrimaryGold, RoundedCornerShape(2.dp)))
    }
}
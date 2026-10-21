package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.R
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.EventosViewModel

@Composable
fun DashboardScreen(
    eventosViewModel: EventosViewModel? = null,
    onNavigateToTickets: () -> Unit = {},
    onNavigateToLive: (String) -> Unit = {},
    onNavigateToBiography: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var selectedEventDetail by remember { mutableStateOf<String?>(null) }
    
    val eventos by eventosViewModel?.eventosLocales?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // Hero Section / Banner with Local Parroquia de Dolores Hidalgo Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            // Local resource: Parroquia de Dolores Hidalgo (Guanajuato)
            Image(
                painter = painterResource(id = R.drawable.hero_dolores_hidalgo),
                contentDescription = "Parroquia de Dolores Hidalgo de Noche",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = "DOLORES HIDALGO, CUNA DE LA INDEPENDENCIA",
                    color = PrimaryGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "El Rey\nVive en Su\nTierra",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 44.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onNavigateToTickets,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("COMPRAR BOLETOS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Access Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Biography Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .clickable { onNavigateToBiography() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.jose_alfredo_portrait),
                        contentDescription = "Biografía",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xEE141D17))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text("📚 Biografía", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Conoce al Maestro", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp)
                    }
                }
            }

            // Audio Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.ranchera_guitar),
                        contentDescription = "Audio",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xEE141D17))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text("🎵 Audio", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Discografía Completa", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ⭐ BANNER DE ACCESO DIRECTO: GALERÍA INTERACTIVA (Módulo Biografía)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable { onNavigateToBiography() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162219)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.jose_alfredo_portrait),
                        contentDescription = "Galería Interactiva",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⭐ BIOGRAFÍA",
                            color = PrimaryGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "El Legado de José Alfredo",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vida, obra y discografía del Rey",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                Text("EXPLORAR ➜", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 💬 ACCESO DIRECTO: FORO / GALERÍA DE LA COMUNIDAD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable { onNavigateToGallery() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162219)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.gal_banner_festival),
                    contentDescription = "Comunidad del festival",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "COMUNIDAD",
                        color = PrimaryGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Comparte tu experiencia",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fotos, recomendaciones y anécdotas de los asistentes",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
                Text("VER ➜", color = PrimaryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Upcoming Event Section
        Text(
            text = "PRÓXIMO EVENTO",
            color = PrimaryGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        val displayEventTitle = eventos.firstOrNull()?.nombre ?: "Gran Gala Mariachi"
        val displayEventLocation = eventos.firstOrNull()?.ubicacion ?: "23 de Noviembre, 20:00 hrs • Escenario Principal"

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clickable { selectedEventDetail = displayEventTitle },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2720)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mariachi_gala_stage),
                        contentDescription = "Mariachi Gala",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayEventTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(displayEventLocation, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
                Text(">", color = PrimaryGold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Live stream option
        Button(
            onClick = {
                val eId = eventos.firstOrNull()?.id ?: "EVT-001"
                onNavigateToLive(eId)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFC51111), // Red for live
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp)
        ) {
            Text("▶ VER EN VIVO AHORA", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Interactive Dialog when clicking event
    if (selectedEventDetail != null) {
        AlertDialog(
            onDismissRequest = { selectedEventDetail = null },
            title = { Text(selectedEventDetail!!, color = PrimaryGold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Evento oficial del Festival José Alfredo Jiménez en Dolores Hidalgo Guanajuato. ¡Compra tus accesos o consulta la ubicación en el mapa!",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedEventDetail = null
                        onNavigateToTickets()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                ) {
                    Text("COMPRAR BOLETOS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedEventDetail = null }) {
                    Text("CERRAR", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E2720)
        )
    }
}

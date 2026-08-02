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
    onNavigateToTickets: () -> Unit = {}
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
                    .height(130.dp),
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
        
        Spacer(modifier = Modifier.height(28.dp))

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

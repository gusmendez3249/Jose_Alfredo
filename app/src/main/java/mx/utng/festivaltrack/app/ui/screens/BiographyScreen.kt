package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

@Composable
fun BiographyScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // Hero Image Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.DarkGray)
        ) {
            // Text overlays
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = "EL REY DE LA CANCIÓN",
                    color = PrimaryGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Biografía",
                    color = PrimaryGold,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            // Quote
            Text(
                text = "\"No tengo trono ni reina,\nni nadie que me\ncomprenda, pero sigo\nsiendo el Rey.\"",
                color = Color.White,
                fontSize = 20.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                lineHeight = 28.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                text = "José Alfredo Jiménez Sandoval fue un cantante y compositor mexicano de música ranchera, considerado por muchos como el mejor de la historia. Sus canciones se convirtieron en himnos del alma mexicana.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Hitos Históricos",
                color = PrimaryGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Timeline Items
            TimelineItem(
                year = "1926",
                title = "El Nacimiento en Dolores Hidalgo",
                description = "Nace el 19 de enero en la Cuna de la Independencia, Guanajuato. Desde pequeño mostró la sensibilidad que definiría su arte."
            )
            TimelineItem(
                year = "1948",
                title = "Su Primer Gran Éxito",
                description = "Andrés Huesca graba su primera canción, marcando el inicio de una carrera meteórica que cambiaría la música regional para siempre."
            )
            TimelineItem(
                year = "1950s",
                title = "La Época de Oro",
                description = "Consolidación como la máxima figura de la composición ranchera, participando en cine y radio, llenando corazones con despecho y pasión."
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Discography Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discografía Destacada",
                    color = PrimaryGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ver todo",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Discography items (Horizontal Scroll or just Row for now)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DiscographyCard(title = "La Enorme Distancia", subtitle = "1950 • 12 Canciones", modifier = Modifier.weight(1f))
                DiscographyCard(title = "El Camino de la Noche", subtitle = "1954 • 10 Canciones", modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TimelineItem(year: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Bullet
        Box(
            modifier = Modifier
                .padding(top = 4.dp, end = 16.dp)
                .size(8.dp)
                .background(PrimaryGold, RoundedCornerShape(50))
        )
        
        // Content card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2720)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(year, color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
fun DiscographyCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.DarkGray, RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.Gray, fontSize = 10.sp)
    }
}

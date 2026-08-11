package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.R
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.viewmodels.ArtistViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun BiographyScreen(viewModel: ArtistViewModel = viewModel()) {
    val scrollState = rememberScrollState()
    val biografia by viewModel.biografia.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // Hero Image: Iconic Jose Alfredo Jimenez Portrait
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.jose_alfredo_portrait),
                contentDescription = "José Alfredo Jiménez Portrait",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay matching PDF Fig 4
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Text overlays
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = "EL REY DE LA CANCIÓN",
                    color = PrimaryGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Biografía",
                    color = PrimaryGold,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.padding(32.dp))
            }
        } else if (biografia != null) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Quote
                Text(
                    text = biografia!!.citaCelebre ?: "\"No tengo trono ni reina,\nni nadie que me\ncomprenda, pero sigo\nsiendo el Rey.\"",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 28.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description
                Text(
                    text = biografia!!.descripcion,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Hitos Históricos",
                color = PrimaryGold,
                fontSize = 13.sp,
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ver todo",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Discography items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DiscographyCard(
                    title = "La Enorme Distancia",
                    subtitle = "1950 • 12 Canciones",
                    drawableId = R.drawable.ranchera_guitar,
                    modifier = Modifier.weight(1f)
                )
                DiscographyCard(
                    title = "El Camino de la Noche",
                    subtitle = "1954 • 10 Canciones",
                    drawableId = R.drawable.mariachi_gala_stage,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        } // End of if (biografia != null)
    }
}

@Composable
fun TimelineItem(year: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp, end = 16.dp)
                .size(8.dp)
                .background(PrimaryGold, RoundedCornerShape(50))
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2720)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(year, color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
fun DiscographyCard(title: String, subtitle: String, drawableId: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.Gray, fontSize = 10.sp)
    }
}

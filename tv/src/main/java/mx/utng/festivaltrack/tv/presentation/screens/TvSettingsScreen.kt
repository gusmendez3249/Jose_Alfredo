package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.presentation.components.SidebarMenuItem
import mx.utng.festivaltrack.tv.ui.theme.FestivalCardBg
import mx.utng.festivaltrack.tv.ui.theme.FestivalDarkBg
import mx.utng.festivaltrack.tv.ui.theme.FestivalGold

@Composable
fun TvSettingsScreen(
    currentNavIndex: Int,
    onNavSelect: (Int) -> Unit,
    onLogout: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FestivalDarkBg)
    ) {
        // SIDEBAR
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(Color(0xFF141A17))
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "FESTIVAL",
                    color = FestivalGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "José Alfredo Jiménez",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(32.dp))

                val navItems = listOf(
                    "Inicio" to Icons.Default.Home,
                    "Galería Histórica" to Icons.Default.Collections,
                    "Transmisión En Vivo" to Icons.Default.LiveTv,
                    "Programación" to Icons.Default.Event,
                    "Ajustes" to Icons.Default.Settings
                )

                navItems.forEachIndexed { index, (label, icon) ->
                    val isSelected = currentNavIndex == index
                    SidebarMenuItem(
                        label = label,
                        icon = icon,
                        isSelected = isSelected,
                        onClick = { onNavSelect(index) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // MAIN CONTENT AREA
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Ajustes del Dispositivo Smart TV", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsCard(
                    icon = Icons.Default.Dns,
                    title = "Servidor Backend & API",
                    subtitle = "http://10.0.2.2:3001/api/v1 (Conectado / Saludable)"
                )
                SettingsCard(
                    icon = Icons.Default.HighQuality,
                    title = "Calidad de Transmisión RTSP",
                    subtitle = "Automática (1080p 60fps / H.264 ExoPlayer)"
                )
                SettingsCard(
                    icon = Icons.Default.Tv,
                    title = "Dispositivo Smart TV",
                    subtitle = "Android TV Leanback OS (Conexión Emulador 10.0.2.2:1935)"
                )
                SettingsCard(
                    icon = Icons.Default.VolumeUp,
                    title = "Modo de Audio",
                    subtitle = "Estéreo En Vivo / Dolby Digital Surround"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC51111),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.4f)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión en Smart TV", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(icon: ImageVector, title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FestivalCardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = FestivalGold, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

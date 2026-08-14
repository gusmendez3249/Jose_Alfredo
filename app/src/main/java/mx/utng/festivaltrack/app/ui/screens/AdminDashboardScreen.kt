package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToLivePanel: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1410)) // Dark slightly green background
            .verticalScroll(scrollState)
    ) {
        // Top App Bar like
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("José Alfredo", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                Text("Jiménez", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
            }
            IconButton(
                onClick = onNavigateToUsers,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2720))
                    .border(1.dp, PrimaryGold, CircleShape)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Manage Users", tint = PrimaryGold, modifier = Modifier.size(20.dp))
            }
        }
        
        HorizontalDivider(color = Color(0xFF2A3A2C))

        // Welcome Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F23)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("¡Bienvenido,\nAdmin!", color = PrimaryGold, fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "El legado de El Rey sigue vivo. Gestiona el contenido del festival y mantén la llama de la música regional ardiendo.",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // Stream Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("STATUS DEL STREAM", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF8B5A5A)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("En Vivo: Dolores Hidalgo", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("1,240 espectadores ahora mismo.", color = Color.Gray, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ACCESO RÁPIDO VIP", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // Content Summary
        Text(
            "Resumen de\nContenido",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricBox(icon = Icons.Default.MusicNote, count = "148", label = "Canciones\nTotales", modifier = Modifier.weight(1f))
            MetricBox(icon = Icons.Default.PhotoLibrary, count = "3.2k", label = "Fotos en\nGalería", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricBox(icon = Icons.Default.Visibility, count = "24.5k", label = "Vistas Totales", modifier = Modifier.weight(1f))
            MetricBox(icon = Icons.Default.CloudUpload, count = "85%", label = "Venta de\nBoletos", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Fast Management
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gestión\nRápida", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .border(1.dp, PrimaryGold, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Admin\nVerificado", color = PrimaryGold, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        ManagementCard(title = "Nuevos Lanzamientos", desc = "Sube archivos de audio, letras y metadatos para la colección oficial del festival.", btn = "GESTIONAR MÚSICA")
        Spacer(modifier = Modifier.height(16.dp))
        ManagementCard(title = "Galería de Eventos", desc = "Añade fotos del último concierto y organiza los álbumes por fecha y artista.", btn = "EDITAR GALERÍA")
        Spacer(modifier = Modifier.height(16.dp))
        ManagementCard(
            title = "Control de Stream", 
            desc = "Configura las claves de transmisión y monitorea la salud del stream en tiempo real.", 
            btn = "IR AL PANEL LIVE",
            onClick = onNavigateToLivePanel
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Recent Activity
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Actividad Reciente", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Ver Todo", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                ActivityItem(icon = Icons.Default.CloudUpload, iconColor = Color(0xFF4CAF50), title = "Nueva canción subida: \"El Rey\" (Remix Festival)", time = "Hace 15 minutos • por Admin Principal")
                ActivityItem(icon = Icons.Default.PersonAdd, iconColor = Color.Gray, title = "Nuevo usuario registrado: Mariachi Juvenil Real", time = "Hace 2 horas • Registro Automático")
                ActivityItem(icon = Icons.Default.Warning, iconColor = Color.Red, title = "Alerta: Intento de acceso fallido en servidor Stream", time = "Hace 4 horas • IP: 192.168.1.104", hideLine = true)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MetricBox(icon: ImageVector, count: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171A18)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF1E2720), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF8AA694), modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(count, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun ManagementCard(title: String, desc: String, btn: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171A18)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(desc, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGold),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(btn)
            }
        }
    }
}

@Composable
fun ActivityItem(icon: ImageVector, iconColor: Color, title: String, time: String, hideLine: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            if (!hideLine) {
                Box(modifier = Modifier.width(2.dp).height(40.dp).background(iconColor.copy(alpha = 0.3f)))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(time, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

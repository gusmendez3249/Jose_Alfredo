package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

import mx.utng.festivaltrack.app.ui.viewmodels.AdminManageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageScreen(
    viewModel: AdminManageViewModel,
    onNavigateToCreateEvent: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Eventos") }
    
    val eventos by viewModel.eventos.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1410))
                .verticalScroll(scrollState)
        ) {
            // Top App Bar like
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = PrimaryGold)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("José Alfredo", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                    Text("Jiménez", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2720))
                        .border(1.dp, PrimaryGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Profile", tint = PrimaryGold, modifier = Modifier.size(20.dp))
                }
            }
            
            HorizontalDivider(color = Color(0xFF2A3A2C))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Gestionar Contenido",
                color = PrimaryGold,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar canciones o imágenes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF171A18),
                    unfocusedContainerColor = Color(0xFF171A18),
                    focusedBorderColor = Color(0xFF2A2A2A),
                    unfocusedBorderColor = Color(0xFF2A2A2A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { FilterChipItem("Eventos", selectedFilter == "Eventos") { selectedFilter = "Eventos" } }
                item { FilterChipItem("Todos", selectedFilter == "Todos") { selectedFilter = "Todos" } }
                item { FilterChipItem("Canciones", selectedFilter == "Canciones") { selectedFilter = "Canciones" } }
                item { FilterChipItem("Galería", selectedFilter == "Galería") { selectedFilter = "Galería" } }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // List Items
            if (selectedFilter == "Eventos") {
                if (eventos.isEmpty()) {
                    Text("No hay eventos disponibles.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    eventos.forEach { evento ->
                        ManageItemCard(
                            status = evento.estado,
                            isDraft = evento.estado == "BORRADOR",
                            title = evento.nombre,
                            subtitle = "${evento.ubicacion} • ${evento.fechaHora}",
                            iconType = "event",
                            onDelete = { viewModel.deleteEvent(evento.id) }
                        )
                    }
                }
            } else {
                ManageItemCard(
                    status = "PUBLICADO",
                    isDraft = false,
                    title = "El Rey",
                    subtitle = "Mariachi Clásico • 3:24",
                    iconType = "music",
                    onDelete = {}
                )
                ManageItemCard(
                    status = "BORRADOR",
                    isDraft = true,
                    title = "Festival...",
                    subtitle = "Galería • 4.2 MB",
                    iconType = "image",
                    onDelete = {}
                )
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
        }
        
        // FAB
        FloatingActionButton(
            onClick = onNavigateToCreateEvent,
            containerColor = PrimaryGold,
            contentColor = Color.Black,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 16.dp)
                .size(64.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun FilterChipItem(label: String, isSelected: Boolean, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) PrimaryGold else Color(0xFF2A2A2A),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.LightGray,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ManageItemCard(
    status: String,
    isDraft: Boolean,
    title: String,
    subtitle: String,
    iconType: String,
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171A18)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (iconType == "music") Icons.Default.MusicNote else Icons.Default.Image,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // Status badge
                Box(
                    modifier = Modifier
                        .background(if (isDraft) Color(0xFF3A3A3A) else Color(0xFF0F3D14), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        status,
                        color = if (isDraft) Color.LightGray else PrimaryGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (iconType == "music") Icons.Default.MusicNote else Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(subtitle, color = Color.LightGray, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryGold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE57373), modifier = Modifier.size(24.dp).clickable { onDelete() })
        }
    }
}

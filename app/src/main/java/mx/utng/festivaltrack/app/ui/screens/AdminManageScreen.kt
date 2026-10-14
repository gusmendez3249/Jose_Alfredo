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
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.data.factory.BiographyEntryType
import mx.utng.festivaltrack.app.data.factory.BiographyFactoryProducer
import mx.utng.festivaltrack.app.data.firebase.BiographyFirestoreService
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

import mx.utng.festivaltrack.app.ui.viewmodels.AdminManageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageScreen(
    viewModel: AdminManageViewModel,
    onNavigateToCreateEvent: () -> Unit = {},
    onEditEvent: (mx.utng.festivaltrack.shared.data.local.entity.EventoEntity) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Eventos") }
    var showCreateBioDialog by remember { mutableStateOf(false) }
    
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
                item { FilterChipItem("Biografía (Factory)", selectedFilter == "Biografía") { selectedFilter = "Biografía" } }
                item { FilterChipItem("Todos", selectedFilter == "Todos") { selectedFilter = "Todos" } }
                item { FilterChipItem("Canciones", selectedFilter == "Canciones") { selectedFilter = "Canciones" } }
                item { FilterChipItem("Galería", selectedFilter == "Galería") { selectedFilter = "Galería" } }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val context = androidx.compose.ui.platform.LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val api = remember { mx.utng.festivaltrack.shared.data.remote.FestivalApiService.create() }
            var cancionesList by remember { mutableStateOf<List<mx.utng.festivaltrack.shared.data.remote.CancionDto>>(emptyList()) }
            var imagenesList by remember { mutableStateOf<List<mx.utng.festivaltrack.shared.data.remote.ImagenDto>>(emptyList()) }

            fun reloadData() {
                coroutineScope.launch {
                    try {
                        cancionesList = api.getCanciones()
                        val galerias = api.getGalerias()
                        imagenesList = galerias.flatMap { it.imagenes }
                    } catch (e: Exception) {}
                }
            }

            LaunchedEffect(Unit) {
                reloadData()
            }

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
                            onEdit = { onEditEvent(evento) },
                            onDelete = {
                                val token = mx.utng.festivaltrack.app.data.TokenManager(context).getToken()
                                viewModel.deleteEvent(token, evento.id)
                            }
                        )
                    }
                }
            } else if (selectedFilter == "Canciones") {
                if (cancionesList.isEmpty()) {
                    Text("No hay canciones registradas en el catálogo.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    cancionesList.forEach { cancion ->
                        ManageItemCard(
                            status = "PUBLICADO",
                            isDraft = false,
                            title = cancion.titulo,
                            subtitle = "${cancion.artista} • ${cancion.duracion}s",
                            iconType = "music",
                            onEdit = {},
                            onDelete = {
                                coroutineScope.launch {
                                    try {
                                        api.deleteCancion(cancion.id)
                                        android.widget.Toast.makeText(context, "Canción eliminada del catálogo", android.widget.Toast.LENGTH_SHORT).show()
                                        reloadData()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Error al eliminar canción", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            } else if (selectedFilter == "Biografía") {
                val firestoreService = remember { BiographyFirestoreService.getInstance(context) }
                val bioItems by firestoreService.itemsFlow.collectAsState()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { showCreateBioDialog = true },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2720)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alta de Biografía (Factory Pattern)", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Crear hito, foto en Storage o curiosidad", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Text(
                            "ALTA +",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .background(PrimaryGold, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (bioItems.isEmpty()) {
                    Text("No hay elementos biográficos registrados.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    bioItems.forEach { item ->
                        ManageItemCard(
                            status = "FIRESTORE",
                            isDraft = false,
                            title = item.titulo,
                            subtitle = "${item.anoOEepoca} • ${item.categoria}",
                            iconType = "image",
                            onEdit = {},
                            onDelete = {
                                firestoreService.deleteBiographyItem(item.id)
                                android.widget.Toast.makeText(context, "Elemento eliminado de Firestore", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            } else if (selectedFilter == "Galería") {
                if (imagenesList.isEmpty()) {
                    Text("No hay fotos en la galería.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    imagenesList.forEach { imagen ->
                        ManageItemCard(
                            status = "PUBLICADO",
                            isDraft = false,
                            title = imagen.titulo ?: "Foto de Galería",
                            subtitle = "Galería del Festival",
                            iconType = "image",
                            onEdit = {},
                            onDelete = {
                                coroutineScope.launch {
                                    try {
                                        api.deleteImagenGaleria(imagen.id)
                                        android.widget.Toast.makeText(context, "Imagen eliminada de la galería", android.widget.Toast.LENGTH_SHORT).show()
                                        reloadData()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Error al eliminar imagen", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                // Todos (Canciones + Galería)
                cancionesList.forEach { cancion ->
                    ManageItemCard(
                        status = "PUBLICADO",
                        isDraft = false,
                        title = cancion.titulo,
                        subtitle = "Canción • ${cancion.artista}",
                        iconType = "music",
                        onEdit = {},
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    api.deleteCancion(cancion.id)
                                    android.widget.Toast.makeText(context, "Canción eliminada", android.widget.Toast.LENGTH_SHORT).show()
                                    reloadData()
                                } catch (e: Exception) {}
                            }
                        }
                    )
                }
                imagenesList.forEach { imagen ->
                    ManageItemCard(
                        status = "PUBLICADO",
                        isDraft = false,
                        title = imagen.titulo ?: "Foto de Galería",
                        subtitle = "Imagen de Galería",
                        iconType = "image",
                        onEdit = {},
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    api.deleteImagenGaleria(imagen.id)
                                    android.widget.Toast.makeText(context, "Imagen eliminada", android.widget.Toast.LENGTH_SHORT).show()
                                    reloadData()
                                } catch (e: Exception) {}
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
        }
        
        // FAB
        FloatingActionButton(
            onClick = {
                if (selectedFilter == "Biografía") {
                    showCreateBioDialog = true
                } else {
                    onNavigateToCreateEvent()
                }
            },
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

        if (showCreateBioDialog) {
            AdminCreateBiographyDialog(
                onDismiss = { showCreateBioDialog = false }
            )
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
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onEdit() },
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
            
            Icon(
                Icons.Default.Edit,
                contentDescription = "Editar",
                tint = PrimaryGold,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onEdit() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = Color(0xFFE57373),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onDelete() }
            )
        }
    }
}

/**
 * Diálogo de Alta de Biografías por el Administrador implementando el FACTORY PATTERN.
 */
@Composable
fun AdminCreateBiographyDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val api = remember { mx.utng.festivaltrack.shared.data.remote.FestivalApiService.create() }
    val firestoreService = remember { BiographyFirestoreService.getInstance(context) }

    var selectedType by remember { mutableStateOf(BiographyEntryType.HISTORICAL_MILESTONE) }
    var titulo by remember { mutableStateOf("") }
    var anoOEepoca by remember { mutableStateOf("1954") }
    var ubicacion by remember { mutableStateOf("Dolores Hidalgo, Gto.") }
    var descripcion by remember { mutableStateOf("") }
    var citaCelebre by remember { mutableStateOf("") }
    var imagenUrl by remember { mutableStateOf("gs://festival-josealfredo.appspot.com/biografias/foto_archivo.jpg") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161F18),
        title = {
            Column {
                Text(
                    "ALTA DE BIOGRAFÍA (FACTORY)",
                    color = PrimaryGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    "Crear Entrada Histórica",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Tipo de Elemento (Factory):", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isMilestone = selectedType == BiographyEntryType.HISTORICAL_MILESTONE
                    Button(
                        onClick = { selectedType = BiographyEntryType.HISTORICAL_MILESTONE },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMilestone) PrimaryGold else Color(0xFF263328),
                            contentColor = if (isMilestone) Color.Black else Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("Hito", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    val isPhoto = selectedType == BiographyEntryType.ARCHIVE_PHOTO
                    Button(
                        onClick = { selectedType = BiographyEntryType.ARCHIVE_PHOTO },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPhoto) PrimaryGold else Color(0xFF263328),
                            contentColor = if (isPhoto) Color.Black else Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("Storage", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    val isCuriosity = selectedType == BiographyEntryType.ANECDOTE_CURIOSITY
                    Button(
                        onClick = { selectedType = BiographyEntryType.ANECDOTE_CURIOSITY },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCuriosity) PrimaryGold else Color(0xFF263328),
                            contentColor = if (isCuriosity) Color.Black else Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("Anécdota", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del Hito o Fotografía") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = anoOEepoca,
                        onValueChange = { anoOEepoca = it },
                        label = { Text("Año / Época") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = ubicacion,
                        onValueChange = { ubicacion = it },
                        label = { Text("Ubicación") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.weight(1.4f)
                    )
                }

                OutlinedTextField(
                    value = citaCelebre,
                    onValueChange = { citaCelebre = it },
                    label = { Text("Cita Célebre (Opcional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción Histórica Detallada") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Gray
                    ),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = imagenUrl,
                    onValueChange = { imagenUrl = it },
                    label = { Text("Storage URL / Imagen") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isBlank() || descripcion.isBlank()) {
                        android.widget.Toast.makeText(context, "Completa título y descripción", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        // 1. Invocar Factory Pattern
                        val createdItem = BiographyFactoryProducer.createAdminEntry(
                            entryType = selectedType,
                            titulo = titulo,
                            anoOEpoca = anoOEepoca,
                            descripcion = descripcion,
                            imagenUrl = imagenUrl,
                            citaCelebre = citaCelebre.takeIf { it.isNotBlank() },
                            ubicacion = ubicacion
                        )

                        // 2. Guardar en servicio de Firestore
                        firestoreService.registerBiographyItem(createdItem)

                        // 3. Notificar a backend
                        coroutineScope.launch {
                            try {
                                api.altaBiografia(
                                    request = mx.utng.festivaltrack.shared.data.remote.BiografiaAltaDto(
                                        titulo = createdItem.titulo,
                                        descripcion = createdItem.descripcion,
                                        citaCelebre = createdItem.citaCelebre,
                                        anoOEpoca = createdItem.anoOEepoca,
                                        imagenUrl = createdItem.storageImageUrl,
                                        tipo = createdItem.entryType.name
                                    )
                                )
                            } catch (e: Exception) {}
                        }

                        android.widget.Toast.makeText(
                            context,
                            "¡Alta exitosa con Factory! Visible en la Galería Interactiva.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
            ) {
                Text("CREAR CON FACTORY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

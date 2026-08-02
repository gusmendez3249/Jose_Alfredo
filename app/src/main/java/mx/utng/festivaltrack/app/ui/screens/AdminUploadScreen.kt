package mx.utng.festivaltrack.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUploadScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var isUploading by remember { mutableStateOf(false) }

    // Native Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            Toast.makeText(context, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
        }
    }

    // Native Audio Picker Launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedAudioUri = uri
        if (uri != null) {
            Toast.makeText(context, "Archivo de audio seleccionado", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1410))
            .verticalScroll(scrollState)
    ) {
        // Top App Bar
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
            "Subir Nueva Canción / Archivo",
            color = PrimaryGold,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Inmortaliza tu talento en el catálogo regional.",
            color = Color.LightGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Image Upload Box (Clickable Launcher)
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(180.dp)
                .background(Color(0xFF2A3A2C), RoundedCornerShape(16.dp))
                .border(1.dp, if (selectedImageUri != null) PrimaryGold else PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (selectedImageUri != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                    contentDescription = "Upload Photo",
                    tint = PrimaryGold,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (selectedImageUri != null) "Imagen Lista ✓" else "Portada del Álbum",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "TOCA PARA SELECCIONAR DE TU GALERÍA",
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Form
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("TÍTULO DE LA CANCIÓN", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Ej: El Rey del Mariachi") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF171A18),
                    unfocusedContainerColor = Color(0xFF171A18),
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color(0xFF2A2A2A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("ARTISTA / INTÉRPRETE", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                placeholder = { Text("Nombre de la agrupación o solista") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF171A18),
                    unfocusedContainerColor = Color(0xFF171A18),
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color(0xFF2A2A2A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Audio File Section Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF334D41)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).background(PrimaryGold.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = "Audio", tint = PrimaryGold, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (selectedAudioUri != null) "Audio Seleccionado ✓" else "Archivo de Audio",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            selectedAudioUri?.lastPathSegment ?: "MP3 / WAV (Max 50MB)",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(if (selectedAudioUri != null) "CAMBIAR" else "SELECCIONAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (isUploading || uploadProgress > 0f) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subiendo archivo al servidor...", color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${(uploadProgress * 100).toInt()}%", color = PrimaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = uploadProgress,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = PrimaryGold,
                    trackColor = Color(0xFF2A2A2A)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Publish Action Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Por favor ingresa un título", Toast.LENGTH_SHORT).show()
                    } else {
                        isUploading = true
                        uploadProgress = 1.0f
                        Toast.makeText(context, "¡Publicado exitosamente en el catálogo!", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("PUBLICAR EN EL CATÁLOGOS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

import mx.utng.festivaltrack.app.ui.viewmodels.AdminManageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCreateEventScreen(
    eventId: String? = null,
    initialTitle: String = "",
    initialDate: String = "",
    initialLocation: String = "",
    initialPrice: String = "",
    viewModel: AdminManageViewModel? = null,
    onNavigateBack: () -> Unit = {}
) {
    var title by remember { mutableStateOf(initialTitle) }
    var date by remember { mutableStateOf(initialDate) }
    var location by remember { mutableStateOf(initialLocation) }
    var price by remember { mutableStateOf(initialPrice) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    val fieldColor = Color(0xFF1E2720)
    val isEditing = !eventId.isNullOrEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Evento" else "Crear Evento", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Button(
                        onClick = {
                            val token = mx.utng.festivaltrack.app.data.TokenManager(context).getToken()
                            viewModel?.saveEvent(
                                token = token,
                                id = eventId,
                                title = title,
                                date = date,
                                location = location,
                                price = price
                            )
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Save, contentDescription = "Guardar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEditing) "GUARDAR CAMBIOS" else "GUARDAR EVENTO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Detalles del Evento", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título del Evento") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedLabelColor = PrimaryGold,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Fecha (Ej. 24 Nov 2024)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedLabelColor = PrimaryGold,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Lugar (Ej. Dolores Hidalgo)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedLabelColor = PrimaryGold,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Precio Base (MXN)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedLabelColor = PrimaryGold,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

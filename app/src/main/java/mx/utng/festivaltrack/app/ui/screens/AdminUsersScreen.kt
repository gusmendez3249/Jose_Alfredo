package mx.utng.festivaltrack.app.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.AdminUsersState
import mx.utng.festivaltrack.app.ui.viewmodels.AdminUsersViewModel
import mx.utng.festivaltrack.shared.data.remote.UsuarioDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: AdminUsersViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    )

    val uiState by viewModel.uiState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            // Note: In a real app we'd use a Snackbar, but for simplicity we just clear it after a delay
            kotlinx.coroutines.delay(3000)
            viewModel.clearActionMessage()
        }
    }

    if (showCreateDialog) {
        CreateAdminDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { nombre, correo, pass ->
                viewModel.registerAdmin(nombre, correo, pass)
                showCreateDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Usuarios", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = PrimaryGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1410))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryGold,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Crear Administrador")
            }
        },
        containerColor = Color(0xFF0F1410)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is AdminUsersState.Loading -> {
                    CircularProgressIndicator(color = PrimaryGold, modifier = Modifier.align(Alignment.Center))
                }
                is AdminUsersState.Error -> {
                    Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center).padding(16.dp))
                }
                is AdminUsersState.Success -> {
                    if (state.users.isEmpty()) {
                        Text("No hay usuarios registrados.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.users) { usuario ->
                                UserCard(
                                    usuario = usuario,
                                    onToggleRole = { viewModel.toggleRole(usuario.id, usuario.rol) }
                                )
                            }
                        }
                    }
                }
            }

            if (actionMessage != null) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = Color(0xFF1E1E1E),
                    contentColor = PrimaryGold
                ) {
                    Text(actionMessage!!)
                }
            }
        }
    }
}

@Composable
fun UserCard(usuario: UsuarioDto, onToggleRole: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F23)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF121212)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (usuario.rol == "ADMINISTRADOR") Icons.Default.Security else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (usuario.rol == "ADMINISTRADOR") PrimaryGold else Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(usuario.nombre, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(usuario.correo, color = Color.LightGray, fontSize = 14.sp)
            }
            
            Button(
                onClick = onToggleRole,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (usuario.rol == "ADMINISTRADOR") Color(0xFF121212) else PrimaryGold,
                    contentColor = if (usuario.rol == "ADMINISTRADOR") PrimaryGold else Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (usuario.rol == "ADMINISTRADOR") "Quitar Admin" else "Hacer Admin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAdminDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Registrar Administrador", color = PrimaryGold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre Completo") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = PrimaryGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo Electrónico") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = PrimaryGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = PrimaryGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(nombre, correo, contrasena) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
            ) {
                Text("Registrar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

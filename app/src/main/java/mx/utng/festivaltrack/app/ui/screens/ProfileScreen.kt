package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Tv
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.viewmodels.ProfileState
import mx.utng.festivaltrack.app.ui.viewmodels.ProfileViewModel
import mx.utng.festivaltrack.app.ui.viewmodels.VincularTvState
import mx.utng.festivaltrack.app.ui.viewmodels.limpiarCodigoTv

/**
 * Diálogo para entrar en la TV del festival con esta cuenta: se escribe el código que muestra la
 * TV (también se puede escanear su QR con la cámara, que abre la web). La TV entra sola en unos
 * segundos.
 */
@Composable
private fun VincularTvDialog(estado: VincularTvState, onVincular: (String) -> Unit, onCerrar: () -> Unit) {
    // TextFieldValue para dejar el cursor al final: al agregar el guion, con un String el cursor
    // se quedaba atrás y lo que se escribía después quedaba en desorden ("H89J6EBQ" → "H89J-EBQ6").
    var campo by remember { mutableStateOf(TextFieldValue("")) }
    val codigo = campo.text
    val enviando = estado is VincularTvState.Enviando
    AlertDialog(
        onDismissRequest = { if (!enviando) onCerrar() },
        containerColor = Color(0xFF1E1E1E),
        icon = { Icon(Icons.Default.Tv, contentDescription = null, tint = PrimaryGold) },
        title = { Text("Vincular una TV", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (estado is VincularTvState.Listo) {
                    Text(
                        "¡Listo! La TV entrará con tu cuenta en unos segundos.",
                        color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "Escribe el código que aparece en la pantalla de inicio de sesión de la TV, junto al QR.",
                        color = Color.Gray, fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = campo,
                        onValueChange = {
                            val limpio = limpiarCodigoTv(it.text)
                            campo = TextFieldValue(limpio, TextRange(limpio.length))
                        },
                        label = { Text("Código de la TV") },
                        placeholder = { Text("ABCD-1234") },
                        singleLine = true,
                        enabled = !enviando,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        isError = estado is VincularTvState.Error,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            focusedLabelColor = PrimaryGold,
                            cursorColor = PrimaryGold,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (estado is VincularTvState.Error) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(estado.mensaje, color = Color(0xFFFF8A80), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Hazlo solo con una TV que tengas enfrente: entrará con tu cuenta.",
                        color = Color.Gray, fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            if (estado is VincularTvState.Listo) {
                Button(
                    onClick = onCerrar,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                ) { Text("Cerrar", fontWeight = FontWeight.Bold) }
            } else {
                Button(
                    onClick = { onVincular(codigo) },
                    enabled = !enviando && codigo.length == 9,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                ) {
                    if (enviando) CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Text("Vincular", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (estado !is VincularTvState.Listo) {
                TextButton(onClick = onCerrar, enabled = !enviando) { Text("Cancelar", color = Color.Gray) }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onComprarBoletos: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val profileState by viewModel.profileState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val nombre = remember { mx.utng.festivaltrack.app.data.TokenManager(context).getUserName() }
    var confirmarSalida by remember { mutableStateOf(false) }
    var verVincularTv by remember { mutableStateOf(false) }

    if (verVincularTv) {
        VincularTvDialog(
            estado = viewModel.vincularTv.collectAsState().value,
            onVincular = viewModel::vincularTv,
            onCerrar = { verVincularTv = false; viewModel.reiniciarVincularTv() }
        )
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("¿Cerrar sesión?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Tendrás que volver a escribir tu correo y contraseña para entrar.", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = { confirmarSalida = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC51111))
                ) { Text("Cerrar sesión", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar", color = Color.Gray) }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Mi perfil", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { confirmarSalida = true }) {
                        Text("Cerrar sesión", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            nombre?.let {
                Text("Hola, $it", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Entrar en la TV del festival con esta cuenta, sin escribir la contraseña con el control.
            OutlinedButton(
                onClick = { verVincularTv = true },
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = PrimaryGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vincular una TV", color = PrimaryGold, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (profileState) {
                is ProfileState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryGold)
                    }
                }
                is ProfileState.Error -> {
                    val error = profileState as ProfileState.Error
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(error.message, color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        if (error.requiereLogin) {
                            // onLogout borra el token y lleva a la pantalla de inicio de sesión
                            Button(
                                onClick = onLogout,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                            ) { Text("Iniciar sesión", color = Color.Black, fontWeight = FontWeight.Bold) }
                        } else {
                            Button(
                                onClick = { viewModel.loadProfile() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                            ) { Text("Reintentar", color = Color.Black, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                is ProfileState.Success -> {
                    val boletos = (profileState as ProfileState.Success).boletos
                    
                    Text("Mis Boletos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (boletos.isEmpty()) {
                        Text("No has comprado ningún boleto aún.", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onComprarBoletos,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                        ) { Text("Comprar boletos", fontWeight = FontWeight.Bold) }
                    } else {
                        var selectedBoleto by remember { mutableStateOf<mx.utng.festivaltrack.shared.data.remote.BoletoDto?>(null) }
                        
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(boletos) { boleto ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBoleto = boleto },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.QrCode, 
                                            contentDescription = "QR", 
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Evento: ${boleto.evento?.nombre ?: "Festival"}", color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Categoría: ${boleto.categoria}", color = Color.Gray, fontSize = 14.sp)
                                            Text("Toca para ver el código QR", color = PrimaryGold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        selectedBoleto?.let { boleto ->
                            AlertDialog(
                                onDismissRequest = { selectedBoleto = null },
                                title = {
                                    Text("Boleto: ${boleto.evento?.nombre ?: "Festival"}", color = PrimaryGold, fontWeight = FontWeight.Bold)
                                },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        mx.utng.festivaltrack.app.ui.utils.DynamicQrCode(
                                            content = boleto.codigoQR,
                                            modifier = Modifier.size(200.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Categoría: ${boleto.categoria}", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("ID: ${boleto.id}", color = Color.Gray, fontSize = 12.sp)
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { selectedBoleto = null }) {
                                        Text("Cerrar", color = PrimaryGold)
                                    }
                                },
                                containerColor = Color(0xFF1E1E1E),
                                titleContentColor = Color.White,
                                textContentColor = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

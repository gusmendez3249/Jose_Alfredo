package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScannerScreen(
    onNavigateBack: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var scanResultMode by remember { mutableStateOf<Int?>(null) } // null = scanning, 1 = ticket approved, 2 = TV synced, 0 = rejected

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escáner QR Multiplataforma", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Apunta la cámara al código QR de tu boleto o al QR de tu Smart TV",
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Simulated Camera Viewfinder
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                    .border(
                        2.dp,
                        if (scanResultMode == null) PrimaryGold else if (scanResultMode!! > 0) Color.Green else Color.Red,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (scanResultMode) {
                    null -> Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.Gray, modifier = Modifier.size(64.dp))
                    1 -> Icon(Icons.Default.CheckCircle, contentDescription = "Boleto Aprobado", tint = Color.Green, modifier = Modifier.size(80.dp))
                    2 -> Icon(Icons.Default.Tv, contentDescription = "TV Sincronizada", tint = PrimaryGold, modifier = Modifier.size(80.dp))
                    else -> Text("❌", fontSize = 64.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            
            when (scanResultMode) {
                1 -> Text("¡Boleto Aprobado! Acceso Concedido", color = Color.Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                2 -> Text("¡Smart TV Sincronizada Exitosamente!", color = PrimaryGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                0 -> Text("Código QR Inválido", color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                else -> Text("Buscando código QR de boleto o TV...", color = Color.Gray, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Interactive Actions
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        scanResultMode = 2
                        coroutineScope.launch {
                            delay(3000)
                            scanResultMode = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escanear QR de Smart TV", fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            scanResultMode = 0
                            coroutineScope.launch {
                                delay(2000)
                                scanResultMode = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Rechazar")
                    }

                    Button(
                        onClick = {
                            scanResultMode = 1
                            coroutineScope.launch {
                                delay(2000)
                                scanResultMode = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.2f), contentColor = Color.Green),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Validar Boleto")
                    }
                }
            }
        }
    }
}

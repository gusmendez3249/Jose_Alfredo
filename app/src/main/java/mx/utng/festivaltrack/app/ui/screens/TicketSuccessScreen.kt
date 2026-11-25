package mx.utng.festivaltrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.app.ui.theme.PrimaryGold
import mx.utng.festivaltrack.app.ui.utils.DynamicQrCode
import mx.utng.festivaltrack.app.ui.viewmodels.UltimaCompra

/**
 * Confirmación de compra: muestra el QR real de cada boleto que generó el servidor
 * (el mismo que aparece en "Mis boletos" y que lee el escáner de la entrada).
 */
@Composable
fun TicketSuccessScreen(
    onNavigateHome: () -> Unit = {},
    onVerMisBoletos: () -> Unit = {}
) {
    val boletos = remember { UltimaCompra.boletos }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("¡Pago exitoso!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (boletos.size > 1) "Tus boletos están listos. Muestra cada código QR al entrar al festival."
            else "Tu boleto está listo. Muestra este código QR al entrar al festival.",
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        boletos.firstOrNull()?.evento?.nombre?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, color = PrimaryGold, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        boletos.forEach { boleto ->
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2720)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Boleto ${boleto.categoria}", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    DynamicQrCode(content = boleto.codigoQR, modifier = Modifier.size(200.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ID: ${boleto.id}", color = PrimaryGold, fontSize = 12.sp)
                }
            }
        }
        if (boletos.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Encuentra tus boletos y su código QR en \"Mis boletos\".", color = Color.White, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onVerMisBoletos,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("VER MIS BOLETOS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onNavigateHome,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("VOLVER AL INICIO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

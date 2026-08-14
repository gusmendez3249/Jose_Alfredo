package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode
import mx.utng.festivaltrack.tv.ui.theme.*

/**
 * Pantalla de inicio de sesión para Android TV.
 * Renderiza una vista dividida:
 * - A la izquierda: un código QR dinámico para permitir el inicio de sesión desde un móvil.
 * - A la derecha: un formulario tradicional con usuario y contraseña (accesible vía D-Pad).
 *
 * @param onLoginSuccess Callback que se ejecuta cuando el usuario inicia sesión exitosamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvLoginScreen(
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2C2114), FestivalDarkBg),
                    radius = 1200f
                )
            )
            .padding(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT CARD (QR & Welcome)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "FESTIVAL JOSÉ ALFREDO JIMÉNEZ",
                    color = FestivalGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bienvenido al\nFestival",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 42.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Vive la magia de José Alfredo Jiménez desde la comodidad de tu hogar. Accede a conciertos exclusivos y contenido inédito.",
                    color = FestivalTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(28.dp))

                // QR Box Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = FestivalCardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(300.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DynamicQrCode(
                            content = "https://festivaljosealfredo.mx/tv-auth",
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Inicia con tu móvil", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("ESCANEA EL CÓDIGO QR", color = FestivalGold, fontSize = 10.sp, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            // RIGHT CARD (Login Form)
            Card(
                colors = CardDefaults.cardColors(containerColor = FestivalSidebarBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(420.dp)
                    .border(1.dp, FestivalGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("CORREO ELECTRÓNICO") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = FestivalGold) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FestivalGold,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = FestivalGold,
                            unfocusedLabelColor = Color.Gray,
                            focusedContainerColor = FestivalCardBg,
                            unfocusedContainerColor = FestivalCardBg
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("CONTRASEÑA") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = FestivalGold) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FestivalGold,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = FestivalGold,
                            unfocusedLabelColor = Color.Gray,
                            focusedContainerColor = FestivalCardBg,
                            unfocusedContainerColor = FestivalCardBg
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onLoginSuccess,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FestivalGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Iniciar Sesión →", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recuperar contraseña", color = FestivalTextSecondary, fontSize = 11.sp)
                        Text("Crear cuenta", color = FestivalGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("• Dolores Hidalgo 2024 • Conectado •", color = FestivalTextSecondary, fontSize = 10.sp)
                }
            }
        }

        // BOTTOM TV D-PAD NAVIGATION HELP BAR
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("(OK) SELECCIONAR", color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("(▲/▼) NAVEGAR", color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("(BACK) REGRESAR", color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

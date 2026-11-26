package mx.utng.festivaltrack.tv.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.util.EnlacesWeb
import mx.utng.festivaltrack.tv.ui.theme.*
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode

/**
 * Inicio de sesión en la TV.
 *
 * - Entra de verdad contra el backend (antes cualquier dato dejaba pasar) y muestra los mismos
 *   mensajes que el móvil: "Correo o contraseña incorrectos", "Sin conexión…".
 * - "Continuar sin cuenta": la TV se puede ver completa como invitado; la cuenta solo hace
 *   falta para escribir en el chat.
 * - Crear cuenta: con el control es incómodo, así que un QR real abre el registro en el celular.
 *
 * @param iniciarSesion Intenta entrar; regresa null si entró o el mensaje de error.
 */
@Composable
fun TvLoginScreen(
    iniciarSesion: suspend (correo: String, contrasena: String) -> String?,
    onEntrar: () -> Unit,
    onContinuarSinCuenta: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var ayudaContrasena by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val camposColores = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = FestivalGold,
        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
        focusedLabelColor = FestivalGold,
        unfocusedLabelColor = Color.Gray,
        focusedContainerColor = FestivalCardBg,
        unfocusedContainerColor = FestivalCardBg,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = FestivalGold
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF2C2114), FestivalDarkBg), radius = 1200f))
            .padding(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bienvenida y QR para crear cuenta
            Column(modifier = Modifier.weight(1f).padding(end = 32.dp), verticalArrangement = Arrangement.Center) {
                Text("FESTIVAL JOSÉ ALFREDO JIMÉNEZ", color = FestivalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text("Bienvenido al\nFestival", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 42.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Mira la programación, la galería y las transmisiones en vivo desde Dolores Hidalgo.",
                    color = FestivalTextSecondary, fontSize = 13.sp
                )
                Spacer(Modifier.height(28.dp))
                Card(colors = CardDefaults.cardColors(containerColor = FestivalCardBg), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        DynamicQrCode(content = EnlacesWeb.registro(), modifier = Modifier.size(120.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("¿No tienes cuenta?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Escanea con tu celular para crearla", color = FestivalGold, fontSize = 12.sp)
                            Text(EnlacesWeb.registro(), color = FestivalTextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Formulario
            Card(
                colors = CardDefaults.cardColors(containerColor = FestivalSidebarBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.width(440.dp).border(1.dp, FestivalGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Iniciar sesión", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = null },
                        label = { Text("CORREO ELECTRÓNICO") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = FestivalGold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        colors = camposColores,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text("CONTRASEÑA") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = FestivalGold) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = camposColores,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = FestivalBadgeLive, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            cargando = true
                            scope.launch {
                                val resultado = iniciarSesion(email, password)
                                cargando = false
                                if (resultado == null) onEntrar() else error = resultado
                            }
                        },
                        enabled = !cargando,
                        colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (cargando) CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        else Text("Iniciar sesión", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onContinuarSinCuenta,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Continuar sin cuenta", color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { ayudaContrasena = true }) {
                        Text("¿Olvidaste tu contraseña?", color = FestivalTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("(OK) SELECCIONAR", color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("(▲/▼) NAVEGAR", color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("(BACK) REGRESAR", color = FestivalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (ayudaContrasena) {
        AlertDialog(
            onDismissRequest = { ayudaContrasena = false },
            containerColor = FestivalCardBg,
            title = { Text("¿Olvidaste tu contraseña?", color = FestivalGold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Pide al equipo del festival que restablezca tu acceso desde el panel de administración, o crea una cuenta nueva con otro correo.",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = { ayudaContrasena = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FestivalGold, contentColor = Color.Black)
                ) { Text("Entendido", fontWeight = FontWeight.Bold) }
            }
        )
    }
}

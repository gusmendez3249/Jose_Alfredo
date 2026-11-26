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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.utng.festivaltrack.shared.data.remote.CodigoTvDto
import mx.utng.festivaltrack.shared.util.EnlacesWeb
import mx.utng.festivaltrack.tv.presentation.viewmodel.VinculoTv
import mx.utng.festivaltrack.tv.ui.theme.*
import mx.utng.festivaltrack.tv.ui.utils.DynamicQrCode

/** Cada cuánto pregunta la TV si ya aprobaron su código desde el celular. */
private const val CONSULTA_MS = 3_000L

/**
 * Inicio de sesión en la TV.
 *
 * - **Con el celular (recomendado):** la TV pide un código y lo muestra en grande y en un QR.
 *   El QR abre la web en el celular (si no hay sesión pide entrar o crear cuenta) y ahí se
 *   confirma; también se puede escribir el código en la app (Perfil → Vincular TV). La TV
 *   pregunta cada 3 s y entra sola en cuanto lo aprueban. El código dura 5 minutos y se
 *   renueva solo.
 * - **Con correo y contraseña** usando el control, con los mismos mensajes que el móvil.
 * - "Continuar sin cuenta": la TV se puede ver completa como invitado; la cuenta solo hace
 *   falta para el chat y Mis boletos.
 *
 * @param iniciarSesion Intenta entrar; regresa null si entró o el mensaje de error.
 * @param pedirCodigo Pide un código para entrar con el celular (null = sin conexión).
 * @param consultarCodigo Pregunta por el código con su token; con [VinculoTv.Aprobado] ya hay sesión.
 */
@Composable
fun TvLoginScreen(
    iniciarSesion: suspend (correo: String, contrasena: String) -> String?,
    pedirCodigo: suspend () -> CodigoTvDto?,
    consultarCodigo: suspend (tvToken: String) -> VinculoTv,
    onEntrar: () -> Unit,
    onContinuarSinCuenta: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var ayudaContrasena by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Código para entrar con el celular: se pide al abrir, se consulta cada 3 s y se renueva al vencer.
    var codigoTv by remember { mutableStateOf<CodigoTvDto?>(null) }
    var sinConexionCodigo by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val codigo = pedirCodigo()
            codigoTv = codigo
            sinConexionCodigo = codigo == null
            if (codigo == null) {
                delay(5_000)
                continue
            }
            val vence = System.currentTimeMillis() + codigo.expiraEnSegundos * 1_000L
            while (System.currentTimeMillis() < vence) {
                delay(CONSULTA_MS)
                when (consultarCodigo(codigo.tvToken)) {
                    is VinculoTv.Aprobado -> { onEntrar(); return@LaunchedEffect }
                    VinculoTv.Expirado -> break
                    VinculoTv.Pendiente, VinculoTv.SinConexion -> Unit
                }
            }
        }
    }

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
            // Bienvenida y entrada con el celular (QR + código)
            Column(modifier = Modifier.weight(1f).padding(end = 32.dp), verticalArrangement = Arrangement.Center) {
                Text("FESTIVAL JOSÉ ALFREDO JIMÉNEZ", color = FestivalGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text("Bienvenido al Festival", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Mira la programación, la galería y las transmisiones en vivo desde Dolores Hidalgo.",
                    color = FestivalTextSecondary, fontSize = 13.sp
                )
                Spacer(Modifier.height(20.dp))
                Card(colors = CardDefaults.cardColors(containerColor = FestivalCardBg), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val codigo = codigoTv
                        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                            when {
                                codigo != null -> DynamicQrCode(content = EnlacesWeb.vincularTv(codigo.codigo), modifier = Modifier.fillMaxSize())
                                sinConexionCodigo -> Text("Sin conexión con el servidor", color = FestivalTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                                else -> CircularProgressIndicator(color = FestivalGold, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Entra con tu celular", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Escanea el código con la cámara y confirma.", color = FestivalTextSecondary, fontSize = 12.sp)
                            Text("O en la app: Perfil → Vincular TV.", color = FestivalTextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                codigo?.codigo ?: "····-····",
                                color = FestivalGold, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                            )
                            Text("La TV entra sola al confirmar. El código cambia cada 5 min.", color = FestivalTextSecondary, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "¿No tienes cuenta? Al escanear el código también puedes crearla.",
                    color = FestivalTextSecondary, fontSize = 12.sp
                )
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
